package com.foodfest.app.features.blindbox

import com.foodfest.app.core.exception.toAppStatus
import com.foodfest.app.core.response.ApiResponse
import com.foodfest.app.plugins.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.blindBoxRoutes(blindBoxService: BlindBoxService) {
    route("/api/blind-box") {
        authenticate("auth-jwt", optional = true) {
            get("/random") {
                val request = call.toBlindBoxRandomRequest()
                val requesterUserId = call.principal<JWTPrincipal>()?.userId

                blindBoxService.randomDish(requesterUserId, request)
                    .onSuccess { dish ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(dish))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Không thể random món"))
                    }
            }
        }
    }
}

private fun ApplicationCall.toBlindBoxRandomRequest(): BlindBoxRandomRequest {
    fun parseBool(key: String, defaultValue: Boolean): Boolean {
        return request.queryParameters[key]?.trim()?.lowercase()?.let { value ->
            value == "true" || value == "1" || value == "yes"
        } ?: defaultValue
    }

    fun parseNames(key: String): List<String> {
        return request.queryParameters[key]
            ?.split(",")
            ?.mapNotNull { it.trim().takeIf { name -> name.isNotBlank() } }
            ?: emptyList()
    }

    return BlindBoxRandomRequest(
        includeSystem = parseBool("includeSystem", true),
        includePersonal = parseBool("includePersonal", false),
        typeTags = parseNames("type"),
        tasteTags = parseNames("taste"),
        ingredientTags = parseNames("ingredient")
    )
}
