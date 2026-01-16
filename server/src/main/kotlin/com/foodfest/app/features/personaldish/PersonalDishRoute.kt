package com.foodfest.app.features.personaldish

import com.foodfest.app.core.response.ApiResponse
import com.foodfest.app.core.exception.toAppStatus
import com.foodfest.app.plugins.userId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

// Response DTOs
@Serializable
data class PersonalDishCheckData(
    val hasSaved: Boolean,
    val personalDish: PersonalDish? = null
)

@Serializable
data class MessageData(val message: String)

fun Route.personalDishRoutes(personalDishService: PersonalDishService) {
    route("/api/my-dishes") {
        authenticate("auth-jwt") {
            // Get all user's personal dishes
            get {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))
                
                val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                
                personalDishService.getByUser(principal.userId, page)
                    .onSuccess { result ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get dishes"))
                    }
            }
            
            // Create a new personal dish (save with custom recipe)
            post {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))
                
                val request = runCatching { call.receive<CreatePersonalDishRequest>() }
                    .getOrElse {
                        return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))
                    }
                
                personalDishService.create(principal.userId, request)
                    .onSuccess { dish ->
                        call.respond(HttpStatusCode.Created, ApiResponse.success(dish, "Đã lưu món ăn của bạn"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to save dish"))
                    }
            }
            
            // Check if user has saved a personal version of an original dish
            get("/check/{originalDishId}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))
                
                val originalDishId = call.parameters["originalDishId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid dish id"))
                
                personalDishService.checkSaved(principal.userId, originalDishId)
                    .onSuccess { personalDish ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(
                            PersonalDishCheckData(
                                hasSaved = (personalDish != null),
                                personalDish = personalDish
                            )
                        ))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to check"))
                    }
            }
            
            // Get a specific personal dish
            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))
                
                val dishId = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid dish id"))
                
                personalDishService.getById(dishId, principal.userId)
                    .onSuccess { dish ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(dish))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Dish not found"))
                    }
            }
            
            // Update a personal dish
            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))
                
                val dishId = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid dish id"))
                
                val request = runCatching { call.receive<UpdatePersonalDishRequest>() }
                    .getOrElse {
                        return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))
                    }
                
                personalDishService.update(dishId, principal.userId, request)
                    .onSuccess { dish ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(dish, "Đã cập nhật món ăn"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to update"))
                    }
            }
            
            // Delete a personal dish
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))
                
                val dishId = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid dish id"))
                
                personalDishService.delete(dishId, principal.userId)
                    .onSuccess {
                        call.respond(HttpStatusCode.OK, ApiResponse.success(MessageData("Đã xóa món ăn")))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to delete"))
                    }
            }
        }
    }
}
