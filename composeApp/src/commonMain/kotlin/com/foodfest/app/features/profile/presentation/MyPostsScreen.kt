package com.foodfest.app.features.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.features.home.data.Post
import com.foodfest.app.features.home.data.PostRepository
import com.foodfest.app.theme.AppColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class MyPostsState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true
)

class MyPostsViewModel(
    private val postRepository: PostRepository = PostRepository()
) {
    var state by mutableStateOf(MyPostsState())
        private set

    private val scope = CoroutineScope(Dispatchers.Main)

    fun loadMyPosts(userId: Int, refresh: Boolean = false) {
        if (state.isLoading) return

        val page = if (refresh) 1 else state.currentPage

        state = state.copy(
            isLoading = true,
            errorMessage = if (refresh) null else state.errorMessage
        )

        scope.launch {
            postRepository.getUserPosts(userId = userId, page = page).fold(
                onSuccess = { response ->
                    val mergedPosts = if (refresh) {
                        response.data
                    } else {
                        state.posts + response.data
                    }

                    val hasMorePages = mergedPosts.size < response.total
                    state = state.copy(
                        posts = mergedPosts,
                        isLoading = false,
                        errorMessage = null,
                        currentPage = if (hasMorePages) page + 1 else page,
                        hasMorePages = hasMorePages
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Không thể tải lịch sử bài đăng"
                    )
                }
            )
        }
    }

    fun retry(userId: Int) {
        loadMyPosts(userId = userId, refresh = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostsScreen(
    userId: Int?,
    onBack: () -> Unit,
    viewModel: MyPostsViewModel = remember { MyPostsViewModel() }
) {
    val state = viewModel.state
    val listState = rememberLazyListState()

    LaunchedEffect(userId) {
        if (userId != null && userId > 0) {
            viewModel.loadMyPosts(userId = userId, refresh = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bài đăng của tôi",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Brown
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = AppColors.Brown
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        when {
            userId == null || userId <= 0 -> {
                StateMessage(
                    title = "Không xác định được người dùng",
                    description = "Vui lòng đăng nhập lại để xem lịch sử bài đăng.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            state.isLoading && state.posts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Orange)
                }
            }

            state.errorMessage != null && state.posts.isEmpty() -> {
                StateMessage(
                    title = "Không thể tải lịch sử bài đăng",
                    description = state.errorMessage,
                    actionLabel = "Thử lại",
                    onAction = { viewModel.retry(userId) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            state.posts.isEmpty() -> {
                StateMessage(
                    title = "Bạn chưa có bài đăng nào",
                    description = "Hãy tạo bài đăng đầu tiên để lưu lại công thức và trải nghiệm nấu ăn.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(AppColors.Background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "${state.posts.size} bài đăng",
                            fontSize = 14.sp,
                            color = AppColors.GrayPlaceholder
                        )
                    }

                    items(state.posts, key = { it.id }) { post ->
                        MyPostHistoryCard(post = post)
                    }

                    if (state.isLoading && state.posts.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(30.dp),
                                    color = AppColors.Orange
                                )
                            }
                        }
                    }
                }

                LaunchedEffect(listState, state.hasMorePages, state.isLoading, state.posts.size, userId) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { lastVisibleIndex ->
                            if (
                                lastVisibleIndex != null &&
                                lastVisibleIndex >= state.posts.size - 2 &&
                                !state.isLoading &&
                                state.hasMorePages
                            ) {
                                viewModel.loadMyPosts(userId = userId, refresh = false)
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun StateMessage(
    title: String,
    description: String?,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        )
        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                color = AppColors.GrayPlaceholder,
                fontSize = 14.sp
            )
        }
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun MyPostHistoryCard(post: Post) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = post.title?.takeIf { it.isNotBlank() } ?: "Bài đăng không tiêu đề",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!post.content.isNullOrBlank()) {
                Text(
                    text = post.content,
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "❤ ${post.likeCount}   💬 ${post.commentCount}",
                    fontSize = 13.sp,
                    color = AppColors.GrayPlaceholder
                )
                Text(
                    text = post.createdAt,
                    fontSize = 12.sp,
                    color = AppColors.GrayPlaceholder,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}
