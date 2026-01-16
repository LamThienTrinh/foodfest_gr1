package com.foodfest.app.features.post

import com.foodfest.app.features.auth.AuthTable
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

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
    val createdAt: String
)

@Serializable
data class Comment(
    val id: Int,
    val userId: Int,
    val userName: String,
    val userAvatar: String? = null,
    val postId: Int,
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
data class CreateCommentRequest(
    val content: String
)

// =============================================
// REPOSITORY
// =============================================
class PostRepository {
    
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
        postType: String? = null
    ): Pair<List<Post>, Int> = newSuspendedTransaction(Dispatchers.IO) {
        val offset = ((page - 1).coerceAtLeast(0)) * limit
        
        // Build WHERE conditions
        val conditions = mutableListOf<Op<Boolean>>()
        
        if (!search.isNullOrBlank()) {
            val pattern = LikePattern("%${search.lowercase()}%")
            conditions.add(LikeOp(PostTable.title.lowerCase(), stringParam(pattern.pattern)))
        }
        
        if (!postType.isNullOrBlank()) {
            conditions.add(PostTable.postType eq postType)
        }
        
        val whereCondition = if (conditions.isEmpty()) {
            Op.TRUE
        } else {
            conditions.reduce { acc, op -> acc and op }
        }
        
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
    
    suspend fun getUserPosts(
        userId: Int,
        page: Int,
        limit: Int,
        currentUserId: Int? = null
    ): Pair<List<Post>, Int> = newSuspendedTransaction(Dispatchers.IO) {
        val offset = ((page - 1).coerceAtLeast(0)) * limit
        
        val total = PostTable.select { PostTable.userId eq userId }.count().toInt()
        
        val posts = (PostTable innerJoin AuthTable)
            .select { PostTable.userId eq userId }
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
        PostTable.deleteWhere { 
            (PostTable.id eq postId) and (PostTable.userId eq userId) 
        } > 0
    }
    
    // Comments
    suspend fun addComment(userId: Int, postId: Int, content: String): Comment? = 
        newSuspendedTransaction(Dispatchers.IO) {
            val commentId = CommentTable.insertAndGetId {
                it[CommentTable.userId] = userId
                it[CommentTable.postId] = postId
                it[CommentTable.content] = content
            }.value
            
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
            
            val total = CommentTable.select { CommentTable.postId eq postId }.count().toInt()
            
            val comments = (CommentTable innerJoin AuthTable)
                .select { CommentTable.postId eq postId }
                .orderBy(CommentTable.createdAt to SortOrder.ASC)
                .limit(limit, offset.toLong())
                .map { rowToComment(it) }
            
            comments to total
        }
    
    private fun getCommentByIdInternal(commentId: Int): Comment? {
        return (CommentTable innerJoin AuthTable)
            .select { CommentTable.id eq commentId }
            .map { rowToComment(it) }
            .singleOrNull()
    }
    
    private fun rowToPost(row: ResultRow, isLiked: Boolean, isSaved: Boolean): Post {
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
            content = row[CommentTable.content],
            createdAt = row[CommentTable.createdAt].toString()
        )
    }
}
