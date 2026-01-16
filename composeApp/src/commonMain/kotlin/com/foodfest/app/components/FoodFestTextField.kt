package com.foodfest.app.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.foodfest.app.theme.AppColors

@Composable
fun FoodFestTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { 
            Text(
                text = placeholder, 
                color = AppColors.GrayPlaceholder,
                style = MaterialTheme.typography.bodyMedium
            ) 
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp), // Chiều cao chuẩn design
        
        // --- CHỈNH ĐỘ BO GÓC Ở ĐÂY ---
        // 12.dp là độ bo vừa phải, đẹp hơn 28.dp (hình viên thuốc)
        shape = RoundedCornerShape(12.dp), 
        
        colors = TextFieldDefaults.colors(
            focusedContainerColor = AppColors.White,
            unfocusedContainerColor = AppColors.White,
            focusedIndicatorColor = Color.Transparent, // Bỏ gạch chân
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = AppColors.Brown
        ),
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions
    )
}