package com.foodfest.app.features.notification

import kotlinx.serialization.Serializable

@Serializable
data class RegisterPushDeviceTokenRequest(
    val platform: String,
    val token: String
)

class NotificationService(
    private val repository: NotificationRepository = NotificationRepository(),
    private val pushService: PushNotificationService = PushNotificationService()
) {
    private fun normalizePlatform(platform: String): String {
        val normalized = platform.trim().lowercase()
        require(normalized in setOf("android", "ios", "web")) { "platform must be android, ios, or web" }
        return normalized
    }

    suspend fun listMyNotifications(userId: Int, limit: Int): Result<List<AppNotification>> = runCatching {
        repository.syncPantryExpiryNotifications(userId)
        repository.listForUser(userId, limit)
    }

    suspend fun getUnreadCount(userId: Int): Result<NotificationUnreadCount> = runCatching {
        repository.syncPantryExpiryNotifications(userId)
        NotificationUnreadCount(repository.unreadCount(userId))
    }

    suspend fun markRead(userId: Int, notificationId: Int): Result<AppNotification> = runCatching {
        require(notificationId > 0) { "Invalid notification id" }
        repository.markRead(userId, notificationId)
    }

    suspend fun markAllRead(userId: Int): Result<NotificationUnreadCount> = runCatching {
        val updatedCount = repository.markAllRead(userId)
        NotificationUnreadCount(updatedCount)
    }

    suspend fun registerDeviceToken(
        userId: Int,
        request: RegisterPushDeviceTokenRequest
    ): Result<PushDeviceToken> = runCatching {
        val platform = normalizePlatform(request.platform)
        val token = request.token.trim()
        require(token.length in 20..4096) { "Invalid device token" }
        repository.registerDeviceToken(userId, platform, token)
    }

    suspend fun deactivateDeviceToken(
        userId: Int,
        request: RegisterPushDeviceTokenRequest
    ): Result<Boolean> = runCatching {
        val platform = normalizePlatform(request.platform)
        val token = request.token.trim()
        require(token.isNotBlank()) { "Invalid device token" }
        repository.deactivateDeviceToken(userId, platform, token)
    }

    /**
     * Phase 6 scheduler entry point: scan all pantry expiry events and push only newly inserted notifications.
     */
    suspend fun runPantryExpiryBackgroundJob(): Result<NotificationJobResult> = runCatching {
        val jobRunId = repository.startJobRun("pantry_expiry")
        var insertedCount = 0
        var pushAttemptedCount = 0
        var pushSentCount = 0

        try {
            val syncResult = repository.syncPantryExpiryNotificationsForAllUsers()
            insertedCount = syncResult.insertedCount

            for (notification in syncResult.insertedNotifications) {
                val targets = repository.listActiveDeviceTokens(notification.userId)
                if (targets.isEmpty()) {
                    repository.logPushDelivery(
                        notificationId = notification.id,
                        userId = notification.userId,
                        deviceTokenId = null,
                        provider = pushService.providerName,
                        status = "skipped",
                        responseMessage = "No active device token"
                    )
                    continue
                }

                for (target in targets) {
                    pushAttemptedCount += 1
                    val result = pushService.send(notification, target)
                    if (result.sent) {
                        pushSentCount += 1
                    }
                    repository.logPushDelivery(
                        notificationId = notification.id,
                        userId = notification.userId,
                        deviceTokenId = target.id,
                        provider = pushService.providerName,
                        status = result.status,
                        responseMessage = result.responseMessage
                    )
                }
            }

            repository.finishJobRun(
                jobRunId = jobRunId,
                status = "success",
                insertedCount = insertedCount,
                pushAttemptedCount = pushAttemptedCount,
                pushSentCount = pushSentCount
            )

            NotificationJobResult(
                insertedCount = insertedCount,
                pushAttemptedCount = pushAttemptedCount,
                pushSentCount = pushSentCount
            )
        } catch (error: Exception) {
            repository.finishJobRun(
                jobRunId = jobRunId,
                status = "failed",
                insertedCount = insertedCount,
                pushAttemptedCount = pushAttemptedCount,
                pushSentCount = pushSentCount,
                errorMessage = error.message ?: "Pantry expiry background job failed"
            )
            throw error
        }
    }
}
