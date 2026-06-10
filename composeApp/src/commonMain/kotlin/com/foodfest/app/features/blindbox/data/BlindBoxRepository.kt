package com.foodfest.app.features.blindbox.data

import com.foodfest.app.core.network.NetworkClient
import com.foodfest.app.core.storage.TokenManager
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

@Serializable
data class BlindBoxDishResult(
    val id: Int,
    val sourceType: String,
    val name: String,
    val imageUrl: String? = null,
    val tags: List<String> = emptyList()
)

@Serializable
private data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

class BlindBoxRepository {
    private val client = NetworkClient.httpClient
    private val baseUrl = NetworkClient.BASE_URL

    suspend fun randomDish(
        includeSystem: Boolean,
        includePersonal: Boolean,
        typeTags: List<String>,
        tasteTags: List<String>,
        ingredientTags: List<String>
    ): Result<BlindBoxDishResult> = runCatching {
        val response = client.get("$baseUrl/api/blind-box/random") {
            parameter("includeSystem", includeSystem)
            parameter("includePersonal", includePersonal)
            if (typeTags.isNotEmpty()) parameter("type", typeTags.joinToString(","))
            if (tasteTags.isNotEmpty()) parameter("taste", tasteTags.joinToString(","))
            if (ingredientTags.isNotEmpty()) parameter("ingredient", ingredientTags.joinToString(","))

            TokenManager.getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }

        val apiResponse = response.body<ApiResponse<BlindBoxDishResult>>()
        if (apiResponse.success && apiResponse.data != null) {
            apiResponse.data
        } else {
            throw IllegalStateException(apiResponse.message ?: "Không thể random món")
        }
    }
}
