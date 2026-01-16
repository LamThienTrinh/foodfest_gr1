package com.foodfest.app.features.savedposts.data

import com.foodfest.app.core.network.NetworkClient
import com.foodfest.app.core.storage.TokenManager
import com.foodfest.app.features.home.data.Post
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// ==========================================
// 1. CẤU TRÚC JSON TỪ BACKEND (INTERNAL)
// ==========================================
@Serializable
private data class SavedPostsApiResponse(
    val success: Boolean,
    val data: SavedPostDataWrapper? = null,
    val message: String? = null
)

@Serializable
private data class SavedPostDataWrapper(
    val data: List<Post> = emptyList(),
    val page: Int = 1,
    val limit: Int = 10,
    val total: Int = 0
)

// ==========================================
// 2. CẤU TRÚC DỮ LIỆU CHO APP (PUBLIC)
// ==========================================
@Serializable
data class SavedPostsResponse(
    val success: Boolean,
    val data: List<Post> = emptyList(),
    val message: String? = null,
    val page: Int = 1,
    val totalPages: Int = 1
)

@Serializable
data class UnsaveResponse(
    val success: Boolean,
    val message: String? = null
)

class SavedPostsRepository {
    private val client = NetworkClient.httpClient
    private val baseUrl = NetworkClient.BASE_URL

    suspend fun getSavedPosts(page: Int = 1, limit: Int = 10): Result<SavedPostsResponse> {
        // 👇 SỬA LẠI: Dùng Dispatchers.Default thay cho IO để chạy được trên commonMain
        return withContext(Dispatchers.Default) {
            try {
                val token = TokenManager.getToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("Chưa đăng nhập"))
                }

                val response = client.get("$baseUrl/api/posts/saved") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("page", page)
                    parameter("limit", limit)
                }

                if (response.status == HttpStatusCode.OK) {
                    val rawResponse = response.body<SavedPostsApiResponse>()

                    // Tính toán Total Pages
                    val totalItems = rawResponse.data?.total ?: 0
                    val limitPerPage = if ((rawResponse.data?.limit ?: 0) > 0) rawResponse.data!!.limit else limit
                    val calculatedTotalPages = (totalItems + limitPerPage - 1) / limitPerPage

                    // Chuyển đổi dữ liệu (Mapping)
                    val cleanResponse = SavedPostsResponse(
                        success = rawResponse.success,
                        data = rawResponse.data?.data ?: emptyList(),
                        page = rawResponse.data?.page ?: 1,
                        totalPages = kotlin.math.max(1, calculatedTotalPages),
                        message = rawResponse.message
                    )

                    Result.success(cleanResponse)
                } else {
                    Result.failure(Exception("Lỗi khi tải bài đăng đã lưu: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun unsavePost(postId: Int): Result<Boolean> {
        // 👇 SỬA LẠI: Dùng Dispatchers.Default
        return withContext(Dispatchers.Default) {
            try {
                val token = TokenManager.getToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("Chưa đăng nhập"))
                }

                val response = client.post("$baseUrl/posts/$postId/save") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                }

                if (response.status == HttpStatusCode.OK) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Lỗi khi bỏ lưu bài đăng"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun likePost(postId: Int): Result<Boolean> {
        // 👇 SỬA LẠI: Dùng Dispatchers.Default
        return withContext(Dispatchers.Default) {
            try {
                val token = TokenManager.getToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("Chưa đăng nhập"))
                }

                val response = client.post("$baseUrl/posts/$postId/like") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                }

                if (response.status == HttpStatusCode.OK) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Lỗi khi thích bài đăng"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}