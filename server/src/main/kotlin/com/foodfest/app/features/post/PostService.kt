package com.foodfest.app.features.post

import com.foodfest.app.core.exception.AppException
import kotlinx.serialization.Serializable

// =============================================
// RESPONSE DTOs
// =============================================
@Serializable
data class PostListResponse(
    val data: List<Post>,
    val page: Int,
    val limit: Int,
    val total: Int
)

@Serializable
data class CommentListResponse(
    val data: List<Comment>,
    val page: Int,
    val limit: Int,
    val total: Int
)

@Serializable
data class LikeResult(
    val isLiked: Boolean,
    val likeCount: Int
)

@Serializable
data class SaveResult(
    val isSaved: Boolean
)

@Serializable
data class DeleteCommentResult(
    val deleted: Boolean,
    val commentId: Int,
    val postId: Int,
    val parentCommentId: Int? = null,
    val deletedCount: Int
)

// =============================================
// SERVICE
// =============================================
class PostService(private val repository: PostRepository) {

    private suspend fun ensurePostExists(postId: Int) {
        if (!repository.existsPost(postId)) {
            throw AppException.NotFound("Không tìm thấy bài viết")
        }
    }

    private suspend fun getCommentNodeOrThrow(commentId: Int): CommentNode {
        return repository.getCommentNode(commentId)
            ?: throw AppException.NotFound("Không tìm thấy bình luận")
    }
    
    suspend fun createPost(
        userId: Int,
        request: CreatePostRequest
    ): Result<Post> = runCatching {
        if (request.content.isNullOrBlank() && request.imageUrl.isNullOrBlank()) {
            throw IllegalArgumentException("Bài viết phải có nội dung hoặc hình ảnh")
        }
        
        repository.createPost(
            userId = userId,
            postType = request.postType,
            title = request.title,
            content = request.content,
            imageUrl = request.imageUrl
        ) ?: throw IllegalStateException("Không thể tạo bài viết")
    }
    
    suspend fun getPostById(postId: Int, currentUserId: Int?): Result<Post> = runCatching {
        repository.getPostById(postId, currentUserId)
            ?: throw NoSuchElementException("Không tìm thấy bài viết")
    }
    
    suspend fun getPosts(
        page: Int, 
        currentUserId: Int?,
        search: String? = null,
        postType: String? = null
    ): Result<PostListResponse> = runCatching {
        val limit = 10
        val (posts, total) = repository.getPosts(page, limit, currentUserId, search, postType)
        PostListResponse(
            data = posts,
            page = page,
            limit = limit,
            total = total
        )
    }

    suspend fun getFollowingFeed(
        page: Int,
        currentUserId: Int,
        search: String? = null,
        postType: String? = null
    ): Result<PostListResponse> = runCatching {
        val limit = 10
        val (posts, total) = repository.getFollowingFeed(
            followerUserId = currentUserId,
            page = page,
            limit = limit,
            search = search,
            postType = postType
        )
        PostListResponse(
            data = posts,
            page = page,
            limit = limit,
            total = total
        )
    }
    
    suspend fun getSavedPosts(userId: Int, page: Int): Result<PostListResponse> = runCatching {
        val limit = 10
        val (posts, total) = repository.getSavedPosts(userId, page, limit)
        PostListResponse(
            data = posts,
            page = page,
            limit = limit,
            total = total
        )
    }
    
    suspend fun getUserPosts(
        userId: Int,
        page: Int,
        currentUserId: Int?
    ): Result<PostListResponse> = runCatching {
        val limit = 10
        val (posts, total) = repository.getUserPosts(userId, page, limit, currentUserId)
        PostListResponse(
            data = posts,
            page = page,
            limit = limit,
            total = total
        )
    }
    
    suspend fun likePost(userId: Int, postId: Int): Result<LikeResult> = runCatching {
        val isLiked = repository.likePost(userId, postId)
        val post = repository.getPostById(postId, userId) 
            ?: throw NoSuchElementException("Không tìm thấy bài viết")
        LikeResult(
            isLiked = isLiked,
            likeCount = post.likeCount
        )
    }
    
    suspend fun savePost(userId: Int, postId: Int): Result<SaveResult> = runCatching {
        val isSaved = repository.savePost(userId, postId)
        SaveResult(isSaved = isSaved)
    }
    
    suspend fun deletePost(userId: Int, postId: Int): Result<Boolean> = runCatching {
        val deleted = repository.deletePost(userId, postId)
        if (!deleted) {
            throw IllegalStateException("Không thể xóa bài viết. Bạn không phải chủ bài viết hoặc bài viết không tồn tại.")
        }
        true
    }
    
    // Comments
    suspend fun addComment(
        userId: Int,
        postId: Int,
        request: CreateCommentRequest
    ): Result<Comment> = runCatching {
        val normalizedContent = request.content.trim()

        if (normalizedContent.isBlank()) {
            throw IllegalArgumentException("Nội dung bình luận không được để trống")
        }

        ensurePostExists(postId)

        val parentCommentId = request.parentCommentId
        if (parentCommentId != null) {
            val parentComment = repository.getCommentNode(parentCommentId)
                ?: throw AppException.NotFound("Không tìm thấy bình luận cha")

            if (parentComment.postId != postId) {
                throw AppException.Validation("Reply phải thuộc cùng bài viết với comment cha")
            }

            if (parentComment.parentCommentId != null || parentComment.depth != 0) {
                throw AppException.Validation("Chỉ hỗ trợ tối đa 2 cấp bình luận")
            }
        }
        
        repository.addComment(
            userId = userId,
            postId = postId,
            content = normalizedContent,
            parentCommentId = parentCommentId
        )
            ?: throw IllegalStateException("Không thể thêm bình luận")
    }
    
    suspend fun getComments(postId: Int, page: Int, limit: Int = 20): Result<CommentListResponse> = runCatching {
        ensurePostExists(postId)

        val safeLimit = limit.coerceIn(1, 50)
        val (comments, total) = repository.getComments(postId, page, safeLimit)
        CommentListResponse(
            data = comments,
            page = page,
            limit = safeLimit,
            total = total
        )
    }

    suspend fun getReplies(commentId: Int, page: Int, limit: Int = 20): Result<CommentListResponse> = runCatching {
        val parentComment = getCommentNodeOrThrow(commentId)
        if (parentComment.parentCommentId != null || parentComment.depth != 0) {
            throw AppException.Validation("Chi lay duoc replies cua comment cap 1")
        }

        val safeLimit = limit.coerceIn(1, 50)
        val (replies, total) = repository.getReplies(commentId, page, safeLimit)
        CommentListResponse(
            data = replies,
            page = page,
            limit = safeLimit,
            total = total
        )
    }

    suspend fun deleteComment(userId: Int, commentId: Int): Result<DeleteCommentResult> = runCatching {
        val commentNode = getCommentNodeOrThrow(commentId)
        if (commentNode.userId != userId) {
            throw AppException.Forbidden("Ban khong co quyen xoa binh luan nay")
        }

        val deleted = repository.deleteComment(commentId)
            ?: throw IllegalStateException("Khong the xoa binh luan")

        DeleteCommentResult(
            deleted = true,
            commentId = deleted.commentId,
            postId = deleted.postId,
            parentCommentId = deleted.parentCommentId,
            deletedCount = deleted.deletedCount
        )
    }
}
