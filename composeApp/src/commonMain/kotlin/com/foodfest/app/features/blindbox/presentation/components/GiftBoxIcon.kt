package com.foodfest.app.features.blindbox.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Màu sắc hộp quà
private val BoxColor = Color(0xFFE91E63) // Hồng đậm
private val RibbonColor = Color(0xFFFFC107) // Vàng

@Composable
fun GiftBoxBody(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 150.dp, height = 120.dp) // Thân thấp hơn tổng thể
            .background(BoxColor, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
    ) {
        // Dây ruy băng dọc thân
        Box(
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight()
                .align(Alignment.Center)
                .background(RibbonColor)
        )
    }
}

@Composable
fun GiftBoxLid(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 160.dp, height = 40.dp) // Nắp rộng hơn thân một chút
            .background(BoxColor, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Dây ruy băng ngang trên nắp
        Box(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
                .background(RibbonColor)
        )
        // Cái nơ
        Text(
            "🎀",
            fontSize = 60.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .offset(y = (-35).dp) // Đẩy nơ lên trên nắp
        )
    }
}
