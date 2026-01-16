package com.foodfest.app.features.home.data

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: Int,
    val userId: Int,
    val userName: String,
    val userAvatar: String? = null,
    val postType: String,
    val title: String? = null,
    val content: String? = null,
    val imageUrl: String? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val createdAt: String
)

// @Serializable
// data class Comment(
//     val id: Int,
//     val userId: Int,
//     val userName: String,
//     val userAvatar: String? = null,
//     val postId: Int,
//     val content: String,
//     val createdAt: String
// )

@Serializable
data class CreatePostRequest(
    val postType: String,
    val title: String? = null,
    val content: String? = null,
    val imageUrl: String? = null
)

// @Serializable
// data class CreateCommentRequest(
//     val content: String
// )

@Serializable
data class PostResponse(
    val data: List<Post>,
    val page: Int,
    val limit: Int,
    val total: Int
)

// @Serializable
// data class CommentResponse(
//     val data: List<Comment>,
//     val page: Int,
//     val limit: Int,
//     val total: Int
// )
