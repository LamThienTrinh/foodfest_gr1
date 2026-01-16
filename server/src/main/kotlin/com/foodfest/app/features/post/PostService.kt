package com.foodfest.app.features.post

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

// =============================================
// SERVICE
// =============================================
class PostService(private val repository: PostRepository) {
    
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
        if (request.content.isBlank()) {
            throw IllegalArgumentException("Nội dung bình luận không được để trống")
        }
        
        repository.addComment(userId, postId, request.content)
            ?: throw IllegalStateException("Không thể thêm bình luận")
    }
    
    suspend fun getComments(postId: Int, page: Int): Result<CommentListResponse> = runCatching {
        val limit = 20
        val (comments, total) = repository.getComments(postId, page, limit)
        CommentListResponse(
            data = comments,
            page = page,
            limit = limit,
            total = total
        )
    }
}
