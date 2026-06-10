package com.foodfest.app.features.personaldish.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.components.AppImage
import com.foodfest.app.features.personaldish.data.PersonalDish
import com.foodfest.app.theme.AppColors

@Composable
fun PersonalDishCard(
    dish: PersonalDish,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.75f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFFFF2EA)),
                contentAlignment = Alignment.Center
            ) {
                if (dish.imageUrl != null) {
                    AppImage(
                        url = dish.imageUrl,
                        contentDescription = dish.dishName,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "🍳",
                        fontSize = 48.sp
                    )
                }
                
                // Badge if has note
                if (!dish.note.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(AppColors.Orange, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Có ghi chú",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }
                }
            }
            
            // Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = dish.dishName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Time info
                if (dish.prepTime != null || dish.cookTime != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = AppColors.GrayPlaceholder
                        )
                        Spacer(Modifier.width(4.dp))
                        val totalTime = (dish.prepTime ?: 0) + (dish.cookTime ?: 0)
                        Text(
                            text = "$totalTime phút",
                            fontSize = 12.sp,
                            color = AppColors.GrayPlaceholder
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyDishesLoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = AppColors.Orange
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Đang tải món ăn...",
                fontSize = 14.sp,
                color = AppColors.GrayPlaceholder
            )
        }
    }
}

@Composable
fun MyDishesEmptyState(
    modifier: Modifier = Modifier,
    onCreateDish: () -> Unit = {},
    onExplore: () -> Unit = {}
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = AppColors.GrayPlaceholder
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Chưa có món nào được lưu",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Brown
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tạo món riêng của bạn để Blind Box có thêm lựa chọn cá nhân.",
                fontSize = 14.sp,
                color = AppColors.GrayPlaceholder,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onCreateDish,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Orange
                )
            ) {
                Text("Tạo món mới")
            }

            TextButton(onClick = onExplore) {
                Text(
                    text = "Khám phá món ăn",
                    color = AppColors.Brown
                )
            }
        }
    }
}

@Composable
fun MyDishesErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "😕",
                fontSize = 64.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Có lỗi xảy ra",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                fontSize = 14.sp,
                color = AppColors.GrayPlaceholder,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Orange
                )
            ) {
                Text("Thử lại")
            }
        }
    }
}

@Composable
fun DishesCountHeader(
    count: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "$count món đã lưu",
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = AppColors.GrayPlaceholder,
        fontSize = 18.sp
    )
}
