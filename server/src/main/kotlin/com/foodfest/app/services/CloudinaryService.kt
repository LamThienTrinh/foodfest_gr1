package com.foodfest.app.services

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest

object CloudinaryService {
    // Hardcode để đảm bảo hoạt động
    private const val cloudName = "dpf6mzl4g"
    private const val apiKey = "627841298718141"
    private const val apiSecret = "Bt8dkkfVbP9CnHpkn2KrciCI2Hc"
    
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 120_000
        }
    }
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun uploadAvatar(base64Image: String, folder: String = "avatars"): String? {
        return try {
            println("=== Cloudinary Upload Start ===")
            println("Cloud name: $cloudName, Folder: $folder")
            
            // Clean base64 string - lấy phần sau dấu phẩy nếu có
            val cleanBase64 = if (base64Image.contains(",")) {
                base64Image.substringAfter(",")
            } else {
                base64Image
            }
            println("Base64 length: ${cleanBase64.length}")
            
            val timestamp = System.currentTimeMillis() / 1000
            
            // Signature: params theo thứ tự alphabetical + apiSecret
            val paramsToSign = "folder=$folder&timestamp=$timestamp"
            val signature = sha1Hex(paramsToSign + apiSecret)
            
            println("Timestamp: $timestamp")
            println("Signature: $signature")
            
            val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"
            
            // Sử dụng submitForm thay vì submitFormWithBinaryData
            val response: HttpResponse = client.submitForm(
                url = uploadUrl,
                formParameters = parameters {
                    append("file", "data:image/jpeg;base64,$cleanBase64")
                    append("api_key", apiKey)
                    append("timestamp", timestamp.toString())
                    append("signature", signature)
                    append("folder", folder)
                }
            )
            
            val responseBody = response.bodyAsText()
            println("Response: ${response.status} - ${responseBody.take(300)}")
            
            if (response.status.isSuccess()) {
                val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
                val secureUrl = jsonResponse["secure_url"]?.jsonPrimitive?.content
                println("SUCCESS! URL: $secureUrl")
                secureUrl
            } else {
                println("FAILED: $responseBody")
                null
            }
        } catch (e: Exception) {
            println("EXCEPTION: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    private fun sha1Hex(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
