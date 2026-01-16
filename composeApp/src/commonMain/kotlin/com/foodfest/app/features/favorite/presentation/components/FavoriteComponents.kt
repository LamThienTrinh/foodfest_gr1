package com.foodfest.app.features.favorite.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.foodfest.app.features.favorite.data.FavoriteDish
import com.foodfest.app.theme.AppColors

@Composable
fun FavoriteDishCard(
    favoriteDish: FavoriteDish,
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
            // Image placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFFFF2EA)),
                contentAlignment = Alignment.Center
            ) {
                if (favoriteDish.imageUrl != null) {
                    AppImage(
                        url = favoriteDish.imageUrl,
                        contentDescription = favoriteDish.dishName,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "🍽️",
                        fontSize = 48.sp
                    )
                }
                
                // Favorite indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(50))
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Yêu thích",
                        modifier = Modifier.size(18.dp),
                        tint = Color.Red
                    )
                }
            }
            
            // Name
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = favoriteDish.dishName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FavoritesLoadingState(
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
                text = "Đang tải danh sách yêu thích...",
                fontSize = 14.sp,
                color = AppColors.GrayPlaceholder
            )
        }
    }
}

@Composable
fun FavoritesEmptyState(
    modifier: Modifier = Modifier,
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
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = AppColors.GrayPlaceholder
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Chưa có món yêu thích",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Brown
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Nhấn vào biểu tượng ❤️ trên món ăn để thêm vào danh sách yêu thích",
                fontSize = 14.sp,
                color = AppColors.GrayPlaceholder,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onExplore,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Orange
                )
            ) {
                Text("Khám phá món ăn")
            }
        }
    }
}

@Composable
fun FavoritesErrorState(
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
fun FavoritesCountHeader(
    count: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "$count món yêu thích",
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = AppColors.GrayPlaceholder,
        fontSize = 18.sp
    )
}
