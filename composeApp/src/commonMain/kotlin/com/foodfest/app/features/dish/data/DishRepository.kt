package com.foodfest.app.features.dish.data

import com.foodfest.app.core.network.NetworkClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class DishTag(
    val id: Int,
    val name: String,
    val type: String
)

@Serializable
data class Dish(
    val id: Int,
    val name: String,
    val imageUrl: String? = null,
    val description: String? = null,
    val ingredients: String? = null,
    val instructions: String? = null,
    val prepTime: Int? = null,
    val cookTime: Int? = null,
    val serving: Int? = null,
    val tags: List<DishTag>? = null
)

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val page: Int,
    val limit: Int,
    val total: Int
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

@Serializable
data class DishImageUploadRequest(val base64Image: String)

class DishRepository {
    private val client = NetworkClient.httpClient
    private val baseUrl = NetworkClient.BASE_URL

    suspend fun getDishes(page: Int = 1): Result<PaginatedResponse<Dish>> {
        return runCatching {
            val response = client.get("$baseUrl/api/dishes") {
                parameter("page", page)
            }
            val result = response.body<ApiResponse<PaginatedResponse<Dish>>>()
            if (result.success && result.data != null) result.data
            else throw IllegalStateException(result.message ?: "Không tải được danh sách món")
        }
    }

    suspend fun getDishesWithFilter(
        page: Int = 1,
        typeTags: List<String> = emptyList(),
        tasteTags: List<String> = emptyList(),
        ingredientTags: List<String> = emptyList()
    ): Result<PaginatedResponse<Dish>> {
        return runCatching {
            val response = client.get("$baseUrl/api/dishes") {
                parameter("page", page)
                if (typeTags.isNotEmpty()) parameter("type", typeTags.joinToString(","))
                if (tasteTags.isNotEmpty()) parameter("taste", tasteTags.joinToString(","))
                if (ingredientTags.isNotEmpty()) parameter("ingredient", ingredientTags.joinToString(","))
            }
            val result = response.body<ApiResponse<PaginatedResponse<Dish>>>()
            if (result.success && result.data != null) result.data
            else throw IllegalStateException(result.message ?: "Không tải được danh sách món")
        }
    }

    suspend fun uploadDishImage(dishId: Int, base64: String): Result<String> {
        return runCatching {
            val response = client.post("$baseUrl/api/dishes/$dishId/image") {
                contentType(ContentType.Application.Json)
                setBody(DishImageUploadRequest(base64))
            }
            val result = response.body<ApiResponse<Map<String, String>>>()
            if (result.success) {
                result.data?.get("imageUrl") ?: throw IllegalStateException("Thiếu imageUrl trong response")
            } else {
                throw IllegalStateException(result.message ?: "Upload thất bại")
            }
        }
    }

    suspend fun getRandomDish(
        typeTags: List<String> = emptyList(),
        tasteTags: List<String> = emptyList(),
        ingredientTags: List<String> = emptyList()
    ): Result<Dish> {
        return runCatching {
            val response = client.get("$baseUrl/api/dishes/random") {
                parameter("count", 1)
                if (typeTags.isNotEmpty()) parameter("type", typeTags.joinToString(","))
                if (tasteTags.isNotEmpty()) parameter("taste", tasteTags.joinToString(","))
                if (ingredientTags.isNotEmpty()) parameter("ingredient", ingredientTags.joinToString(","))
            }
            val result = response.body<ApiResponse<List<Dish>>>()
            if (result.success) {
                val dishes = result.data.orEmpty()
                dishes.firstOrNull() ?: throw IllegalStateException("Không có món nào phù hợp")
            } else {
                throw IllegalStateException(result.message ?: "Quay thất bại")
            }
        }
    }
    
    suspend fun getDishById(dishId: Int): Result<Dish> {
        return runCatching {
            val response = client.get("$baseUrl/api/dishes/$dishId")
            val result = response.body<ApiResponse<Dish>>()
            if (result.success && result.data != null) result.data
            else throw IllegalStateException(result.message ?: "Không tìm thấy món ăn")
        }
    }
}
