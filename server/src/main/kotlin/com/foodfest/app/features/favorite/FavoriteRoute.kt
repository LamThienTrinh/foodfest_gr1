package com.foodfest.app.features.favorite

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

// Response DTOs với @Serializable
@Serializable
data class FavoriteIdsData(val ids: List<Int>)

@Serializable
data class FavoriteCheckData(val isFavorite: Boolean)

@Serializable
data class FavoriteToggleData(val isFavorite: Boolean, val message: String)

@Serializable
data class FavoriteMessageData(val message: String)

fun Route.favoriteRoutes(favoriteService: FavoriteService) {
    route("/api/favorites") {
        authenticate("auth-jwt") {
            // Get user's favorite dishes
            get {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))
                
                val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                
                favoriteService.getFavorites(principal.userId, page)
                    .onSuccess { result ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get favorites"))
                    }
            }
            
            // Get list of favorite dish IDs (for quick check)
            get("/ids") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))
                
                favoriteService.getFavoriteIds(principal.userId)
                    .onSuccess { ids ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(FavoriteIdsData(ids)))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get favorite ids"))
                    }
            }
            
            // Check if a dish is favorited
            get("/check/{dishId}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))
                
                val dishId = call.parameters["dishId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid dish id"))
                
                favoriteService.isFavorite(principal.userId, dishId)
                    .onSuccess { isFav ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(FavoriteCheckData(isFav)))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to check favorite"))
                    }
            }
            
            // Toggle favorite - use route() block to ensure priority
            route("/toggle/{dishId}") {
                post {
                    val principal = call.principal<JWTPrincipal>()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))
                    
                    val dishId = call.parameters["dishId"]?.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid dish id"))
                    
                    favoriteService.toggleFavorite(principal.userId, dishId)
                        .onSuccess { isFavNow ->
                            val message = if (isFavNow) "Đã thêm vào yêu thích" else "Đã bỏ yêu thích"
                            call.respond(HttpStatusCode.OK, ApiResponse.success(FavoriteToggleData(isFavNow, message)))
                        }
                        .onFailure { error ->
                            call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to toggle favorite"))
                        }
                }
            }
            
            // Add to favorites - use route() block
            route("/add/{dishId}") {
                post {
                    val principal = call.principal<JWTPrincipal>()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))
                    
                    val dishId = call.parameters["dishId"]?.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid dish id"))
                    
                    favoriteService.addFavorite(principal.userId, dishId)
                        .onSuccess { added ->
                            if (added) {
                                call.respond(HttpStatusCode.Created, ApiResponse.success(FavoriteMessageData("Đã thêm vào yêu thích")))
                            } else {
                                call.respond(HttpStatusCode.OK, ApiResponse.success(FavoriteMessageData("Món này đã có trong danh sách yêu thích")))
                            }
                        }
                        .onFailure { error ->
                            call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to add favorite"))
                        }
                }
            }
            
            // Remove from favorites - use route() block
            route("/remove/{dishId}") {
                delete {
                    val principal = call.principal<JWTPrincipal>()
                        ?: return@delete call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))
                    
                    val dishId = call.parameters["dishId"]?.toIntOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid dish id"))
                    
                    favoriteService.removeFavorite(principal.userId, dishId)
                        .onSuccess { removed ->
                            if (removed) {
                                call.respond(HttpStatusCode.OK, ApiResponse.success(FavoriteMessageData("Đã bỏ yêu thích")))
                            } else {
                                call.respond(HttpStatusCode.NotFound, ApiResponse.error<Unit>("Món này không có trong danh sách yêu thích"))
                            }
                        }
                        .onFailure { error ->
                            call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to remove favorite"))
                        }
                }
            }
        }
    }
}
