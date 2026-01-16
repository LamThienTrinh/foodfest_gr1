package com.foodfest.app.features.auth.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.components.FoodFestTextField
import com.foodfest.app.theme.AppColors
import foodfest.composeapp.generated.resources.Res
import foodfest.composeapp.generated.resources.foodfest_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun RegisterForm(
    fullName: String,
    username: String,
    password: String,
    confirmPassword: String,
    errorMessage: String?,
    isLoading: Boolean,
    onFullNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        
        Image(
            painter = painterResource(Res.drawable.foodfest_logo),
            contentDescription = "FoodFest Logo",
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Tạo tài khoản",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.Brown
        )
        
        Text(
            text = "Đăng ký để khám phá ẩm thực Việt Nam",
            fontSize = 14.sp,
            color = AppColors.GrayPlaceholder,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        FoodFestTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            placeholder = "Họ và tên"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FoodFestTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = "Xác nhận mật khẩu",
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
            onClick = onRegisterClick,
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
                    color = AppColors.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Đăng ký",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Đã có tài khoản? ",
                fontSize = 14.sp,
                color = AppColors.GrayPlaceholder
            )
            TextButton(onClick = onLoginClick) {
                Text(
                    text = "Đăng nhập",
                    fontSize = 14.sp,
                    color = AppColors.Orange,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
