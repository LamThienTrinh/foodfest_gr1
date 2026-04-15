package com.foodfest.app.features.home.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.features.home.data.Post
import com.foodfest.app.theme.AppColors
import com.foodfest.app.components.AppImage
import androidx.compose.ui.layout.ContentScale
import foodfest.composeapp.generated.resources.Res
import foodfest.composeapp.generated.resources.default_avatar
import org.jetbrains.compose.resources.painterResource

@Composable
fun PostCard(
    post: Post,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onSaveClick: () -> Unit,
    onUserClick: () -> Unit = {},
    showFollowButton: Boolean = false,
    isFollowingAuthor: Boolean = false,
    isFollowLoading: Boolean = false,
    onFollowClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // User info row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppColors.Orange.copy(alpha = 0.2f))
                        .clickable(onClick = onUserClick),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.default_avatar),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onUserClick)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = post.userName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary
                        )
                        // Post type badge
                        PostTypeBadge(postType = post.postType)
                    }
                    Text(
                        text = formatTimeAgo(post.createdAt),
                        fontSize = 12.sp,
                        color = AppColors.GrayPlaceholder
                    )
                }

                if (showFollowButton) {
                    TextButton(
                        onClick = onFollowClick,
                        enabled = !isFollowLoading
                    ) {
                        if (isFollowLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = AppColors.Orange,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isFollowingAuthor) "Đang theo dõi" else "Theo dõi",
                                color = if (isFollowingAuthor) AppColors.GrayPlaceholder else AppColors.Orange,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                // Save button
                IconButton(onClick = onSaveClick) {
                    Icon(
                        imageVector = if (post.isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Lưu",
                        tint = if (post.isSaved) AppColors.Orange else AppColors.GrayPlaceholder
                    )
                }
            }
            
            // Title (if any)
            post.title?.let { title ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
            }
            
            // Content (if any)
            post.content?.let { content ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content,
                    fontSize = 14.sp,
                    color = AppColors.TextPrimary,
                    lineHeight = 20.sp
                )
            }
            
            // Image (if any)
            post.imageUrl?.let { imageUrl ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = AppColors.Background
                ) {
                    AppImage(
                        url = imageUrl,
                        contentDescription = post.title ?: "Post image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Divider
            HorizontalDivider(color = AppColors.Background)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like button
                Row(
                    modifier = Modifier
                        .clickable(onClick = onLikeClick)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Yêu thích",
                        tint = if (post.isLiked) AppColors.Orange else AppColors.GrayPlaceholder,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatCount(post.likeCount) + " Yêu thích",
                        fontSize = 13.sp,
                        color = if (post.isLiked) AppColors.Orange else AppColors.GrayPlaceholder
                    )
                }
                
                // Comment button
                Row(
                    modifier = Modifier
                        .clickable(onClick = onCommentClick)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubbleOutline,
                        contentDescription = "Bình luận",
                        tint = AppColors.GrayPlaceholder,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.commentCount} Bình luận",
                        fontSize = 13.sp,
                        color = AppColors.GrayPlaceholder
                    )
                }
             }
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> {
            val value = count / 1000000.0
            // Mẹo làm tròn 1 số thập phân trong Common Main:
            // Nhân 10 -> chuyển về Int (bỏ phần dư) -> chia lại cho 10.0
            val rounded = (value * 10).toInt() / 10.0
            "${rounded}M"
        }
        count >= 1000 -> {
            val value = count / 1000.0
            val rounded = (value * 10).toInt() / 10.0
            "${rounded}K"
        }
        else -> count.toString()
    }
}

private fun formatTimeAgo(createdAt: String): String {
    // TODO: Parse ISO date and calculate time difference
    // For now, just return the raw string or a placeholder
    return try {
        // Simple parsing - in real app use kotlinx-datetime
        if (createdAt.contains("T")) {
            val parts = createdAt.split("T")
            val date = parts[0]
            val time = parts[1].split(".")[0]
            "$date $time"
        } else {
            createdAt
        }
    } catch (e: Exception) {
        createdAt
    }
}

@Composable
private fun PostTypeBadge(postType: String) {
    val (label, emoji, color) = when (postType.lowercase()) {
        "recipe" -> Triple("Công thức", "🍳", Color(0xFF4CAF50))
        "review" -> Triple("Review", "⭐", Color(0xFFFFC107))
        "share" -> Triple("Chia sẻ", "📝", Color(0xFF2196F3))
        else -> Triple(postType, "📌", AppColors.GrayPlaceholder)
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = "$emoji $label",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
