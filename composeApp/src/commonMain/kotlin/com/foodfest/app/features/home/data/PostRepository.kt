package com.foodfest.app.features.home.data

import com.foodfest.app.core.network.NetworkClient
import com.foodfest.app.core.storage.TokenManager
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
private data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

@Serializable
data class PostListResponse(
    val data: List<Post>,
    val page: Int,
    val limit: Int,
    val total: Int
)

// @Serializable
// data class CommentListResponse(
//     val data: List<Comment>,
//     val page: Int,
//     val limit: Int,
//     val total: Int
// )

@Serializable
data class LikeResult(
    val isLiked: Boolean,
    val likeCount: Int
)

@Serializable
data class SaveResult(
    val isSaved: Boolean
)

class PostRepository {
    private val client = NetworkClient.httpClient
    private val baseUrl = NetworkClient.BASE_URL

    private fun getAuthHeaders(): Map<String, String> {
        val token = TokenManager.getToken()
        return if (token != null) {
            mapOf("Authorization" to "Bearer $token")
        } else {
            emptyMap()
        }
    }

    suspend fun getPosts(
        page: Int = 1, 
        limit: Int = 10,
        search: String? = null,
        postType: String? = null
    ): Result<PostListResponse> {
        return try {
            val response = client.get("$baseUrl/api/posts") {
                parameter("page", page)
                parameter("limit", limit)
                if (!search.isNullOrBlank()) {
                    parameter("search", search)
                }
                if (!postType.isNullOrBlank()) {
                    parameter("postType", postType)
                }
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            val apiResponse = response.body<ApiResponse<PostListResponse>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to get posts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostById(postId: Int): Result<Post> {
        return try {
            val response = client.get("$baseUrl/api/posts/$postId") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            val apiResponse = response.body<ApiResponse<Post>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to get post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPost(request: CreatePostRequest): Result<Post> {
        return try {
            val response = client.post("$baseUrl/api/posts") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(request)
            }
            
            val apiResponse = response.body<ApiResponse<Post>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to create post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likePost(postId: Int): Result<LikeResult> {
        return try {
            val response = client.post("$baseUrl/api/posts/$postId/like") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            val apiResponse = response.body<ApiResponse<LikeResult>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to like post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePost(postId: Int): Result<SaveResult> {
        return try {
            val response = client.post("$baseUrl/api/posts/$postId/save") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            val apiResponse = response.body<ApiResponse<SaveResult>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to save post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSavedPosts(page: Int = 1, limit: Int = 10): Result<PostListResponse> {
        return try {
            val response = client.get("$baseUrl/api/posts/saved") {
                parameter("page", page)
                parameter("limit", limit)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            val apiResponse = response.body<ApiResponse<PostListResponse>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to get saved posts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // suspend fun getComments(postId: Int, page: Int = 1, limit: Int = 20): Result<CommentListResponse> {
    //     return try {
    //         val response = client.get("$baseUrl/api/posts/$postId/comments") {
    //             parameter("page", page)
    //             parameter("limit", limit)
    //             getAuthHeaders().forEach { (key, value) ->
    //                 header(key, value)
    //             }
    //         }
            
    //         val apiResponse = response.body<ApiResponse<CommentListResponse>>()
    //         if (apiResponse.success && apiResponse.data != null) {
    //             Result.success(apiResponse.data)
    //         } else {
    //             Result.failure(Exception(apiResponse.message ?: "Failed to get comments"))
    //         }
    //     } catch (e: Exception) {
    //         Result.failure(e)
    //     }
    // }

    // suspend fun addComment(postId: Int, content: String): Result<Comment> {
    //     return try {
    //         val response = client.post("$baseUrl/api/posts/$postId/comments") {
    //             contentType(ContentType.Application.Json)
    //             getAuthHeaders().forEach { (key, value) ->
    //                 header(key, value)
    //             }
    //             setBody(CreateCommentRequest(content))
    //         }
            
    //         val apiResponse = response.body<ApiResponse<Comment>>()
    //         if (apiResponse.success && apiResponse.data != null) {
    //             Result.success(apiResponse.data)
    //         } else {
    //             Result.failure(Exception(apiResponse.message ?: "Failed to add comment"))
    //         }
    //     } catch (e: Exception) {
    //         Result.failure(e)
    //     }
    // }

    // Upload ảnh lên Cloudinary qua server
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun uploadImage(imageBytes: ByteArray, fileName: String): Result<String> {
        return try {
            // Convert to base64
            val base64Data = Base64.encode(imageBytes)
            
            val response = client.post("$baseUrl/api/upload/image") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(UploadImageRequest(
                    imageData = base64Data,
                    fileName = fileName
                ))
            }
            
            val apiResponse = response.body<ApiResponse<UploadImageResponse>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data.imageUrl)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to upload image"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Serializable
data class UploadImageRequest(
    val imageData: String, // Base64 encoded
    val fileName: String
)

@Serializable
data class UploadImageResponse(
    val imageUrl: String
)
