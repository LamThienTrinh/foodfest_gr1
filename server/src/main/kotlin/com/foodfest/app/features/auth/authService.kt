package com.foodfest.app.features.auth

import com.foodfest.app.services.CloudinaryService
import com.foodfest.app.utils.JWTConfig
import kotlinx.serialization.Serializable
import org.mindrot.jbcrypt.BCrypt

// =============================================
// REQUEST/RESPONSE MODELS
// =============================================
@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val fullName: String,
    val avatarBase64: String? = null
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class UpdateProfileRequest(
    val fullName: String? = null
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

@Serializable
data class AuthResponse(
    val token: String,
    val user: User
)

// =============================================
// SERVICE (Business Logic)
// =============================================
class AuthService(
    private val authRepository: AuthRepository
) {
    
    // URL avatar mặc định (dùng placeholder từ UI Avatars)
    companion object {
        const val DEFAULT_AVATAR_URL = "https://ui-avatars.com/api/?background=CFD8DC&color=78909C&name=User&size=200"
    }
    
    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return try {
            // Validation
            if (request.username.isBlank() || request.password.isBlank() || request.fullName.isBlank()) {
                return Result.failure(IllegalArgumentException("All fields are required"))
            }
            
            if (request.username.length < 3 || request.username.length > 50) {
                return Result.failure(IllegalArgumentException("Username must be between 3-50 characters"))
            }
            
            if (request.password.length < 6) {
                return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
            }
            
            // Check if user exists
            if (authRepository.userExists(request.username)) {
                return Result.failure(IllegalArgumentException("Username already exists"))
            }
            
            // Sử dụng avatar mặc định dựa trên tên người dùng
            val avatarUrl = generateDefaultAvatarUrl(request.fullName)
            
            // Hash password
            val passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())
            
            // Create user
            val user = authRepository.createUser(
                username = request.username,
                passwordHash = passwordHash,
                fullName = request.fullName,
                avatarUrl = avatarUrl
            )
            
            // Generate JWT token
            val token = JWTConfig.generateToken(user.id, user.username)
            
            Result.success(AuthResponse(token = token, user = user))
        } catch (e: Exception) {
            println(" Registration error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private fun generateDefaultAvatarUrl(fullName: String): String {
        // Tạo avatar URL với initials từ tên người dùng
        val encodedName = java.net.URLEncoder.encode(fullName, "UTF-8")
        return "https://ui-avatars.com/api/?background=FF8C42&color=ffffff&name=$encodedName&size=200&bold=true"
    }
    
    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            // Validation
            if (request.username.isBlank() || request.password.isBlank()) {
                return Result.failure(IllegalArgumentException("Username and password are required"))
            }
            
            // Get user
            val user = authRepository.getUserByUsername(request.username)
                ?: return Result.failure(IllegalArgumentException("Invalid credentials"))
            
            // Verify password
            if (!BCrypt.checkpw(request.password, user.passwordHash)) {
                return Result.failure(IllegalArgumentException("Invalid credentials"))
            }
            
            // Generate JWT token
            val token = JWTConfig.generateToken(user.id, user.username)
            
            Result.success(AuthResponse(token = token, user = user))
        } catch (e: Exception) {
            println(" Login error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun getProfile(userId: Int): Result<User> {
        return try {
            val user = authRepository.getUserById(userId)
                ?: return Result.failure(IllegalArgumentException("User not found"))
            
            Result.success(user)
        } catch (e: Exception) {
            println(" Get profile error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateProfile(userId: Int, request: UpdateProfileRequest): Result<User> {
        return try {
            // Validation
            if (request.fullName.isNullOrBlank()) {
                return Result.failure(IllegalArgumentException("Full name is required"))
            }
            
            if (request.fullName.length < 2 || request.fullName.length > 100) {
                return Result.failure(IllegalArgumentException("Full name must be between 2-100 characters"))
            }
            
            // Update
            val updated = authRepository.updateFullName(userId, request.fullName)
            if (!updated) {
                return Result.failure(IllegalArgumentException("Failed to update profile"))
            }
            
            // Return updated user
            val user = authRepository.getUserById(userId)
                ?: return Result.failure(IllegalArgumentException("User not found"))
            
            Result.success(user)
        } catch (e: Exception) {
            println(" Update profile error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun changePassword(userId: Int, request: ChangePasswordRequest): Result<Unit> {
        return try {
            // Validation
            if (request.currentPassword.isBlank() || request.newPassword.isBlank()) {
                return Result.failure(IllegalArgumentException("Both passwords are required"))
            }
            
            if (request.newPassword.length < 6) {
                return Result.failure(IllegalArgumentException("New password must be at least 6 characters"))
            }
            
            // Get current user
            val user = authRepository.getUserById(userId)
                ?: return Result.failure(IllegalArgumentException("User not found"))
            
            // Verify current password
            if (!BCrypt.checkpw(request.currentPassword, user.passwordHash)) {
                return Result.failure(IllegalArgumentException("Current password is incorrect"))
            }
            
            // Hash new password and update
            val newPasswordHash = BCrypt.hashpw(request.newPassword, BCrypt.gensalt())
            val updated = authRepository.updatePassword(userId, newPasswordHash)
            
            if (!updated) {
                return Result.failure(IllegalArgumentException("Failed to change password"))
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            println(" Change password error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateAvatar(userId: Int, request: UpdateAvatarRequest): Result<User> {
        return try {
            // Validation
            if (request.avatarBase64.isBlank()) {
                return Result.failure(IllegalArgumentException("Avatar image is required"))
            }
            
            // Upload to Cloudinary
            val avatarUrl = CloudinaryService.uploadAvatar(request.avatarBase64, "avatars")
                ?: return Result.failure(IllegalArgumentException("Failed to upload avatar"))
            
            // Update database
            val updated = authRepository.updateAvatar(userId, avatarUrl)
            if (!updated) {
                return Result.failure(IllegalArgumentException("Failed to update avatar"))
            }
            
            // Return updated user
            val user = authRepository.getUserById(userId)
                ?: return Result.failure(IllegalArgumentException("User not found"))
            
            Result.success(user)
        } catch (e: Exception) {
            println(" Update avatar error: ${e.message}")
            Result.failure(e)
        }
    }
}

