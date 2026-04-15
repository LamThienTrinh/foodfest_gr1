package com.foodfest.app.features.auth

import com.foodfest.app.core.response.ApiResponse
import com.foodfest.app.core.exception.toAppStatus
import com.foodfest.app.plugins.userId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
private data class RegisterRequestCompat(
    val username: String? = null,
    val email: String? = null,
    val password: String? = null,
    val fullName: String? = null,
    val name: String? = null,
    val avatarBase64: String? = null
)

@Serializable
private data class LoginRequestCompat(
    val username: String? = null,
    val email: String? = null,
    val password: String? = null
)

// =============================================
// ROUTES (API Endpoints)
// =============================================
fun Route.AuthRoutes(authService: AuthService) {
    route("/api/auth") {
        // Public routes (không cần token)
        post("/register") {
            val requestCompat = runCatching { call.receive<RegisterRequestCompat>() }
                .getOrElse {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.error<Unit>("Invalid request body")
                    )
                }

            val username = requestCompat.username ?: requestCompat.email
            val fullName = requestCompat.fullName ?: requestCompat.name
            val password = requestCompat.password

            if (username.isNullOrBlank() || fullName.isNullOrBlank() || password.isNullOrBlank()) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.error<Unit>("username/email, password and fullName/name are required")
                )
            }

            val request = RegisterRequest(
                username = username,
                password = password,
                fullName = fullName,
                avatarBase64 = requestCompat.avatarBase64
            )
            
            authService.register(request)
                .onSuccess { authResponse ->
                    call.respond(HttpStatusCode.Created, ApiResponse.success(authResponse))
                }
                .onFailure { error ->
                    call.respond(
                        error.toAppStatus(),
                        ApiResponse.error<Unit>(error.message ?: "Registration failed")
                    )
                }
        }
        
        post("/login") {
            val requestCompat = runCatching { call.receive<LoginRequestCompat>() }
                .getOrElse {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.error<Unit>("Invalid request body")
                    )
                }

            val username = requestCompat.username ?: requestCompat.email
            val password = requestCompat.password

            if (username.isNullOrBlank() || password.isNullOrBlank()) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.error<Unit>("username/email and password are required")
                )
            }

            val request = LoginRequest(username = username, password = password)
            
            authService.login(request)
                .onSuccess { authResponse ->
                    call.respond(HttpStatusCode.OK, ApiResponse.success(authResponse))
                }
                .onFailure { error ->
                    call.respond(
                        error.toAppStatus(),
                        ApiResponse.error<Unit>(error.message ?: "Login failed")
                    )
                }
        }
        
        // Protected routes (cần token)
        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.userId
                
                authService.getProfile(userId)
                    .onSuccess { user ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(user))
                    }
                    .onFailure { error ->
                        call.respond(
                            error.toAppStatus(),
                            ApiResponse.error<Unit>(error.message ?: "User not found")
                        )
                    }
            }
            
            // Update profile (name)
            put("/profile") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.userId
                val request = call.receive<UpdateProfileRequest>()
                
                authService.updateProfile(userId, request)
                    .onSuccess { user ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(user))
                    }
                    .onFailure { error ->
                        call.respond(
                            error.toAppStatus(),
                            ApiResponse.error<Unit>(error.message ?: "Update failed")
                        )
                    }
            }
            
            // Change password
            put("/password") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.userId
                val request = call.receive<ChangePasswordRequest>()
                
                authService.changePassword(userId, request)
                    .onSuccess {
                        call.respond(HttpStatusCode.OK, ApiResponse.success(mapOf("message" to "Password changed successfully")))
                    }
                    .onFailure { error ->
                        call.respond(
                            error.toAppStatus(),
                            ApiResponse.error<Unit>(error.message ?: "Password change failed")
                        )
                    }
            }
            
            // Update avatar
            put("/avatar") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.userId
                val request = call.receive<UpdateAvatarRequest>()
                
                authService.updateAvatar(userId, request)
                    .onSuccess { user ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(user))
                    }
                    .onFailure { error ->
                        call.respond(
                            error.toAppStatus(),
                            ApiResponse.error<Unit>(error.message ?: "Avatar update failed")
                        )
                    }
            }
        }
    }

    route("/api/users") {
        authenticate("auth-jwt", optional = true) {
            get("/{userId}/profile") {
                val targetUserId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.error<Unit>("Invalid user id")
                    )

                if (targetUserId <= 0) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.error<Unit>("Invalid user id")
                    )
                }

                val principal = call.principal<JWTPrincipal>()

                authService.getPublicProfile(targetUserId, principal?.userId)
                    .onSuccess { profile ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(profile))
                    }
                    .onFailure { error ->
                        call.respond(
                            error.toAppStatus(),
                            ApiResponse.error<Unit>(error.message ?: "User not found")
                        )
                    }
            }
        }
    }
}
