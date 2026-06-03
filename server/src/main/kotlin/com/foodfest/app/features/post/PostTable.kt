package com.foodfest.app.features.post

import com.foodfest.app.core.exception.AppException
import com.foodfest.app.features.auth.AuthTable
import com.foodfest.app.features.follow.FollowTable
import com.foodfest.app.features.notification.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.JavaInstantColumnType
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

// =============================================
// TABLE DEFINITIONS
// =============================================
object PostTable : IntIdTable("posts", "post_id") {
    val userId = reference("user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val postType = varchar("post_type", 20)
    val title = varchar("title", 200).nullable()
    val content = text("content").nullable()
    val imageUrl = text("image_url").nullable()
    val likeCount = integer("like_count").default(0)
    val commentCount = integer("comment_count").default(0)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

object SavedPostTable : Table("saved_posts") {
    val userId = reference("user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val postId = reference("post_id", PostTable, onDelete = ReferenceOption.CASCADE)
    val savedAt = timestamp("saved_at").defaultExpression(CurrentTimestamp())
    
    override val primaryKey = PrimaryKey(userId, postId)
}

object PostLikeTable : Table("post_likes") {
    val userId = reference("user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val postId = reference("post_id", PostTable, onDelete = ReferenceOption.CASCADE)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    
    override val primaryKey = PrimaryKey(userId, postId)
}

object CommentTable : IntIdTable("comments", "comment_id") {
    val userId = reference("user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val postId = reference("post_id", PostTable, onDelete = ReferenceOption.CASCADE)
    val parentCommentId = optReference("parent_comment_id", this, onDelete = ReferenceOption.CASCADE)
    val replyCount = integer("reply_count").default(0)
    val depth = integer("depth").default(0)
    val content = text("content")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

// =============================================
// MODELS
// =============================================
@Serializable
data class Post(
    val id: Int,
    val userId: Int,
    val userName: String,
    val userAvatar: String? = null,
    val postType: String,
    val title: String? = null,
    val content: String? = null,
    val imageUrl: String? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val trendScore: Int? = null,
    val trendRank: Int? = null,
    val isTrending: Boolean = false,
    val createdAt: String
)

@Serializable
data class Comment(
    val id: Int,
    val userId: Int,
    val userName: String,
    val userAvatar: String? = null,
    val postId: Int,
    val parentCommentId: Int? = null,
    val replyCount: Int = 0,
    val depth: Int = 0,
    val content: String,
    val createdAt: String
)

@Serializable
data class CreatePostRequest(
    val postType: String,
    val title: String? = null,
    val content: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class UpdatePostRequest(
    val title: String? = null,
    val content: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class CreateCommentRequest(
    val content: String,
    val parentCommentId: Int? = null
)

data class CommentNode(
    val id: Int,
    val userId: Int,
    val postId: Int,
    val parentCommentId: Int?,
    val replyCount: Int,
    val depth: Int
)

data class CommentDeleteSummary(
    val commentId: Int,
    val postId: Int,
    val parentCommentId: Int?,
    val deletedCount: Int
)

// =============================================
// REPOSITORY
// =============================================
class PostRepository {

    private data class FeedWhereSql(
        val clause: String,
        val args: List<Pair<IColumnType, Any?>>
    )

    private data class RankedFeedPost(
        val postId: Int,
        val trendScore: Int,
        val trendRank: Int?,
        val isTrending: Boolean
    )

    suspend fun existsPost(postId: Int): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        PostTable.select { PostTable.id eq postId }.count() > 0
    }
    
    suspend fun createPost(
        userId: Int,
        postType: String,
        title: String?,
        content: String?,
        imageUrl: String?
    ): Post? = newSuspendedTransaction(Dispatchers.IO) {
        val postId = PostTable.insertAndGetId {
            it[PostTable.userId] = userId
            it[PostTable.postType] = postType
            it[PostTable.title] = title
            it[PostTable.content] = content
            it[PostTable.imageUrl] = imageUrl
        }.value
        
        getPostByIdInternal(postId, userId)
    }
    
    suspend fun getPostById(postId: Int, currentUserId: Int? = null): Post? = 
        newSuspendedTransaction(Dispatchers.IO) {
            getPostByIdInternal(postId, currentUserId)
        }
    
    private fun getPostByIdInternal(postId: Int, currentUserId: Int?): Post? {
        val row = (PostTable innerJoin AuthTable)
            .select { PostTable.id eq postId }
            .singleOrNull() ?: return null
        
        val isLiked = currentUserId?.let { uid ->
            PostLikeTable.select { 
                (PostLikeTable.postId eq postId) and (PostLikeTable.userId eq uid) 
            }.count() > 0
        } ?: false
        
        val isSaved = currentUserId?.let { uid ->
            SavedPostTable.select { 
                (SavedPostTable.postId eq postId) and (SavedPostTable.userId eq uid) 
            }.count() > 0
        } ?: false
        
        return rowToPost(row, isLiked, isSaved)
    }
    
    suspend fun getPosts(
        page: Int,
        limit: Int,
        currentUserId: Int? = null,
        search: String? = null,
        postType: String? = null,
        searchType: String = "post",
        includeTrending: Boolean = false
    ): Pair<List<Post>, Int> = newSuspendedTransaction(Dispatchers.IO) {
        val normalizedSearchType = searchType.trim().lowercase()
        if (includeTrending) {
            return@newSuspendedTransaction getPostsWithTrendingRanking(
                page = page,
                limit = limit,
                currentUserId = currentUserId,
                search = search,
                postType = postType,
                searchType = normalizedSearchType
            )
        }

        val offset = ((page - 1).coerceAtLeast(0)) * limit
        val postWithAuthor = PostTable.innerJoin(AuthTable)

        // Điều kiện lọc feed.
        val conditions = mutableListOf<Op<Boolean>>()

        if (!search.isNullOrBlank()) {
            val pattern = LikePattern("%${search.lowercase()}%")

            if (normalizedSearchType == "user") {
                conditions.add(
                    Op.build {
                        (AuthTable.username.lowerCase() like pattern.pattern) or
                            (AuthTable.fullName.lowerCase() like pattern.pattern)
                    }
                )
            } else {
                conditions.add(
                    Op.build {
                        (PostTable.title.lowerCase() like pattern.pattern) or
                            (PostTable.content.lowerCase() like pattern.pattern)
                    }
                )
            }
        }

        if (!postType.isNullOrBlank()) {
            conditions.add(PostTable.postType eq postType)
        }

        val whereCondition = if (conditions.isEmpty()) {
            Op.TRUE
        } else {
            conditions.reduce { acc, op -> acc and op }
        }

        val total = postWithAuthor.select { whereCondition }.count().toInt()

        val orderedQuery = postWithAuthor
            .select { whereCondition }
            .orderBy(PostTable.createdAt to SortOrder.DESC)

        val posts = orderedQuery
            .limit(limit, offset.toLong())
            .map { row ->
                val postId = row[PostTable.id].value
                
                val isLiked = currentUserId?.let { uid ->
                    PostLikeTable.select { 
                        (PostLikeTable.postId eq postId) and (PostLikeTable.userId eq uid) 
                    }.count() > 0
                } ?: false
                
                val isSaved = currentUserId?.let { uid ->
                    SavedPostTable.select { 
                        (SavedPostTable.postId eq postId) and (SavedPostTable.userId eq uid) 
                    }.count() > 0
                } ?: false
                
                rowToPost(row, isLiked, isSaved)
            }
        
        posts to total
    }

    private fun Transaction.getPostsWithTrendingRanking(
        page: Int,
        limit: Int,
        currentUserId: Int?,
        search: String?,
        postType: String?,
        searchType: String
    ): Pair<List<Post>, Int> {
        val offset = ((page - 1).coerceAtLeast(0)) * limit
        val whereSql = buildFeedWhereSql(search = search, postType = postType, searchType = searchType)

        val countSql = """
            SELECT COUNT(*) AS total
            FROM posts p
            INNER JOIN users u ON p.user_id = u.user_id
            ${whereSql.clause}
        """.trimIndent()

        val total = exec(countSql, whereSql.args, StatementType.SELECT) { rs ->
            if (rs.next()) rs.getInt("total") else 0
        } ?: 0

        if (total == 0) {
            return emptyList<Post>() to 0
        }

        val cutoff = Instant.now().minus(7, ChronoUnit.DAYS)
        val rankingSql = """
            WITH base AS (
                SELECT
                    p.post_id,
                    p.created_at,
                    p.like_count,
                    p.comment_count
                FROM posts p
                INNER JOIN users u ON p.user_id = u.user_id
                ${whereSql.clause}
            ),
            trend AS (
                SELECT
                    b.post_id,
                    (b.like_count + b.comment_count * 2) AS trend_score,
                    DENSE_RANK() OVER (
                        ORDER BY
                            (b.like_count + b.comment_count * 2) DESC,
                            b.created_at DESC,
                            b.post_id DESC
                    ) AS trend_rank
                FROM base b
                WHERE b.created_at >= ?
            )
            SELECT
                b.post_id,
                (b.like_count + b.comment_count * 2) AS trend_score,
                t.trend_rank,
                (t.trend_rank IS NOT NULL) AS is_trending
            FROM base b
            LEFT JOIN trend t ON t.post_id = b.post_id
            ORDER BY
                CASE WHEN t.trend_rank IS NULL THEN 1 ELSE 0 END ASC,
                t.trend_rank ASC NULLS LAST,
                b.created_at DESC,
                b.post_id DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val rankingArgs = whereSql.args.toMutableList()
        rankingArgs.add(JavaInstantColumnType() to cutoff)
        rankingArgs.add(IntegerColumnType() to limit)
        rankingArgs.add(IntegerColumnType() to offset)

        val rankedPosts = exec(rankingSql, rankingArgs, StatementType.SELECT) { rs ->
            buildList {
                while (rs.next()) {
                    val rankValue = rs.getObject("trend_rank") as? Number
                    add(
                        RankedFeedPost(
                            postId = rs.getInt("post_id"),
                            trendScore = rs.getInt("trend_score"),
                            trendRank = rankValue?.toInt(),
                            isTrending = rs.getBoolean("is_trending")
                        )
                    )
                }
            }
        } ?: emptyList()

        if (rankedPosts.isEmpty()) {
            return emptyList<Post>() to total
        }

        val orderedPostIds = rankedPosts.map { it.postId }
        val rankByPostId = rankedPosts.associateBy { it.postId }

        val rowsByPostId = (PostTable innerJoin AuthTable)
            .select { PostTable.id inList orderedPostIds }
            .associateBy { it[PostTable.id].value }

        val posts = orderedPostIds.mapNotNull { postId ->
            val row = rowsByPostId[postId] ?: return@mapNotNull null
            val rankMeta = rankByPostId[postId] ?: return@mapNotNull null

            val isLiked = currentUserId?.let { uid ->
                PostLikeTable.select {
                    (PostLikeTable.postId eq postId) and (PostLikeTable.userId eq uid)
                }.count() > 0
            } ?: false

            val isSaved = currentUserId?.let { uid ->
                SavedPostTable.select {
                    (SavedPostTable.postId eq postId) and (SavedPostTable.userId eq uid)
                }.count() > 0
            } ?: false

            rowToPost(
                row = row,
                isLiked = isLiked,
                isSaved = isSaved,
                trendScore = rankMeta.trendScore,
                trendRank = rankMeta.trendRank,
                isTrending = rankMeta.isTrending
            )
        }

        return posts to total
    }

    private fun buildFeedWhereSql(
        search: String?,
        postType: String?,
        searchType: String
    ): FeedWhereSql {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<Pair<IColumnType, Any?>>()

        val normalizedSearch = search?.trim()?.lowercase()
        if (!normalizedSearch.isNullOrBlank()) {
            val pattern = "%$normalizedSearch%"
            if (searchType == "user") {
                conditions += "(LOWER(u.username) LIKE ? OR LOWER(u.full_name) LIKE ?)"
            } else {
                conditions += "(LOWER(COALESCE(p.title, '')) LIKE ? OR LOWER(COALESCE(p.content, '')) LIKE ?)"
            }
            args.add(TextColumnType() to pattern)
            args.add(TextColumnType() to pattern)
        }

        val normalizedPostType = postType?.trim()?.takeIf { it.isNotBlank() }
        if (normalizedPostType != null) {
            conditions += "p.post_type = ?"
            args.add(TextColumnType() to normalizedPostType)
        }

        val clause = if (conditions.isEmpty()) {
            ""
        } else {
            "WHERE ${conditions.joinToString(" AND ")}"
        }

        return FeedWhereSql(clause = clause, args = args)
    }

            suspend fun getFollowingFeed(
                followerUserId: Int,
                page: Int,
                limit: Int,
                search: String? = null,
                postType: String? = null
            ): Pair<List<Post>, Int> = newSuspendedTransaction(Dispatchers.IO) {
                val offset = ((page - 1).coerceAtLeast(0)) * limit

                val conditions = mutableListOf<Op<Boolean>>()
                conditions.add(FollowTable.followerId eq followerUserId)

                if (!search.isNullOrBlank()) {
                    val pattern = LikePattern("%${search.lowercase()}%")
                    conditions.add(
                        Op.build {
                            (PostTable.title.lowerCase() like pattern.pattern) or
                                (PostTable.content.lowerCase() like pattern.pattern)
                        }
                    )
                }

                if (!postType.isNullOrBlank()) {
                    conditions.add(PostTable.postType eq postType)
                }

                val whereCondition = conditions.reduce { acc, op -> acc and op }

                val postWithAuthorAndFollow = PostTable
                    .join(AuthTable, JoinType.INNER, PostTable.userId, AuthTable.id)
                    .join(FollowTable, JoinType.INNER, PostTable.userId, FollowTable.followingId)

                val total = postWithAuthorAndFollow.select { whereCondition }.count().toInt()

                val posts = postWithAuthorAndFollow
                    .select { whereCondition }
                    .orderBy(PostTable.createdAt to SortOrder.DESC)
                    .limit(limit, offset.toLong())
                    .map { row ->
                        val postId = row[PostTable.id].value

                        val isLiked = PostLikeTable.select {
                            (PostLikeTable.postId eq postId) and (PostLikeTable.userId eq followerUserId)
                        }.count() > 0

                        val isSaved = SavedPostTable.select {
                            (SavedPostTable.postId eq postId) and (SavedPostTable.userId eq followerUserId)
                        }.count() > 0

                        rowToPost(row, isLiked, isSaved)
                    }

                posts to total
            }
    
    suspend fun getUserPosts(
        userId: Int,
        page: Int,
        limit: Int,
        currentUserId: Int? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null
    ): Pair<List<Post>, Int> = newSuspendedTransaction(Dispatchers.IO) {
        val offset = ((page - 1).coerceAtLeast(0)) * limit

        // Lọc bài theo chủ sở hữu và khoảng ngày.
        val conditions = mutableListOf<Op<Boolean>>()
        conditions.add(PostTable.userId eq userId)
        startDate?.let { start ->
            val startInstant = start.atZone(ZoneOffset.UTC).toInstant()
            conditions.add(Op.build { PostTable.createdAt greaterEq startInstant })
        }
        endDate?.let { end ->
            val endInstant = end.atZone(ZoneOffset.UTC).toInstant()
            conditions.add(Op.build { PostTable.createdAt lessEq endInstant })
        }

        val whereCondition = conditions.reduce { acc, op -> acc and op }

        val total = PostTable.select { whereCondition }.count().toInt()

        val posts = (PostTable innerJoin AuthTable)
            .select { whereCondition }
            .orderBy(PostTable.createdAt to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { row ->
                val postId = row[PostTable.id].value
                
                val isLiked = currentUserId?.let { uid ->
                    PostLikeTable.select { 
                        (PostLikeTable.postId eq postId) and (PostLikeTable.userId eq uid) 
                    }.count() > 0
                } ?: false
                
                val isSaved = currentUserId?.let { uid ->
                    SavedPostTable.select { 
                        (SavedPostTable.postId eq postId) and (SavedPostTable.userId eq uid) 
                    }.count() > 0
                } ?: false
                
                rowToPost(row, isLiked, isSaved)
            }
        
        posts to total
    }
    
    suspend fun getSavedPosts(
        userId: Int,
        page: Int,
        limit: Int
    ): Pair<List<Post>, Int> = newSuspendedTransaction(Dispatchers.IO) {
        val offset = ((page - 1).coerceAtLeast(0)) * limit
        
        val total = SavedPostTable.select { SavedPostTable.userId eq userId }.count().toInt()
        
        // Join SavedPostTable -> PostTable -> AuthTable (via PostTable.userId)
        val posts = SavedPostTable
            .innerJoin(PostTable, { SavedPostTable.postId }, { PostTable.id })
            .innerJoin(AuthTable, { PostTable.userId }, { AuthTable.id })
            .select { SavedPostTable.userId eq userId }
            .orderBy(SavedPostTable.savedAt to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { row ->
                val postId = row[PostTable.id].value
                
                val isLiked = PostLikeTable.select { 
                    (PostLikeTable.postId eq postId) and (PostLikeTable.userId eq userId) 
                }.count() > 0
                
                rowToPost(row, isLiked, isSaved = true)
            }
        
        posts to total
    }
    
    suspend fun likePost(userId: Int, postId: Int): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val exists = PostLikeTable.select { 
            (PostLikeTable.userId eq userId) and (PostLikeTable.postId eq postId) 
        }.count() > 0
        
        if (exists) {
            // Unlike
            PostLikeTable.deleteWhere { 
                (PostLikeTable.userId eq userId) and (PostLikeTable.postId eq postId) 
            }
            PostTable.update({ PostTable.id eq postId }) {
                with(SqlExpressionBuilder) {
                    it.update(likeCount, likeCount - 1)
                }
            }
            false
        } else {
            // Like
            PostLikeTable.insert {
                it[PostLikeTable.userId] = userId
                it[PostLikeTable.postId] = postId
            }
            PostTable.update({ PostTable.id eq postId }) {
                with(SqlExpressionBuilder) {
                    it.update(likeCount, likeCount + 1)
                }
            }
            val postRow = PostTable.select { PostTable.id eq postId }.singleOrNull()
            val ownerUserId = postRow?.get(PostTable.userId)?.value
            if (ownerUserId != null && ownerUserId != userId) {
                val likerName = AuthTable.select { AuthTable.id eq userId }
                    .singleOrNull()
                    ?.let { row -> row[AuthTable.fullName].trim().ifBlank { row[AuthTable.username] } }
                    ?: "Một người dùng"
                val postTitle = postRow[PostTable.title]?.takeIf { it.isNotBlank() } ?: "bài viết của bạn"
                NotificationRepository.insertEventNotification(
                    userId = ownerUserId,
                    type = "post_like",
                    title = "Có lượt thích mới",
                    message = "$likerName đã thích $postTitle",
                    relatedEntityType = "post",
                    relatedEntityId = postId,
                    actionUrl = "foodfest://posts/$postId"
                )
            }
            true
        }
    }
    
    suspend fun savePost(userId: Int, postId: Int): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val exists = SavedPostTable.select { 
            (SavedPostTable.userId eq userId) and (SavedPostTable.postId eq postId) 
        }.count() > 0
        
        if (exists) {
            // Unsave
            SavedPostTable.deleteWhere { 
                (SavedPostTable.userId eq userId) and (SavedPostTable.postId eq postId) 
            }
            false
        } else {
            // Save
            SavedPostTable.insert {
                it[SavedPostTable.userId] = userId
                it[SavedPostTable.postId] = postId
            }
            true
        }
    }
    
    suspend fun deletePost(userId: Int, postId: Int): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val currentRow = PostTable.select { PostTable.id eq postId }.singleOrNull()
            ?: throw AppException.NotFound("Không tìm thấy bài viết")

        if (currentRow[PostTable.userId].value != userId) {
            throw AppException.Forbidden("Bạn không có quyền xóa bài viết này")
        }

        PostTable.deleteWhere {
            (PostTable.id eq postId) and (PostTable.userId eq userId)
        } > 0
    }

    suspend fun updatePost(
        userId: Int,
        postId: Int,
        title: String?,
        content: String?,
        imageUrl: String?
    ): Post? = newSuspendedTransaction(Dispatchers.IO) {
        val currentRow = PostTable.select { PostTable.id eq postId }.singleOrNull()
            ?: throw AppException.NotFound("Không tìm thấy bài viết")

        if (currentRow[PostTable.userId].value != userId) {
            throw AppException.Forbidden("Bạn không có quyền cập nhật bài viết này")
        }

        // Null = giữ nguyên, rỗng = xóa giá trị.
        val hasTitleUpdate = title != null
        val hasContentUpdate = content != null
        val hasImageUpdate = imageUrl != null

        val normalizedTitle = title?.trim()?.takeIf { it.isNotBlank() }
        val normalizedContent = content?.trim()?.takeIf { it.isNotBlank() }
        val normalizedImage = imageUrl?.trim()?.takeIf { it.isNotBlank() }

        val nextTitle = if (hasTitleUpdate) normalizedTitle else currentRow[PostTable.title]
        val nextContent = if (hasContentUpdate) normalizedContent else currentRow[PostTable.content]
        val nextImage = if (hasImageUpdate) normalizedImage else currentRow[PostTable.imageUrl]

        if (nextContent.isNullOrBlank() && nextImage.isNullOrBlank()) {
            throw IllegalArgumentException("Bài viết phải có nội dung hoặc hình ảnh")
        }

        PostTable.update({
            (PostTable.id eq postId) and (PostTable.userId eq userId)
        }) {
            it[PostTable.title] = nextTitle
            it[PostTable.content] = nextContent
            it[PostTable.imageUrl] = nextImage
        }

        getPostByIdInternal(postId, userId)
    }
    
    // Comments
    suspend fun addComment(
        userId: Int,
        postId: Int,
        content: String,
        parentCommentId: Int? = null
    ): Comment? = 
        newSuspendedTransaction(Dispatchers.IO) {
            val depth = if (parentCommentId == null) 0 else 1

            val commentId = CommentTable.insertAndGetId {
                it[CommentTable.userId] = userId
                it[CommentTable.postId] = postId
                it[CommentTable.parentCommentId] = parentCommentId
                it[CommentTable.replyCount] = 0
                it[CommentTable.depth] = depth
                it[CommentTable.content] = content
            }.value

            if (parentCommentId != null) {
                CommentTable.update({ CommentTable.id eq parentCommentId }) {
                    with(SqlExpressionBuilder) {
                        it.update(replyCount, replyCount + 1)
                    }
                }
            }
            
            // Update comment count
            PostTable.update({ PostTable.id eq postId }) {
                with(SqlExpressionBuilder) {
                    it.update(commentCount, commentCount + 1)
                }
            }
            
            getCommentByIdInternal(commentId)
        }
    
    suspend fun getComments(postId: Int, page: Int, limit: Int): Pair<List<Comment>, Int> = 
        newSuspendedTransaction(Dispatchers.IO) {
            val offset = ((page - 1).coerceAtLeast(0)) * limit
            val topLevelCondition = (CommentTable.postId eq postId) and (CommentTable.parentCommentId eq null)
            
            val total = CommentTable.select { topLevelCondition }.count().toInt()
            
            val comments = (CommentTable innerJoin AuthTable)
                .select { topLevelCondition }
                .orderBy(CommentTable.createdAt to SortOrder.ASC)
                .limit(limit, offset.toLong())
                .map { rowToComment(it) }
            
            comments to total
        }

    suspend fun getReplies(parentCommentId: Int, page: Int, limit: Int): Pair<List<Comment>, Int> =
        newSuspendedTransaction(Dispatchers.IO) {
            val offset = ((page - 1).coerceAtLeast(0)) * limit
            val repliesCondition = CommentTable.parentCommentId eq parentCommentId

            val total = CommentTable.select { repliesCondition }.count().toInt()

            val replies = (CommentTable innerJoin AuthTable)
                .select { repliesCondition }
                .orderBy(CommentTable.createdAt to SortOrder.ASC)
                .limit(limit, offset.toLong())
                .map { rowToComment(it) }

            replies to total
        }

    suspend fun getCommentNode(commentId: Int): CommentNode? = newSuspendedTransaction(Dispatchers.IO) {
        getCommentNodeInternal(commentId)
    }

    suspend fun deleteComment(commentId: Int): CommentDeleteSummary? = newSuspendedTransaction(Dispatchers.IO) {
        val commentNode = getCommentNodeInternal(commentId) ?: return@newSuspendedTransaction null

        val deletedCount = if (commentNode.parentCommentId == null) {
            val childCount = CommentTable.select { CommentTable.parentCommentId eq commentNode.id }.count().toInt()
            CommentTable.deleteWhere { CommentTable.parentCommentId eq commentNode.id }
            childCount + 1
        } else {
            val parentRow = CommentTable
                .select { CommentTable.id eq commentNode.parentCommentId }
                .singleOrNull()
            if (parentRow != null) {
                val newReplyCount = (parentRow[CommentTable.replyCount] - 1).coerceAtLeast(0)
                CommentTable.update({ CommentTable.id eq commentNode.parentCommentId }) {
                    it[replyCount] = newReplyCount
                }
            }
            1
        }

        val deleted = CommentTable.deleteWhere { CommentTable.id eq commentNode.id } > 0
        if (!deleted) return@newSuspendedTransaction null

        val postRow = PostTable.select { PostTable.id eq commentNode.postId }.singleOrNull()
        if (postRow != null) {
            val newCommentCount = (postRow[PostTable.commentCount] - deletedCount).coerceAtLeast(0)
            PostTable.update({ PostTable.id eq commentNode.postId }) {
                it[commentCount] = newCommentCount
            }
        }

        CommentDeleteSummary(
            commentId = commentNode.id,
            postId = commentNode.postId,
            parentCommentId = commentNode.parentCommentId,
            deletedCount = deletedCount
        )
    }
    
    private fun getCommentByIdInternal(commentId: Int): Comment? {
        return (CommentTable innerJoin AuthTable)
            .select { CommentTable.id eq commentId }
            .map { rowToComment(it) }
            .singleOrNull()
    }

    private fun getCommentNodeInternal(commentId: Int): CommentNode? {
        val row = CommentTable
            .select { CommentTable.id eq commentId }
            .singleOrNull() ?: return null

        return CommentNode(
            id = row[CommentTable.id].value,
            userId = row[CommentTable.userId].value,
            postId = row[CommentTable.postId].value,
            parentCommentId = row[CommentTable.parentCommentId]?.value,
            replyCount = row[CommentTable.replyCount],
            depth = row[CommentTable.depth]
        )
    }
    
    private fun rowToPost(
        row: ResultRow,
        isLiked: Boolean,
        isSaved: Boolean,
        trendScore: Int? = null,
        trendRank: Int? = null,
        isTrending: Boolean = false
    ): Post {
        return Post(
            id = row[PostTable.id].value,
            userId = row[PostTable.userId].value,
            userName = row[AuthTable.fullName],
            userAvatar = row[AuthTable.avatarUrl],
            postType = row[PostTable.postType],
            title = row[PostTable.title],
            content = row[PostTable.content],
            imageUrl = row[PostTable.imageUrl],
            likeCount = row[PostTable.likeCount],
            commentCount = row[PostTable.commentCount],
            isLiked = isLiked,
            isSaved = isSaved,
            trendScore = trendScore,
            trendRank = trendRank,
            isTrending = isTrending,
            createdAt = row[PostTable.createdAt].toString()
        )
    }
    
    private fun rowToComment(row: ResultRow): Comment {
        return Comment(
            id = row[CommentTable.id].value,
            userId = row[CommentTable.userId].value,
            userName = row[AuthTable.fullName],
            userAvatar = row[AuthTable.avatarUrl],
            postId = row[CommentTable.postId].value,
            parentCommentId = row[CommentTable.parentCommentId]?.value,
            replyCount = row[CommentTable.replyCount],
            depth = row[CommentTable.depth],
            content = row[CommentTable.content],
            createdAt = row[CommentTable.createdAt].toString()
        )
    }
}
