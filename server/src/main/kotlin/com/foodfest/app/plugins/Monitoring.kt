package com.foodfest.app.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import org.slf4j.event.Level
import java.util.UUID

fun Application.configureMonitoring() {
    install(CallId) {
        retrieveFromHeader("X-Correlation-ID")
        retrieveFromHeader("X-Request-ID")
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() && it.length <= 128 }
        replyToHeader("X-Correlation-ID")
    }

    install(CallLogging) {
        level = Level.INFO
        callIdMdc("correlationId")
    }
}
