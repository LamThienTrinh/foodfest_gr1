package com.foodfest.app.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.cdimascio.dotenv.dotenv
import java.util.*

object JWTConfig {
    private val dotenv = dotenv {
        directory = "./server"
        ignoreIfMissing = true
    }
    
    private val secret = dotenv["JWT_SECRET"] ?: "your-secret-key-change-this-in-production"
    private val issuer = dotenv["JWT_ISSUER"] ?: "foodfest-api"
    private val audience = dotenv["JWT_AUDIENCE"] ?: "foodfest-users"
    
    private val validityInMs = 7 * 24 * 60 * 60 * 1000L
    private val algorithm = Algorithm.HMAC256(secret)
    
    fun generateToken(userId: Int, username: String): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("username", username)
            .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
            .sign(algorithm)
    }
    
    fun verifyToken(token: String): Int? {
        return try {
            val verifier = JWT.require(algorithm)
                .withAudience(audience)
                .withIssuer(issuer)
                .build()
            
            val jwt = verifier.verify(token)
            jwt.getClaim("userId").asInt()
        } catch (e: Exception) {
            null
        }
    }
}
