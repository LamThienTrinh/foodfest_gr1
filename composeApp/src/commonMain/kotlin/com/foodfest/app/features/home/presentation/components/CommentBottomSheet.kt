package com.foodfest.app.features.home.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.features.home.data.Comment
import com.foodfest.app.features.home.presentation.models.CommentThreadState
import com.foodfest.app.theme.AppColors
import foodfest.composeapp.generated.resources.Res
import foodfest.composeapp.generated.resources.default_avatar
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    comments: List<Comment>,
    totalCommentCount: Int,
    threadStates: Map<Int, CommentThreadState>,
    inputText: String,
    replyingToUserName: String?,
    isLoading: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    onToggleThread: (Comment) -> Unit,
    onReplyClick: (Comment) -> Unit,
    onLoadMoreReplies: (Int) -> Unit,
    onInputTextChange: (String) -> Unit,
    onCancelReply: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val displayCount = totalCommentCount.coerceAtLeast(comments.size)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.White
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .height(560.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "$displayCount bình luận",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = AppColors.Background)
            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading && comments.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Orange)
                    }
                }

                errorMessage != null && comments.isEmpty() -> {
                    EmptyCommentsState(
                        title = "Không tải được bình luận",
                        description = errorMessage,
                        actionText = "Thử lại",
                        onActionClick = onRetryLoad,
                        modifier = Modifier.weight(1f)
                    )
                }

                comments.isEmpty() -> {
                    EmptyCommentsState(
                        title = "Chưa có bình luận",
                        description = "Hãy là người đầu tiên bình luận cho bài viết này.",
                        actionText = "Làm mới",
                        onActionClick = onRetryLoad,
                        modifier = Modifier.weight(1f)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            val threadState = threadStates[comment.id]
                                ?: CommentThreadState(hasMoreReplies = comment.replyCount > 0)

                            CommentThreadItem(
                                comment = comment,
                                threadState = threadState,
                                onToggleThread = { onToggleThread(comment) },
                                onReplyClick = { onReplyClick(comment) },
                                onLoadMoreReplies = { onLoadMoreReplies(comment.id) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = AppColors.Background)
            Spacer(modifier = Modifier.height(10.dp))

            if (!replyingToUserName.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Đang phản hồi @$replyingToUserName",
                        fontSize = 12.sp,
                        color = AppColors.Orange,
                        fontWeight = FontWeight.SemiBold
                    )

                    TextButton(onClick = onCancelReply) {
                        Text("Hủy")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            if (replyingToUserName.isNullOrBlank()) {
                                "Viết bình luận..."
                            } else {
                                "Viết phản hồi..."
                            }
                        )
                    },
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp)
                )

                Button(
                    onClick = onSubmit,
                    enabled = inputText.trim().isNotEmpty() && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = if (isSubmitting) "..." else "Gửi")
                }
            }
        }
    }
}

@Composable
private fun CommentThreadItem(
    comment: Comment,
    threadState: CommentThreadState,
    onToggleThread: () -> Unit,
    onReplyClick: (Comment) -> Unit,
    onLoadMoreReplies: () -> Unit
) {
    val hasReplies = comment.replyCount > 0

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CommentItem(
            comment = comment,
            onClick = if (hasReplies) onToggleThread else null
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = comment.createdAt,
                fontSize = 12.sp,
                color = AppColors.GrayPlaceholder
            )

            Text(
                text = "Trả lời",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextSecondary,
                modifier = Modifier.clickable { onReplyClick(comment) }
            )

            if (hasReplies) {
                Text(
                    text = if (threadState.expanded) {
                        "Ẩn ${comment.replyCount} phản hồi"
                    } else {
                        "${comment.replyCount} phản hồi"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Orange,
                    modifier = Modifier.clickable(onClick = onToggleThread)
                )
            } else {
                Text(
                    text = "0 phản hồi",
                    fontSize = 12.sp,
                    color = AppColors.GrayPlaceholder
                )
            }
        }

        if (threadState.expanded && hasReplies) {
            CommentRepliesSection(
                threadState = threadState,
                onRetry = onLoadMoreReplies,
                onLoadMoreReplies = onLoadMoreReplies
            )
        }
    }
}

@Composable
private fun CommentRepliesSection(
    threadState: CommentThreadState,
    onRetry: () -> Unit,
    onLoadMoreReplies: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 44.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (threadState.isLoadingReplies && threadState.replies.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = AppColors.Orange,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Đang tải phản hồi...",
                    fontSize = 12.sp,
                    color = AppColors.GrayPlaceholder
                )
            }
        }

        if (threadState.repliesErrorMessage != null && threadState.replies.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = threadState.repliesErrorMessage,
                    fontSize = 12.sp,
                    color = AppColors.GrayPlaceholder,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onRetry) {
                    Text("Thử lại")
                }
            }
        }

        threadState.replies.forEach { reply ->
            ReplyItem(reply = reply)
        }

        if (threadState.isLoadingReplies && threadState.replies.isNotEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(14.dp)
                    .padding(vertical = 2.dp),
                color = AppColors.Orange,
                strokeWidth = 2.dp
            )
        }

        if (threadState.hasMoreReplies && !threadState.isLoadingReplies) {
            TextButton(onClick = onLoadMoreReplies) {
                Text("Xem thêm phản hồi")
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(
            painter = painterResource(Res.drawable.default_avatar),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
        )

        Column(modifier = Modifier.weight(1f)) {
            Surface(
                modifier = if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
                shape = RoundedCornerShape(12.dp),
                color = AppColors.Background
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = comment.userName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = comment.content,
                        fontSize = 14.sp,
                        color = AppColors.TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplyItem(reply: Comment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(Res.drawable.default_avatar),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
        )

        Column(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AppColors.Background
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Text(
                        text = reply.userName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = reply.content,
                        fontSize = 13.sp,
                        color = AppColors.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = reply.createdAt,
                fontSize = 11.sp,
                color = AppColors.GrayPlaceholder
            )
        }
    }
}

@Composable
private fun EmptyCommentsState(
    title: String,
    description: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            fontSize = 13.sp,
            color = AppColors.GrayPlaceholder,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onActionClick,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(actionText)
        }
    }
}
