package com.foodfest.app.features.notification

import com.foodfest.app.core.exception.toAppStatus
import com.foodfest.app.core.response.ApiResponse
import com.foodfest.app.plugins.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.notificationRoutes(notificationService: NotificationService) {
    route("/api/notifications") {
        authenticate("auth-jwt") {
            get {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                notificationService.listMyNotifications(principal.userId, limit)
                    .onSuccess { notifications ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(notifications))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to load notifications"))
                    }
            }

            get("/unread-count") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                notificationService.getUnreadCount(principal.userId)
                    .onSuccess { result ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to load notifications"))
                    }
            }

            put("/{notificationId}/read") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val notificationId = call.parameters["notificationId"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid notification id"))

                notificationService.markRead(principal.userId, notificationId)
                    .onSuccess { notification ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(notification, "Notification marked as read"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to mark notification read"))
                    }
            }

            put("/read-all") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                notificationService.markAllRead(principal.userId)
                    .onSuccess { result ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result, "Notifications marked as read"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to mark notifications read"))
                    }
            }

            post("/device-tokens") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val request = runCatching { call.receive<RegisterPushDeviceTokenRequest>() }.getOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                notificationService.registerDeviceToken(principal.userId, request)
                    .onSuccess { token ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(token, "Device token registered"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to register device token"))
                    }
            }

            delete("/device-tokens") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val request = runCatching { call.receive<RegisterPushDeviceTokenRequest>() }.getOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                notificationService.deactivateDeviceToken(principal.userId, request)
                    .onSuccess {
                        call.respond(HttpStatusCode.OK, ApiResponse.success(Unit, "Device token deactivated"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to deactivate device token"))
                    }
            }
        }
    }
}
