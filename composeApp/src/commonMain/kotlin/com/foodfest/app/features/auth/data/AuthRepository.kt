package com.foodfest.app.features.auth.data

import com.foodfest.app.core.network.NetworkClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

// =============================================
// REQUEST MODELS
// =============================================
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val fullName: String
)

@Serializable
data class UpdateProfileRequest(
    val fullName: String
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class UpdateAvatarRequest(
    val avatarBase64: String
)

// =============================================
@Serializable
data class User(
    val id: Int,
    val username: String,
    val fullName: String,
    val avatarUrl: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val createdAt: String
)

@Serializable
data class AuthResponse(
    val token: String,  // Backend trả token trước
    val user: User      
)

// Response wrapper - khớp với ApiResponse của Backend
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,  
    val data: T? = null
)

// =============================================
// REPOSITORY
// =============================================
class AuthRepository {
    
    private val client = NetworkClient.httpClient
    private val baseUrl = NetworkClient.BASE_URL
    
    suspend fun login(username: String, password: String): Result<AuthResponse> {
        return try {
            val response = client.post("$baseUrl/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username, password))
            }
            
            val result = response.body<ApiResponse<AuthResponse>>()
            
            if (result.success && result.data != null) {
                Result.success(result.data)
            } else {
                Result.failure(Exception(result.message ?: "Đăng nhập thất bại"))
            }
        } catch (e: Exception) {
            println(" Login error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun register(
        username: String,
        password: String,
        fullName: String
    ): Result<AuthResponse> {
        return try {
            val response = client.post("$baseUrl/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(username, password, fullName))
            }
            
            val result = response.body<ApiResponse<AuthResponse>>()
            
            if (result.success && result.data != null) {
                Result.success(result.data)
            } else {
                Result.failure(Exception(result.message ?: "Đăng ký thất bại"))
            }
        } catch (e: Exception) {
            println(" Register error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getProfile(token: String): Result<User> {
        return try {
            val response = client.get("$baseUrl/api/auth/me") {
                header("Authorization", "Bearer $token")
            }
            
            val result = response.body<ApiResponse<User>>()
            
            if (result.success && result.data != null) {
                Result.success(result.data)
            } else {
                Result.failure(Exception(result.message ?: "Không thể lấy thông tin"))
            }
        } catch (e: Exception) {
            println(" GetProfile error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateProfile(token: String, fullName: String): Result<User> {
        return try {
            val response = client.put("$baseUrl/api/auth/profile") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(UpdateProfileRequest(fullName))
            }
            
            val result = response.body<ApiResponse<User>>()
            
            if (result.success && result.data != null) {
                Result.success(result.data)
            } else {
                Result.failure(Exception(result.message ?: "Cập nhật thất bại"))
            }
        } catch (e: Exception) {
            println(" UpdateProfile error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun changePassword(token: String, currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val response = client.put("$baseUrl/api/auth/password") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(ChangePasswordRequest(currentPassword, newPassword))
            }
            
            val result = response.body<ApiResponse<Map<String, String>>>()
            
            if (result.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(result.message ?: "Đổi mật khẩu thất bại"))
            }
        } catch (e: Exception) {
            println(" ChangePassword error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateAvatar(token: String, avatarBase64: String): Result<User> {
        return try {
            val response = client.put("$baseUrl/api/auth/avatar") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(UpdateAvatarRequest(avatarBase64))
            }
            
            val result = response.body<ApiResponse<User>>()
            
            if (result.success && result.data != null) {
                Result.success(result.data)
            } else {
                Result.failure(Exception(result.message ?: "Cập nhật avatar thất bại"))
            }
        } catch (e: Exception) {
            println(" UpdateAvatar error: ${e.message}")
            Result.failure(e)
        }
    }
}
