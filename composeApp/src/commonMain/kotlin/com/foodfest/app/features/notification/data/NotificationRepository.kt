package com.foodfest.app.features.notification.data

import com.foodfest.app.core.network.NetworkClient
import com.foodfest.app.core.storage.TokenManager
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
private data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

class NotificationRepository {
    private val client = NetworkClient.httpClient
    private val baseUrl = NetworkClient.BASE_URL

    private fun authHeaders(): Map<String, String> {
        val token = TokenManager.getToken()
        return if (token != null) mapOf("Authorization" to "Bearer $token") else emptyMap()
    }

    suspend fun getNotifications(limit: Int = 50): Result<List<AppNotification>> {
        return try {
            val response = client.get("$baseUrl/api/notifications") {
                authHeaders().forEach { (key, value) -> header(key, value) }
                parameter("limit", limit)
            }

            val apiResponse = response.body<ApiResponse<List<AppNotification>>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Không tải được thông báo"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadCount(): Result<Int> {
        return try {
            val response = client.get("$baseUrl/api/notifications/unread-count") {
                authHeaders().forEach { (key, value) -> header(key, value) }
            }

            val apiResponse = response.body<ApiResponse<NotificationUnreadCount>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data.unreadCount)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Không tải được số thông báo"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markRead(notificationId: Int): Result<AppNotification> {
        return try {
            val response = client.put("$baseUrl/api/notifications/$notificationId/read") {
                authHeaders().forEach { (key, value) -> header(key, value) }
            }

            val apiResponse = response.body<ApiResponse<AppNotification>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Không thể đánh dấu đã đọc"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllRead(): Result<Int> {
        return try {
            val response = client.put("$baseUrl/api/notifications/read-all") {
                authHeaders().forEach { (key, value) -> header(key, value) }
            }

            val apiResponse = response.body<ApiResponse<NotificationUnreadCount>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data.unreadCount)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Không thể đánh dấu đã đọc"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registers the native push token after Android/iOS provides one.
     */
    suspend fun registerPushDeviceToken(platform: String, token: String): Result<PushDeviceToken> {
        return try {
            val response = client.post("$baseUrl/api/notifications/device-tokens") {
                contentType(ContentType.Application.Json)
                authHeaders().forEach { (key, value) -> header(key, value) }
                setBody(RegisterPushDeviceTokenRequest(platform = platform, token = token))
            }

            val apiResponse = response.body<ApiResponse<PushDeviceToken>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Không đăng ký được push token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deactivates the native push token when the app logs out or token changes.
     */
    suspend fun deactivatePushDeviceToken(platform: String, token: String): Result<Boolean> {
        return try {
            val response = client.delete("$baseUrl/api/notifications/device-tokens") {
                contentType(ContentType.Application.Json)
                authHeaders().forEach { (key, value) -> header(key, value) }
                setBody(RegisterPushDeviceTokenRequest(platform = platform, token = token))
            }

            val apiResponse = response.body<ApiResponse<Unit>>()
            if (apiResponse.success) {
                Result.success(true)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Không hủy được push token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
