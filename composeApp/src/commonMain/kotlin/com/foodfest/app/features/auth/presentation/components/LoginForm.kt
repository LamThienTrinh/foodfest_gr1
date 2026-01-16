package com.foodfest.app.features.auth.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.components.FoodFestTextField
import com.foodfest.app.theme.AppColors

@Composable
fun LoginForm(
    username: String,
    password: String,
    errorMessage: String?,
    isLoading: Boolean,
    showForm: Boolean,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val formAlpha by animateFloatAsState(
        targetValue = if (showForm) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 300),
        label = "formAlpha"
    )
    
    if (!showForm) return
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(formAlpha),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FoodFestTextField(
            value = username,
            onValueChange = onUsernameChange,
            placeholder = "Tên đăng nhập"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FoodFestTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "Mật khẩu",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 12.sp
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brown),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = AppColors.White
                )
            } else {
                Text(
                    "Đăng nhập",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onRegisterClick) {
            Text(
                "Đăng ký",
                fontSize = 14.sp,
                color = AppColors.Orange,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.None
            )
        }
    }
}
