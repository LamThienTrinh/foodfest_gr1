package com.foodfest.app.features.upload

import com.foodfest.app.core.response.ApiResponse
import com.foodfest.app.services.CloudinaryService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class UploadImageRequest(
    val imageData: String, // Base64 encoded
    val fileName: String
)

@Serializable
data class UploadImageResponse(
    val imageUrl: String
)

fun Route.uploadRoutes() {
    route("/api/upload") {
        authenticate("auth-jwt") {
            // Upload ảnh lên Cloudinary
            post("/image") {
                val request = runCatching { call.receive<UploadImageRequest>() }.getOrNull()
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.error<Unit>("Invalid request body")
                    )
                
                if (request.imageData.isBlank()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.error<Unit>("Image data is required")
                    )
                }
                
                // Upload lên Cloudinary với folder "posts"
                val imageUrl = CloudinaryService.uploadAvatar(
                    base64Image = request.imageData,
                    folder = "posts"
                )
                
                if (imageUrl != null) {
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse.success(
                            UploadImageResponse(imageUrl = imageUrl),
                            "Upload successful"
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<Unit>("Failed to upload image to Cloudinary")
                    )
                }
            }
        }
    }
}
