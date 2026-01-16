package com.foodfest.app.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val success: Boolean = false,
    val message: String
)

fun Application.configureSecurity() {
    val dotenv = dotenv {
        directory = "./server"
        ignoreIfMissing = true
    }
    
    val secret = dotenv["JWT_SECRET"] ?: "your-secret-key-change-this-in-production"
    val issuer = dotenv["JWT_ISSUER"] ?: "foodfest-api"
    val audience = dotenv["JWT_AUDIENCE"] ?: "foodfest-users"
    val realm = dotenv["JWT_REALM"] ?: "FoodFest API"
    
    install(Authentication) {
        jwt("auth-jwt") {
            this.realm = realm
            
            verifier(
                JWT.require(Algorithm.HMAC256(secret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
            )
            
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asInt()
                val username = credential.payload.getClaim("username").asString()
                
                if (userId != null && username != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(message = "Token is invalid or expired")
                )
            }
        }
    }
}

// Extension để lấy userId từ JWT Principal
val JWTPrincipal.userId: Int
    get() = payload.getClaim("userId").asInt()

val JWTPrincipal.username: String
    get() = payload.getClaim("username").asString()
