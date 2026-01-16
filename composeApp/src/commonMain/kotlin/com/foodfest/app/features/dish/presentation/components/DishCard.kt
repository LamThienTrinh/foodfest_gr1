package com.foodfest.app.features.dish.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.components.AppImage
import com.foodfest.app.features.dish.data.Dish
import com.foodfest.app.theme.AppColors

/**
 * Card hiển thị món ăn trong grid list
 */
@Composable
fun DishCard(
    dish: Dish,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Tính tổng thời gian nấu
    val totalTime = (dish.prepTime ?: 0) + (dish.cookTime ?: 0)
    val timeText = when {
        totalTime > 0 -> "$totalTime phút"
        else -> "Chưa cập nhật"
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column {
            // Dish Image
            DishImagePlaceholder(
                dishName = dish.name,
                imageUrl = dish.imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )
            
            // Dish Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = dish.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                    color = AppColors.TextPrimary
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Time and serving info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Time",
                            tint = AppColors.GrayPlaceholder,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = timeText,
                            fontSize = 13.sp,
                            color = AppColors.GrayPlaceholder
                        )
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    // Serving
                    if (dish.serving != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Serving",
                                tint = AppColors.GrayPlaceholder,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${dish.serving} người",
                                fontSize = 13.sp,
                                color = AppColors.GrayPlaceholder
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Hiển thị ảnh món ăn hoặc placeholder nếu chưa có ảnh
 */
@Composable
fun DishImagePlaceholder(
    dishName: String,
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFF0F0F0)),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            // Hiển thị ảnh thực từ URL
            AppImage(
                url = imageUrl,
                contentDescription = dishName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder khi chưa có ảnh
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = dishName,
                    tint = AppColors.GrayPlaceholder.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Chưa có ảnh",
                    fontSize = 12.sp,
                    color = AppColors.GrayPlaceholder
                )
            }
        }
    }
}
