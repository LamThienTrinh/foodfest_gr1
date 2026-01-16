package com.foodfest.app.features.follow

import com.foodfest.app.core.response.ApiResponse
import com.foodfest.app.core.exception.toAppStatus
import com.foodfest.app.plugins.userId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

// Response DTOs
@Serializable
data class FollowCheckData(val isFollowing: Boolean)

fun Route.followRoutes(followService: FollowService) {
    route("/api/users") {
        authenticate("auth-jwt") {
            // Toggle follow a user
            post("/{userId}/follow") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))
                
                val targetUserId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid user id"))
                
                followService.toggleFollow(principal.userId, targetUserId)
                    .onSuccess { result ->
                        val message = if (result.isFollowing) "Đã theo dõi" else "Đã hủy theo dõi"
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result, message))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to toggle follow"))
                    }
            }
            
            // Check if following a user
            get("/{userId}/is-following") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))
                
                val targetUserId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid user id"))
                
                followService.isFollowing(principal.userId, targetUserId)
                    .onSuccess { isFollowing ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(FollowCheckData(isFollowing)))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to check follow status"))
                    }
            }
            
            // Get followers of a user
            get("/{userId}/followers") {
                val principal = call.principal<JWTPrincipal>()
                
                val targetUserId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid user id"))
                
                val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                
                followService.getFollowers(targetUserId, principal?.userId, page)
                    .onSuccess { result ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get followers"))
                    }
            }
            
            // Get following of a user
            get("/{userId}/following") {
                val principal = call.principal<JWTPrincipal>()
                
                val targetUserId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid user id"))
                
                val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                
                followService.getFollowing(targetUserId, principal?.userId, page)
                    .onSuccess { result ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get following"))
                    }
            }
        }
    }
}
