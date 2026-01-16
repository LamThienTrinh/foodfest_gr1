package com.foodfest.app.features.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.features.home.presentation.components.*
import com.foodfest.app.theme.AppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = remember { HomeViewModel() },
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCreatePost: () -> Unit = {},
    // onNavigateToComments: (Int) -> Unit = {},
    onNavigateToUserProfile: (Int) -> Unit = {}
) {
    val state = viewModel.state
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // Load posts on first composition
    LaunchedEffect(Unit) {
        viewModel.loadPosts(refresh = true)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FoodFest",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Brown
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        when {
            // Loading state
            state.isLoading && state.posts.isEmpty() -> {
                LoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            
            // Error state
            state.errorMessage != null && state.posts.isEmpty() -> {
                ErrorState(
                    message = state.errorMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    onRetry = {
                        scope.launch {
                            viewModel.loadPosts(refresh = true)
                        }
                    }
                )
            }
            
            // Content
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search bar
                    item {
                        PostSearchBar(
                            query = state.searchQuery,
                            onQueryChange = { query ->
                                viewModel.updateSearchQuery(query)
                            },
                            onSearch = {
                                scope.launch {
                                    viewModel.searchPosts()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                    
                    // Post type filter
                    item {
                        PostTypeFilter(
                            selectedType = state.selectedPostType,
                            onTypeSelected = { postType ->
                                viewModel.updatePostTypeFilter(postType)
                                scope.launch {
                                    viewModel.searchPosts()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                    
                    // Create post prompt
                    item {
                        CreatePostPrompt(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            onClick = onNavigateToCreatePost
                        )
                    }
                    
                    // Empty state
                    if (state.posts.isEmpty() && !state.isLoading) {
                        item {
                            EmptyPostsState(
                                onRefresh = {
                                    scope.launch {
                                        viewModel.refreshPosts()
                                    }
                                }
                            )
                        }
                    }
                    
                    // Posts feed
                    items(state.posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            onLikeClick = {
                                scope.launch {
                                    viewModel.likePost(post.id)
                                }
                            },
                            // onCommentClick = { 
                            //     onNavigateToComments(post.id)
                            // },
                            onSaveClick = {
                                scope.launch {
                                    viewModel.savePost(post.id)
                                }
                            },
                            onUserClick = {
                                onNavigateToUserProfile(post.userId)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
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
                                viewModel.loadPosts()
                            }
                        }
                }
            }
        }
    }
}

    