package com.foodfest.app.features.auth.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.core.storage.TokenManager
import com.foodfest.app.features.auth.data.AuthRepository
import com.foodfest.app.features.auth.data.User
import com.foodfest.app.features.auth.presentation.models.AuthState

class LoginViewModel {
    private val authRepository = AuthRepository()
    
    var state by mutableStateOf(AuthState())
        private set
    
    fun updateUsername(value: String) {
        state = state.copy(username = value, errorMessage = null)
    }
    
    fun updatePassword(value: String) {
        state = state.copy(password = value, errorMessage = null)
    }
    
    fun setShowLoginForm(show: Boolean) {
        state = state.copy(showLoginForm = show)
    }
    
    suspend fun login(onSuccess: (String, User) -> Unit) {
        if (state.username.isBlank() || state.password.isBlank()) {
            state = state.copy(errorMessage = "Vui lòng nhập tên đăng nhập và mật khẩu")
            return
        }
        
        state = state.copy(isLoading = true, errorMessage = null)
        
        val result = authRepository.login(state.username, state.password)
        
        result.fold(
            onSuccess = { authResponse ->
                TokenManager.saveToken(authResponse.token)
                TokenManager.saveUserInfo(
                    userId = authResponse.user.id,
                    username = authResponse.user.username
                )
                println("✅ Đăng nhập thành công! Token đã được lưu")
                state = state.copy(isLoading = false)
                onSuccess(authResponse.token, authResponse.user)
            },
            onFailure = { e ->
                state = state.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        )
    }
}

class RegisterViewModel {
    private val authRepository = AuthRepository()
    
    var state by mutableStateOf(AuthState())
        private set
    
    fun updateUsername(value: String) {
        state = state.copy(username = value, errorMessage = null)
    }
    
    fun updatePassword(value: String) {
        state = state.copy(password = value, errorMessage = null)
    }
    
    fun updateConfirmPassword(value: String) {
        state = state.copy(confirmPassword = value, errorMessage = null)
    }
    
    fun updateFullName(value: String) {
        state = state.copy(fullName = value, errorMessage = null)
    }
    
    suspend fun register(onSuccess: (String, User) -> Unit) {
        // Validation
        when {
            state.fullName.isBlank() -> {
                state = state.copy(errorMessage = "Vui lòng nhập họ và tên")
                return
            }
            state.username.isBlank() -> {
                state = state.copy(errorMessage = "Vui lòng nhập tên đăng nhập")
                return
            }
            state.username.length < 3 -> {
                state = state.copy(errorMessage = "Tên đăng nhập phải có ít nhất 3 ký tự")
                return
            }
            state.password.isBlank() -> {
                state = state.copy(errorMessage = "Vui lòng nhập mật khẩu")
                return
            }
            state.password.length < 6 -> {
                state = state.copy(errorMessage = "Mật khẩu phải có ít nhất 6 ký tự")
                return
            }
            state.password != state.confirmPassword -> {
                state = state.copy(errorMessage = "Mật khẩu xác nhận không khớp")
                return
            }
        }
        
        state = state.copy(isLoading = true, errorMessage = null)
        
        val result = authRepository.register(
            username = state.username,
            password = state.password,
            fullName = state.fullName
        )
        
        result.fold(
            onSuccess = { authResponse ->
                TokenManager.saveToken(authResponse.token)
                TokenManager.saveUserInfo(
                    userId = authResponse.user.id,
                    username = authResponse.user.username
                )
                println("✅ Đăng ký thành công! Token đã được lưu")
                state = state.copy(isLoading = false)
                onSuccess(authResponse.token, authResponse.user)
            },
            onFailure = { e ->
                state = state.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Đăng ký thất bại"
                )
            }
        )
    }
}
