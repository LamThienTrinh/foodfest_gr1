package com.foodfest.app.features.favorite.data

import com.foodfest.app.core.network.NetworkClient
import com.foodfest.app.core.storage.TokenManager
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class FavoriteDish(
    val dishId: Int,
    val dishName: String,
    val imageUrl: String? = null,
    val savedAt: String
)

@Serializable
data class PaginatedFavorites(
    val data: List<FavoriteDish>,
    val page: Int,
    val limit: Int,
    val total: Int
)

@Serializable
data class FavoriteIdsResponse(
    val ids: List<Int>
)

@Serializable
data class FavoriteCheckResponse(
    val isFavorite: Boolean
)

@Serializable
data class FavoriteToggleResponse(
    val isFavorite: Boolean,
    val message: String
)

@Serializable
private data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

class FavoriteRepository {
    private val client = NetworkClient.httpClient
    private val baseUrl = NetworkClient.BASE_URL
    
    /**
     * Lấy danh sách món ăn yêu thích của user
     */
    suspend fun getFavorites(page: Int = 1): Result<PaginatedFavorites> = runCatching {
        val token = TokenManager.getToken() ?: throw Exception("Chưa đăng nhập")
        
        val response = client.get("$baseUrl/api/favorites") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("page", page)
        }
        
        when (response.status) {
            HttpStatusCode.OK -> {
                val apiResponse = response.body<ApiResponse<PaginatedFavorites>>()
                apiResponse.data ?: throw Exception(apiResponse.message ?: "Không có dữ liệu")
            }
            HttpStatusCode.Unauthorized -> throw Exception("Phiên đăng nhập đã hết hạn")
            else -> throw Exception("Lỗi: ${response.status}")
        }
    }
    
    /**
     * Lấy danh sách ID các món đã favorite (để check nhanh)
     */
    suspend fun getFavoriteIds(): Result<List<Int>> = runCatching {
        val token = TokenManager.getToken() ?: throw Exception("Chưa đăng nhập")
        
        val response = client.get("$baseUrl/api/favorites/ids") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        
        when (response.status) {
            HttpStatusCode.OK -> {
                val apiResponse = response.body<ApiResponse<FavoriteIdsResponse>>()
                apiResponse.data?.ids ?: emptyList()
            }
            HttpStatusCode.Unauthorized -> throw Exception("Phiên đăng nhập đã hết hạn")
            else -> throw Exception("Lỗi: ${response.status}")
        }
    }
    
    /**
     * Check xem món có được favorite chưa
     */
    suspend fun isFavorite(dishId: Int): Result<Boolean> = runCatching {
        val token = TokenManager.getToken() ?: throw Exception("Chưa đăng nhập")
        
        val response = client.get("$baseUrl/api/favorites/check/$dishId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        
        when (response.status) {
            HttpStatusCode.OK -> {
                val apiResponse = response.body<ApiResponse<FavoriteCheckResponse>>()
                apiResponse.data?.isFavorite ?: false
            }
            HttpStatusCode.Unauthorized -> throw Exception("Phiên đăng nhập đã hết hạn")
            else -> throw Exception("Lỗi: ${response.status}")
        }
    }
    
    /**
     * Toggle favorite (thêm nếu chưa có, bỏ nếu có)
     */
    suspend fun toggleFavorite(dishId: Int): Result<Boolean> = runCatching {
        val token = TokenManager.getToken() ?: throw Exception("Chưa đăng nhập")
        
        val response = client.post("$baseUrl/api/favorites/toggle/$dishId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        
        when (response.status) {
            HttpStatusCode.OK -> {
                val apiResponse = response.body<ApiResponse<FavoriteToggleResponse>>()
                apiResponse.data?.isFavorite ?: false
            }
            HttpStatusCode.Unauthorized -> throw Exception("Phiên đăng nhập đã hết hạn")
            else -> throw Exception("Lỗi: ${response.status}")
        }
    }
    
    /**
     * Thêm vào favorites
     */
    suspend fun addFavorite(dishId: Int): Result<Unit> = runCatching {
        val token = TokenManager.getToken() ?: throw Exception("Chưa đăng nhập")
        
        val response = client.post("$baseUrl/api/favorites/$dishId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        
        when (response.status) {
            HttpStatusCode.Created, HttpStatusCode.OK -> Unit
            HttpStatusCode.Unauthorized -> throw Exception("Phiên đăng nhập đã hết hạn")
            else -> throw Exception("Lỗi: ${response.status}")
        }
    }
    
    /**
     * Bỏ khỏi favorites
     */
    suspend fun removeFavorite(dishId: Int): Result<Unit> = runCatching {
        val token = TokenManager.getToken() ?: throw Exception("Chưa đăng nhập")
        
        val response = client.delete("$baseUrl/api/favorites/$dishId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        
        when (response.status) {
            HttpStatusCode.OK -> Unit
            HttpStatusCode.NotFound -> throw Exception("Món này không có trong danh sách yêu thích")
            HttpStatusCode.Unauthorized -> throw Exception("Phiên đăng nhập đã hết hạn")
            else -> throw Exception("Lỗi: ${response.status}")
        }
    }
}
