package com.foodfest.app.features.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foodfest.app.features.auth.data.User
import com.foodfest.app.features.auth.presentation.components.RegisterForm
import com.foodfest.app.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegisterSuccess: (String, User) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val viewModel = remember { RegisterViewModel() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RegisterForm(
                fullName = viewModel.state.fullName,
                username = viewModel.state.username,
                password = viewModel.state.password,
                confirmPassword = viewModel.state.confirmPassword,
                errorMessage = viewModel.state.errorMessage,
                isLoading = viewModel.state.isLoading,
                onFullNameChange = { viewModel.updateFullName(it) },
                onUsernameChange = { viewModel.updateUsername(it) },
                onPasswordChange = { viewModel.updatePassword(it) },
                onConfirmPasswordChange = { viewModel.updateConfirmPassword(it) },
                onRegisterClick = {
                    scope.launch {
                        viewModel.register(onRegisterSuccess)
                    }
                },
                onLoginClick = onNavigateToLogin
            )
        }
    }
}
