package com.foodfest.app.features.home.presentation.models

import com.foodfest.app.features.home.data.Comment

data class CommentThreadState(
    val expanded: Boolean = false,
    val replies: List<Comment> = emptyList(),
    val isLoadingReplies: Boolean = false,
    val hasMoreReplies: Boolean = false,
    val currentRepliesPage: Int = 1,
    val repliesErrorMessage: String? = null
)