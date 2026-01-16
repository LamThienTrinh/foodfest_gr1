package com.foodfest.app.features.savedposts.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.features.home.data.Post
import com.foodfest.app.theme.AppColors
import foodfest.composeapp.generated.resources.Res
import foodfest.composeapp.generated.resources.default_avatar
import org.jetbrains.compose.resources.painterResource

@Composable
fun SavedPostCard(
    post: Post,
    onLikeClick: () -> Unit,
    // onCommentClick: () -> Unit,
    onUnsaveClick: () -> Unit,
    onUserClick: () -> Unit = {},
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
                    Text(
                        text = post.userName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = formatTimeAgo(post.createdAt),
                        fontSize = 12.sp,
                        color = AppColors.GrayPlaceholder
                    )
                }
                
                // Unsave button (always filled since it's saved)
                IconButton(onClick = onUnsaveClick) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = "Bỏ lưu",
                        tint = AppColors.Orange
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Content
            post.content?.let { content ->
                Text(
                    text = content,
                    fontSize = 14.sp,
                    color = AppColors.TextPrimary,
                    lineHeight = 20.sp
                )
            }
            
            // Image (if any)
            post.imageUrl?.let { _ ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = AppColors.Background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🖼️",
                            fontSize = 48.sp
                        )
                    }
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
                // Row(
                //     modifier = Modifier
                //         .clickable(onClick = onCommentClick)
                //         .padding(horizontal = 16.dp, vertical = 8.dp),
                //     verticalAlignment = Alignment.CenterVertically
                // ) {
                //     Icon(
                //         imageVector = Icons.Filled.ChatBubbleOutline,
                //         contentDescription = "Bình luận",
                //         tint = AppColors.GrayPlaceholder,
                //         modifier = Modifier.size(20.dp)
                //     )
                //     Spacer(modifier = Modifier.width(6.dp))
                //     Text(
                //         text = "${post.commentCount} Bình luận",
                //         fontSize = 13.sp,
                //         color = AppColors.GrayPlaceholder
                //     )
                // }
            }
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> String.format("%.1fM", count / 1000000.0)
        count >= 1000 -> String.format("%.1fk", count / 1000.0)
        else -> count.toString()
    }
}

private fun formatTimeAgo(createdAt: String): String {
    // If already formatted, return as is
    if (createdAt.contains("trước") || createdAt.contains("ago")) {
        return createdAt
    }
    // TODO: Parse ISO date and calculate time ago
    return createdAt
}
