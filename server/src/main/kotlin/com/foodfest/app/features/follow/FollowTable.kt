package com.foodfest.app.features.follow

import com.foodfest.app.features.auth.AuthTable
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

// =============================================
// TABLE DEFINITION
// =============================================
object FollowTable : Table("follows") {
    val followerId = reference("follower_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val followingId = reference("following_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    
    override val primaryKey = PrimaryKey(followerId, followingId)
}

// =============================================
// MODELS
// =============================================
@Serializable
data class FollowUser(
    val id: Int,
    val username: String,
    val fullName: String,
    val avatarUrl: String? = null,
    val isFollowing: Boolean = false
)

@Serializable
data class FollowResult(
    val isFollowing: Boolean,
    val followerCount: Int,
    val followingCount: Int
)

@Serializable
data class FollowListResponse(
    val data: List<FollowUser>,
    val page: Int,
    val limit: Int,
    val total: Int
)

// =============================================
// REPOSITORY
// =============================================
class FollowRepository {
    
    /**
     * Follow/Unfollow a user
     * Uses transaction to ensure data integrity when updating counts
     */
    suspend fun toggleFollow(followerId: Int, followingId: Int): FollowResult = 
        newSuspendedTransaction(Dispatchers.IO) {
            val exists = FollowTable.select { 
                (FollowTable.followerId eq followerId) and (FollowTable.followingId eq followingId) 
            }.count() > 0
            
            if (exists) {
                // Unfollow
                FollowTable.deleteWhere { 
                    (FollowTable.followerId eq followerId) and (FollowTable.followingId eq followingId) 
                }
                
                // Giảm following_count của người đi follow
                AuthTable.update({ AuthTable.id eq followerId }) {
                    with(SqlExpressionBuilder) {
                        it.update(followingCount, followingCount - 1)
                    }
                }
                
                // Giảm follower_count của người được follow
                AuthTable.update({ AuthTable.id eq followingId }) {
                    with(SqlExpressionBuilder) {
                        it.update(followerCount, followerCount - 1)
                    }
                }
            } else {
                // Follow
                FollowTable.insert {
                    it[FollowTable.followerId] = followerId
                    it[FollowTable.followingId] = followingId
                }
                
                // Tăng following_count của người đi follow
                AuthTable.update({ AuthTable.id eq followerId }) {
                    with(SqlExpressionBuilder) {
                        it.update(followingCount, followingCount + 1)
                    }
                }
                
                // Tăng follower_count của người được follow
                AuthTable.update({ AuthTable.id eq followingId }) {
                    with(SqlExpressionBuilder) {
                        it.update(followerCount, followerCount + 1)
                    }
                }
            }
            
            // Lấy số liệu mới nhất của người được follow
            val targetUser = AuthTable.select { AuthTable.id eq followingId }.single()
            
            FollowResult(
                isFollowing = !exists,
                followerCount = targetUser[AuthTable.followerCount],
                followingCount = targetUser[AuthTable.followingCount]
            )
        }
    
    /**
     * Check if user A is following user B
     */
    suspend fun isFollowing(followerId: Int, followingId: Int): Boolean = 
        newSuspendedTransaction(Dispatchers.IO) {
            FollowTable.select { 
                (FollowTable.followerId eq followerId) and (FollowTable.followingId eq followingId) 
            }.count() > 0
        }
    
    /**
     * Get list of followers (người theo dõi mình)
     */
    suspend fun getFollowers(
        userId: Int,
        currentUserId: Int?,
        page: Int,
        limit: Int
    ): Pair<List<FollowUser>, Int> = newSuspendedTransaction(Dispatchers.IO) {
        val offset = ((page - 1).coerceAtLeast(0)) * limit
        
        val total = FollowTable.select { FollowTable.followingId eq userId }.count().toInt()
        
        // Join với AuthTable để lấy thông tin người follow (follower_id -> user)
        val followers = FollowTable
            .join(AuthTable, JoinType.INNER, FollowTable.followerId, AuthTable.id)
            .slice(AuthTable.id, AuthTable.username, AuthTable.fullName, AuthTable.avatarUrl)
            .select { FollowTable.followingId eq userId }
            .orderBy(FollowTable.createdAt to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { row ->
                val targetId = row[AuthTable.id].value
                
                // Check if current user is following this person
                val isFollowing = currentUserId?.let { uid ->
                    FollowTable.select { 
                        (FollowTable.followerId eq uid) and (FollowTable.followingId eq targetId) 
                    }.count() > 0
                } ?: false
                
                FollowUser(
                    id = targetId,
                    username = row[AuthTable.username],
                    fullName = row[AuthTable.fullName],
                    avatarUrl = row[AuthTable.avatarUrl],
                    isFollowing = isFollowing
                )
            }
        
        followers to total
    }
    
    /**
     * Get list of following (người mình đang theo dõi)
     */
    suspend fun getFollowing(
        userId: Int,
        currentUserId: Int?,
        page: Int,
        limit: Int
    ): Pair<List<FollowUser>, Int> = newSuspendedTransaction(Dispatchers.IO) {
        val offset = ((page - 1).coerceAtLeast(0)) * limit
        
        val total = FollowTable.select { FollowTable.followerId eq userId }.count().toInt()
        
        // Join với AuthTable để lấy thông tin user được follow
        val following = FollowTable
            .join(AuthTable, JoinType.INNER, FollowTable.followingId, AuthTable.id)
            .slice(AuthTable.id, AuthTable.username, AuthTable.fullName, AuthTable.avatarUrl)
            .select { FollowTable.followerId eq userId }
            .orderBy(FollowTable.createdAt to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { row ->
                val targetId = row[AuthTable.id].value
                
                // Nếu đang xem danh sách following của mình thì isFollowing = true
                val isFollowing = if (currentUserId == userId) {
                    true
                } else {
                    currentUserId?.let { uid ->
                        FollowTable.select { 
                            (FollowTable.followerId eq uid) and (FollowTable.followingId eq targetId) 
                        }.count() > 0
                    } ?: false
                }
                
                FollowUser(
                    id = targetId,
                    username = row[AuthTable.username],
                    fullName = row[AuthTable.fullName],
                    avatarUrl = row[AuthTable.avatarUrl],
                    isFollowing = isFollowing
                )
            }
        
        following to total
    }
}

// =============================================
// SERVICE
// =============================================
class FollowService(private val repository: FollowRepository) {
    
    suspend fun toggleFollow(followerId: Int, followingId: Int): Result<FollowResult> = runCatching {
        if (followerId == followingId) {
            throw IllegalArgumentException("Không thể tự follow chính mình")
        }
        repository.toggleFollow(followerId, followingId)
    }
    
    suspend fun isFollowing(followerId: Int, followingId: Int): Result<Boolean> = runCatching {
        repository.isFollowing(followerId, followingId)
    }
    
    suspend fun getFollowers(
        userId: Int,
        currentUserId: Int?,
        page: Int
    ): Result<FollowListResponse> = runCatching {
        val limit = 20
        val (followers, total) = repository.getFollowers(userId, currentUserId, page, limit)
        FollowListResponse(
            data = followers,
            page = page,
            limit = limit,
            total = total
        )
    }
    
    suspend fun getFollowing(
        userId: Int,
        currentUserId: Int?,
        page: Int
    ): Result<FollowListResponse> = runCatching {
        val limit = 20
        val (following, total) = repository.getFollowing(userId, currentUserId, page, limit)
        FollowListResponse(
            data = following,
            page = page,
            limit = limit,
            total = total
        )
    }
}
