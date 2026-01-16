package com.foodfest.app.features.savedposts.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.features.savedposts.presentation.components.*
import com.foodfest.app.theme.AppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPostsScreen(
    viewModel: SavedPostsViewModel = remember { SavedPostsViewModel() },
    onBack: () -> Unit,
    // onNavigateToComments: (Int) -> Unit = {},
    onNavigateToUserProfile: (Int) -> Unit = {},
    onBrowsePosts: () -> Unit = {}
) {
    val state = viewModel.state
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // Load saved posts on first composition
    LaunchedEffect(Unit) {
        viewModel.loadSavedPosts(refresh = true)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bài đăng đã lưu",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Orange,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        when {
            // Loading state
            state.isLoading && state.posts.isEmpty() -> {
                SavedPostsLoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(AppColors.Background)
                )
            }
            
            // Error state
            state.errorMessage != null && state.posts.isEmpty() -> {
                SavedPostsErrorState(
                    message = state.errorMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(AppColors.Background),
                    onRetry = {
                        scope.launch {
                            viewModel.loadSavedPosts(refresh = true)
                        }
                    }
                )
            }
            
            // Empty state
            state.posts.isEmpty() && !state.isLoading -> {
                SavedPostsEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(AppColors.Background),
                    onBrowsePosts = onBrowsePosts
                )
            }
            
            // Content
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
                    // Header with count
                    item {
                        Text(
                            text = "${state.posts.size} bài đăng đã lưu",
                            fontSize = 14.sp,
                            color = AppColors.GrayPlaceholder,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    // Saved posts list
                    items(state.posts, key = { it.id }) { post ->
                        SavedPostCard(
                            post = post,
                            onLikeClick = {
                                scope.launch {
                                    viewModel.likePost(post.id)
                                }
                            },
                            // onCommentClick = {
                            //     onNavigateToComments(post.id)
                            // },
                            onUnsaveClick = {
                                scope.launch {
                                    viewModel.unsavePost(post.id)
                                }
                            },
                            onUserClick = {
                                onNavigateToUserProfile(post.userId)
                            }
                        )
                    }
                    
                    // Loading more indicator
                    if (state.isLoading && state.posts.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = AppColors.Orange
                                )
                            }
                        }
                    }
                }
                
                // Load more when reaching end
                LaunchedEffect(listState) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { lastVisibleIndex ->
                            if (lastVisibleIndex != null &&
                                lastVisibleIndex >= state.posts.size - 2 &&
                                !state.isLoading &&
                                state.hasMorePages
                            ) {
                                viewModel.loadSavedPosts()
                            }
                        }
                }
            }
        }
    }
}
