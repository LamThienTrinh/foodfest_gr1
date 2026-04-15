package com.foodfest.app.features.follow.data

import com.foodfest.app.core.network.NetworkClient
import com.foodfest.app.core.storage.TokenManager
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import kotlinx.serialization.Serializable

@Serializable
private data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

@Serializable
data class FollowResult(
    val isFollowing: Boolean,
    val followerCount: Int,
    val followingCount: Int
)

@Serializable
private data class FollowCheckData(
    val isFollowing: Boolean
)

class FollowRepository {
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

    suspend fun toggleFollow(targetUserId: Int): Result<FollowResult> {
        return try {
            val response = client.post("$baseUrl/api/users/$targetUserId/follow") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            val apiResponse = response.body<ApiResponse<FollowResult>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to toggle follow"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFollowing(targetUserId: Int): Result<Boolean> {
        return try {
            val response = client.get("$baseUrl/api/users/$targetUserId/is-following") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            val apiResponse = response.body<ApiResponse<FollowCheckData>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data.isFollowing)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to check follow status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
