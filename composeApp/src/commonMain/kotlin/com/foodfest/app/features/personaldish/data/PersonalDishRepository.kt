package com.foodfest.app.features.personaldish.data

import com.foodfest.app.core.network.NetworkClient
import com.foodfest.app.core.storage.TokenManager
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class PersonalDish(
    val id: Int,
    val userId: Int,
    val originalDishId: Int? = null,
    val dishName: String,
    val imageUrl: String? = null,
    val description: String? = null,
    val ingredients: String? = null,
    val instructions: String? = null,
    val prepTime: Int? = null,
    val cookTime: Int? = null,
    val serving: Int? = null,
    val note: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: String
)

@Serializable
data class PaginatedPersonalDishes(
    val data: List<PersonalDish>,
    val page: Int,
    val limit: Int,
    val total: Int
)

@Serializable
data class CreatePersonalDishRequest(
    val originalDishId: Int? = null,
    val dishName: String,
    val imageUrl: String? = null,
    val description: String? = null,
    val ingredients: String? = null,
    val instructions: String? = null,
    val prepTime: Int? = null,
    val cookTime: Int? = null,
    val serving: Int? = null,
    val note: String? = null,
    val tagIds: List<Int>? = null
)

@Serializable
data class UpdatePersonalDishRequest(
    val dishName: String? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val ingredients: String? = null,
    val instructions: String? = null,
    val prepTime: Int? = null,
    val cookTime: Int? = null,
    val serving: Int? = null,
    val note: String? = null,
    val tagIds: List<Int>? = null
)

@Serializable
data class CheckSavedResponse(
    val hasSaved: Boolean,
    val personalDish: PersonalDish? = null
)

@Serializable
private data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

class PersonalDishRepository {
    private val client = NetworkClient.httpClient
    private val baseUrl = NetworkClient.BASE_URL
    
    /**
     * Lấy danh sách món ăn cá nhân của user
     */
    suspend fun getMyDishes(page: Int = 1): Result<PaginatedPersonalDishes> = runCatching {
        val token = TokenManager.getToken() ?: throw Exception("Chưa đăng nhập")
        
        val response = client.get("$baseUrl/api/my-dishes") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("page", page)
        }
        
        when (response.status) {
            HttpStatusCode.OK -> {
                val apiResponse = response.body<ApiResponse<PaginatedPersonalDishes>>()
                apiResponse.data ?: throw Exception(apiResponse.message ?: "Không có dữ liệu")
            }
            HttpStatusCode.Unauthorized -> throw Exception("Phiên đăng nhập đã hết hạn")
            else -> throw Exception("Lỗi: ${response.status}")
        }
    }
    
    /**
     * Lấy chi tiết một món ăn cá nhân
     */
    suspend fun getById(id: Int): Result<PersonalDish> = runCatching {
        val token = TokenManager.getToken() ?: throw Exception("Chưa đăng nhập")
        
        val response = client.get("$baseUrl/api/my-dishes/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        
        when (response.status) {
            HttpStatusCode.OK -> {
                val apiResponse = response.body<ApiResponse<PersonalDish>>()
                apiResponse.data ?: throw Exception(apiResponse.message ?: "Không tìm thấy món ăn")
            }
            HttpStatusCode.NotFound -> throw Exception("Không tìm thấy món ăn")
            HttpStatusCode.Unauthorized -> throw Exception("Phiên đăng nhập đã hết hạn")
            else -> throw Exception("Lỗi: ${response.status}")
        }
    }
    
    /**
     * Check xem user đã lưu công thức riêng từ món gốc chưa
     */
    suspend fun checkSaved(originalDishId: Int): Result<CheckSavedResponse> = runCatching {
        val token = TokenManager.getToken() ?: throw Exception("Chưa đăng nhập")
        
        val response = client.get("$baseUrl/api/my-dishes/check/$originalDishId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        
        when (response.status) {
            HttpStatusCode.OK -> {
                val apiResponse = response.body<ApiResponse<CheckSavedResponse>>()
                apiResponse.data ?: CheckSavedResponse(hasSaved = false)
            }
            HttpStatusCode.Unauthorized -> throw Exception("Phiên đăng nhập đã hết hạn")
            else -> throw Exception("Lỗi: ${response.status}")
        }
    }
    
    /**
     * Tạo món ăn cá nhân mới (lưu với công thức riêng)
     */
    suspend fun create(request: CreatePersonalDishRequest): Result<PersonalDish> = runCatching {
        val token = TokenManager.getToken() ?: throw Exception("Chưa đăng nhập")
        
        val response = client.post("$baseUrl/api/my-dishes") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        
        when (response.status) {
            HttpStatusCode.Created -> {
                val apiResponse = response.body<ApiResponse<PersonalDish>>()
                apiResponse.data ?: throw Exception(apiResponse.message ?: "Lỗi khi lưu món ăn")
            }
            HttpStatusCode.BadRequest -> {
                val apiResponse = response.body<ApiResponse<Unit>>()
                throw Exception(apiResponse.message ?: "Dữ liệu không hợp lệ")
            }
            HttpStatusCode.Unauthorized -> throw Exception("Phiên đăng nhập đã hết hạn")
            else -> throw Exception("Lỗi: ${response.status}")
        }
    }
    
    /**
     * Cập nhật món ăn cá nhân
     */
    suspend fun update(id: Int, request: UpdatePersonalDishRequest): Result<PersonalDish> = runCatching {
        val token = TokenManager.getToken() ?: throw Exception("Chưa đăng nhập")
        
        val response = client.put("$baseUrl/api/my-dishes/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        
        when (response.status) {
            HttpStatusCode.OK -> {
                val apiResponse = response.body<ApiResponse<PersonalDish>>()
                apiResponse.data ?: throw Exception(apiResponse.message ?: "Lỗi khi cập nhật")
            }
            HttpStatusCode.NotFound -> throw Exception("Không tìm thấy món ăn")
            HttpStatusCode.Unauthorized -> throw Exception("Phiên đăng nhập đã hết hạn")
            else -> throw Exception("Lỗi: ${response.status}")
        }
    }
    
    /**
     * Xóa món ăn cá nhân
     */
    suspend fun delete(id: Int): Result<Unit> = runCatching {
        val token = TokenManager.getToken() ?: throw Exception("Chưa đăng nhập")
        
        val response = client.delete("$baseUrl/api/my-dishes/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        
        when (response.status) {
            HttpStatusCode.OK -> Unit
            HttpStatusCode.NotFound -> throw Exception("Không tìm thấy món ăn")
            HttpStatusCode.Unauthorized -> throw Exception("Phiên đăng nhập đã hết hạn")
            else -> throw Exception("Lỗi: ${response.status}")
        }
    }
}
