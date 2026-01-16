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

// =============================================
// ROUTES (API Endpoints)
// =============================================
fun Route.AuthRoutes(authService: AuthService) {
    route("/api/auth") {
        // Public routes (không cần token)
        post("/register") {
            val request = call.receive<RegisterRequest>()
            
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
            val request = call.receive<LoginRequest>()
            
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
}
