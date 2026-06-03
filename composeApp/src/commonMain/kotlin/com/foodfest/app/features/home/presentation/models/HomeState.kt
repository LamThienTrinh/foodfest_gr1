package com.foodfest.app.features.home.presentation.models

import com.foodfest.app.features.home.data.Comment
import com.foodfest.app.features.home.data.Post

data class HomeState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true,
    val searchQuery: String = "",
    val searchType: String = "post", // "post" hoặc "user"
    val selectedPostType: String? = null,  // null = all types
    val feedMode: HomeFeedMode = HomeFeedMode.ALL,
    val isCommentSheetVisible: Boolean = false,
    val selectedCommentPostId: Int? = null,
    val selectedPostCommentCount: Int = 0,
    val selectedReplyCommentId: Int? = null,
    val selectedReplyUserName: String? = null,
    val comments: List<Comment> = emptyList(),
    val commentThreadStates: Map<Int, CommentThreadState> = emptyMap(),
    val authorFollowStates: Map<Int, Boolean> = emptyMap(),
    val followLoadingAuthorIds: Set<Int> = emptySet(),
    val isCommentsLoading: Boolean = false,
    val isCommentSubmitting: Boolean = false,
    val commentsErrorMessage: String? = null,
    val commentInput: String = ""
)
