package com.foodfest.app.features.dish

import com.foodfest.app.core.response.ApiResponse
import com.foodfest.app.core.exception.toAppStatus
import com.foodfest.app.features.dish.DishService.DishFilterParams
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

@kotlinx.serialization.Serializable
data class DishImageUploadRequest(val base64Image: String)

fun Route.dishRoutes(dishService: DishService) {
    route("/api/dishes") {
        get {
            val filter = call.toDishFilterParams()
            dishService.listDishes(filter)
                .onSuccess { result ->
                    call.respond(HttpStatusCode.OK, ApiResponse.success(result))
                }
                .onFailure { error ->
                    call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to load dishes"))
                }
        }

        get("/random") {
            val filter = call.toDishFilterParams()
            val count = call.request.queryParameters["count"]?.toIntOrNull() ?: 10

            dishService.randomDishes(filter, count)
                .onSuccess { dishes ->
                    call.respond(HttpStatusCode.OK, ApiResponse.success(dishes))
                }
                .onFailure { error ->
                    call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to random dishes"))
                }
        }

        get("/search") {
            val keyword = call.request.queryParameters["keyword"]?.trim().orEmpty()
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1

            dishService.search(keyword, page)
                .onSuccess { result ->
                    call.respond(HttpStatusCode.OK, ApiResponse.success(result))
                }
                .onFailure { error ->
                    call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to search dishes"))
                }
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid dish id"))

            dishService.getDish(id)
                .onSuccess { dish ->
                    call.respond(HttpStatusCode.OK, ApiResponse.success(dish))
                }
                .onFailure { error ->
                    call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Dish not found"))
                }
        }

        post("/{id}/image") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid dish id"))

            val request = call.receive<DishImageUploadRequest>()

            dishService.uploadDishImage(id, request.base64Image)
                .onSuccess { url ->
                    call.respond(HttpStatusCode.OK, ApiResponse.success(mapOf("imageUrl" to url)))
                }
                .onFailure { error ->
                    call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to upload image"))
                }
        }
    }
}

private fun ApplicationCall.toDishFilterParams(): DishFilterParams {
    fun parseIds(key: String): List<Int> = request.queryParameters[key]
        ?.split(",")
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?.filter { it > 0 }
        ?: emptyList()

    fun parseNames(key: String): List<String> = request.queryParameters[key]
        ?.split(",")
        ?.mapNotNull { it.trim().takeIf { name -> name.isNotBlank() } }
        ?: emptyList()

    val page = request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val limit = 5 // enforced later but kept for completeness

    return DishFilterParams(
        tagIds = parseIds("tags"),
        typeTags = parseNames("type"),
        tasteTags = parseNames("taste"),
        ingredientTags = parseNames("ingredient"),
        seasonTags = parseNames("season"),
        page = page,
        limit = limit
    )
}
