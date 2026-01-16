package com.foodfest.app.features.auth.presentation.models

data class AuthState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showLoginForm: Boolean = false
)
