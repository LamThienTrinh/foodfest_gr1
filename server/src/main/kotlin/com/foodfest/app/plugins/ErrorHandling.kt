package com.foodfest.app.plugins

import com.foodfest.app.core.exception.AppException
import com.foodfest.app.core.exception.toAppStatus
import com.foodfest.app.core.response.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.*
import io.ktor.server.response.*

private fun ApplicationCall.errorContext(): String {
    val principal = principal<JWTPrincipal>()
    val authUserId = principal?.userId?.toString() ?: "anonymous"
    val pathUserId = parameters["userId"] ?: "-"
    val postId = parameters["postId"] ?: "-"
    val commentId = parameters["commentId"] ?: "-"
    val correlationId = callId ?: "-"

    return "method=${request.httpMethod.value} uri=${request.uri} correlationId=$correlationId authUserId=$authUserId pathUserId=$pathUserId postId=$postId commentId=$commentId"
}

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<AppException> { call, cause ->
            call.application.environment.log.warn("AppException: ${cause.message}; ${call.errorContext()}")
            call.respond(cause.status, ApiResponse.error<Unit>(cause.message))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.application.environment.log.warn("IllegalArgumentException: ${cause.message}; ${call.errorContext()}")
            call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>(cause.message ?: "Invalid request"))
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled exception; ${call.errorContext()}", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiResponse.error<Unit>("Internal server error"))
        }
    }
}
