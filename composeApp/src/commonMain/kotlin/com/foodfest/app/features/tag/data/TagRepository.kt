package com.foodfest.app.features.tag.data

import com.foodfest.app.core.network.NetworkClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val id: Int,
    val name: String,
    val type: String
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

class TagRepository {
    private val client = NetworkClient.httpClient
    private val baseUrl = NetworkClient.BASE_URL

    suspend fun getAllTags(): Result<List<Tag>> {
        return runCatching {
            val response = client.get("$baseUrl/api/tags")
            val result = response.body<ApiResponse<List<Tag>>>()
            if (result.success && result.data != null) result.data
            else throw IllegalStateException(result.message ?: "Không tải được danh sách tags")
        }
    }

    suspend fun getTagsByType(type: String): Result<List<Tag>> {
        return runCatching {
            val response = client.get("$baseUrl/api/tags") {
                parameter("type", type)
            }
            val result = response.body<ApiResponse<List<Tag>>>()
            if (result.success && result.data != null) result.data
            else throw IllegalStateException(result.message ?: "Không tải được danh sách tags")
        }
    }
}
