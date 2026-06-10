package com.foodfest.app.features.upload.data

import com.foodfest.app.core.network.NetworkClient
import com.foodfest.app.core.storage.TokenManager
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
private data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

@Serializable
private data class UploadImageRequest(
    val imageData: String,
    val fileName: String,
    val folder: String? = null
)

@Serializable
private data class UploadImageResponse(
    val imageUrl: String
)

/**
 * Shared image upload client for screens that need a Cloudinary URL before saving data.
 */
class ImageUploadRepository {
    private val client = NetworkClient.httpClient
    private val baseUrl = NetworkClient.BASE_URL

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun uploadImage(
        imageBytes: ByteArray,
        fileName: String,
        folder: String = "posts"
    ): Result<String> = runCatching {
        val response = client.post("$baseUrl/api/upload/image") {
            contentType(ContentType.Application.Json)
            TokenManager.getToken()?.let { token ->
                header("Authorization", "Bearer $token")
            }
            setBody(
                UploadImageRequest(
                    imageData = Base64.encode(imageBytes),
                    fileName = fileName,
                    folder = folder
                )
            )
        }

        val apiResponse = response.body<ApiResponse<UploadImageResponse>>()
        if (apiResponse.success && apiResponse.data != null) {
            apiResponse.data.imageUrl
        } else {
            throw IllegalStateException(apiResponse.message ?: "Upload ảnh thất bại")
        }
    }
}
