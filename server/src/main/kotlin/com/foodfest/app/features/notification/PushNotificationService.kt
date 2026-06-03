package com.foodfest.app.features.notification

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class PushSendResult(
    val sent: Boolean,
    val status: String,
    val responseMessage: String
)

@Serializable
private data class FcmNotificationPayload(
    val title: String,
    val body: String
)

@Serializable
private data class FcmLegacyRequest(
    val to: String,
    val notification: FcmNotificationPayload,
    val data: Map<String, String>
)

/**
 * Phase 6 push sender. FCM is optional at runtime; missing config logs skipped delivery.
 */
class PushNotificationService {
    val providerName: String = "fcm_legacy"

    private val client = HttpClient(CIO)
    private val dotenv = dotenv {
        directory = "./server"
        ignoreIfMissing = true
    }
    private val fcmEndpoint = readConfig("FCM_ENDPOINT")
        ?: "https://fcm.googleapis.com/fcm/send"

    private fun readConfig(key: String): String? {
        return System.getenv(key)?.takeIf { it.isNotBlank() }
            ?: dotenv[key]?.takeIf { it.isNotBlank() }
    }

    /**
     * Sends one push notification to one registered device token.
     */
    suspend fun send(notification: AppNotification, target: PushDeliveryTarget): PushSendResult {
        val serverKey = readConfig("FCM_SERVER_KEY")
        if (serverKey.isNullOrBlank()) {
            return PushSendResult(
                sent = false,
                status = "skipped",
                responseMessage = "FCM_SERVER_KEY is not configured"
            )
        }

        return try {
            val body = Json.encodeToString(
                FcmLegacyRequest(
                    to = target.token,
                    notification = FcmNotificationPayload(
                        title = notification.title,
                        body = notification.message
                    ),
                    data = mapOf(
                        "notificationId" to notification.id.toString(),
                        "type" to notification.type,
                        "actionUrl" to notification.actionUrl.orEmpty(),
                        "relatedEntityType" to notification.relatedEntityType.orEmpty(),
                        "relatedEntityId" to notification.relatedEntityId?.toString().orEmpty()
                    )
                )
            )
            val response = client.post(fcmEndpoint) {
                header(HttpHeaders.Authorization, "key=$serverKey")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            val responseText = response.bodyAsText()
            PushSendResult(
                sent = response.status.isSuccess(),
                status = if (response.status.isSuccess()) "sent" else "failed",
                responseMessage = responseText
            )
        } catch (error: Exception) {
            PushSendResult(
                sent = false,
                status = "failed",
                responseMessage = error.message ?: "Push delivery failed"
            )
        }
    }
}
