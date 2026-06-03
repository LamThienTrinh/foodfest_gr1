package com.foodfest.app.features.notification

/**
 * Thin scheduler facade so the Ktor plugin can trigger Phase 6 pantry expiry jobs.
 */
class PantryExpiryScheduler(
    private val notificationService: NotificationService
) {
    /**
     * Runs one pantry expiry scan and push delivery attempt.
     */
    suspend fun runOnce(): Result<NotificationJobResult> {
        return notificationService.runPantryExpiryBackgroundJob()
    }
}
