package com.foodfest.app.features.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.features.auth.data.User
import com.foodfest.app.theme.AppColors
import com.foodfest.app.core.storage.TokenManager
import foodfest.composeapp.generated.resources.Res
import foodfest.composeapp.generated.resources.default_avatar
import org.jetbrains.compose.resources.painterResource

@Composable
fun ProfileScreen(
    user: User?,
    onLogout: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToMyDishes: () -> Unit = {},
    onNavigateToDishUpload: () -> Unit = {},
    onNavigateToSavedPosts: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .verticalScroll(scrollState)
    ) {
        // Header with avatar
        ProfileHeader(
            username = user?.fullName ?: "Người dùng",
            email = user?.username ?: "",
            avatarUrl = user?.avatarUrl,
            onEditClick = onNavigateToEditProfile
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Menu items
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // Favorites
            ProfileMenuItem(
                icon = Icons.Default.Favorite,
                title = "Yêu thích",
                subtitle = "Món ăn yêu thích",
                onClick = onNavigateToFavorites
            )

            Spacer(modifier = Modifier.height(12.dp))

            // My Dishes - Personal saved dishes
            ProfileMenuItem(
                icon = Icons.Default.Restaurant,
                title = "Món đã lưu",
                subtitle = "Công thức đã lưu của bạn",
                onClick = onNavigateToMyDishes
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Saved Posts
            ProfileMenuItem(
                icon = Icons.Default.Bookmark,
                title = "Bài đăng đã lưu",
                subtitle = "Các bài đăng bạn đã lưu",
                onClick = onNavigateToSavedPosts
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileMenuItem(
                icon = Icons.Default.Restaurant,
                title = "Upload ảnh món",
                subtitle = "Chọn món và tải ảnh lên",
                onClick = onNavigateToDishUpload
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Logout button 
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout, // Dùng AutoMirrored
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Đăng xuất",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileHeader(
    username: String,
    email: String,
    avatarUrl: String?,
    onEditClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppColors.Orange,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(top = 48.dp, bottom = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar - hiển thị từ URL hoặc default
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    // TODO: Thêm Coil/Kamel để load ảnh từ URL
                    // Tạm thời dùng default avatar
                    Image(
                        painter = painterResource(Res.drawable.default_avatar),
                        contentDescription = "Avatar",
                        modifier = Modifier.size(100.dp).clip(CircleShape)
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.default_avatar),
                        contentDescription = "Avatar",
                        modifier = Modifier.size(100.dp).clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Username
            Text(
                text = username,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Email
            Text(
                text = email,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Edit profile button
            OutlinedButton(
                onClick = onEditClick,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                // Fix lỗi Deprecated: Tự tạo BorderStroke mới, clean hơn nhiều
                border = BorderStroke(1.dp, Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Chỉnh sửa",
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Orange.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.Orange,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = AppColors.GrayPlaceholder
                )
            }

            // Arrow icon (Đã fix dùng AutoMirrored)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AppColors.GrayPlaceholder,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}