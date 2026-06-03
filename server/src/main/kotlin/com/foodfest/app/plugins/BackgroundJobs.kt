package com.foodfest.app.plugins

import com.foodfest.app.features.notification.PantryExpiryScheduler
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.util.concurrent.TimeUnit

/**
 * Starts backend background jobs after DB/routing are configured.
 */
fun Application.configureBackgroundJobs() {
    val dotenv = dotenv {
        directory = "./server"
        ignoreIfMissing = true
    }

    fun readConfig(key: String): String? {
        return System.getenv(key)?.takeIf { it.isNotBlank() }
            ?: dotenv[key]?.takeIf { it.isNotBlank() }
    }

    val schedulerEnabled = readConfig("PANTRY_EXPIRY_SCHEDULER_ENABLED")
        ?.equals("false", ignoreCase = true) != true
    if (!schedulerEnabled) {
        environment.log.info("Pantry expiry scheduler disabled by PANTRY_EXPIRY_SCHEDULER_ENABLED=false")
        return
    }

    val intervalHours = readConfig("PANTRY_EXPIRY_SCHEDULER_INTERVAL_HOURS")
        ?.toLongOrNull()
        ?.coerceAtLeast(1)
        ?: 12L
    val runOnStartup = readConfig("PANTRY_EXPIRY_SCHEDULER_RUN_ON_STARTUP")
        ?.equals("false", ignoreCase = true) != true

    val scheduler = GlobalContext.get().get<PantryExpiryScheduler>()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    environment.monitor.subscribe(ApplicationStopping) {
        scope.cancel()
    }

    scope.launch {
        if (runOnStartup) {
            runPantryExpiryJob(scheduler, trigger = "startup")
        }

        val intervalMs = TimeUnit.HOURS.toMillis(intervalHours)
        while (isActive) {
            delay(intervalMs)
            runPantryExpiryJob(scheduler, trigger = "interval")
        }
    }

    environment.log.info("Pantry expiry scheduler enabled; interval=${intervalHours}h, runOnStartup=$runOnStartup")
}

/**
 * Runs the scheduler safely; job failures are logged and do not stop the application.
 */
private suspend fun Application.runPantryExpiryJob(
    scheduler: PantryExpiryScheduler,
    trigger: String
) {
    scheduler.runOnce()
        .onSuccess { result ->
            environment.log.info(
                "Pantry expiry job ($trigger) success: inserted=${result.insertedCount}, " +
                    "pushAttempted=${result.pushAttemptedCount}, pushSent=${result.pushSentCount}"
            )
        }
        .onFailure { error ->
            environment.log.error("Pantry expiry job ($trigger) failed: ${error.message}", error)
        }
}
