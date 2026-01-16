package com.foodfest.app.features.savedposts.presentation.models

import com.foodfest.app.features.home.data.Post

data class SavedPostsState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true
)
