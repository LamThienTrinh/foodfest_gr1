package com.foodfest.app.features.savedposts.presentation.models

import com.foodfest.app.features.home.data.Comment
import com.foodfest.app.features.home.data.Post
import com.foodfest.app.features.home.presentation.models.CommentThreadState

data class SavedPostsState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true,
    val isCommentSheetVisible: Boolean = false,
    val selectedCommentPostId: Int? = null,
    val selectedPostCommentCount: Int = 0,
    val selectedReplyCommentId: Int? = null,
    val selectedReplyUserName: String? = null,
    val comments: List<Comment> = emptyList(),
    val commentThreadStates: Map<Int, CommentThreadState> = emptyMap(),
    val isCommentsLoading: Boolean = false,
    val isCommentSubmitting: Boolean = false,
    val commentsErrorMessage: String? = null,
    val commentInput: String = ""
)
