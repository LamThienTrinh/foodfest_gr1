package com.foodfest.app.features.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foodfest.app.features.auth.data.User
import com.foodfest.app.features.auth.presentation.components.AnimatedLogo
import com.foodfest.app.features.auth.presentation.components.LoginForm
import com.foodfest.app.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onLoginSuccess: (String, User) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val viewModel = remember { LoginViewModel() }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        delay(2000)
        viewModel.setShowLoginForm(true)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(if (viewModel.state.showLoginForm) 0.15f else 0.5f))
            
            AnimatedLogo(
                showLoginForm = viewModel.state.showLoginForm,
                showLoading = !viewModel.state.showLoginForm
            )
            
            LoginForm(
                username = viewModel.state.username,
                password = viewModel.state.password,
                errorMessage = viewModel.state.errorMessage,
                isLoading = viewModel.state.isLoading,
                showForm = viewModel.state.showLoginForm,
                onUsernameChange = { viewModel.updateUsername(it) },
                onPasswordChange = { viewModel.updatePassword(it) },
                onLoginClick = {
                    scope.launch {
                        viewModel.login(onLoginSuccess)
                    }
                },
                onRegisterClick = onNavigateToRegister
            )
            
            Spacer(modifier = Modifier.weight(if (viewModel.state.showLoginForm) 0.6f else 0.5f))
        }
    }
}
