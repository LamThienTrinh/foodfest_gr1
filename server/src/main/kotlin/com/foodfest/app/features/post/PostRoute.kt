package com.foodfest.app.features.post

import com.foodfest.app.core.response.ApiResponse
import com.foodfest.app.core.exception.toAppStatus
import com.foodfest.app.plugins.userId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

// Response DTOs
@Serializable
data class DeletePostData(
    val deleted: Boolean,
    val postId: Int,
    val message: String
)

fun Route.postRoutes(postService: PostService) {
    route("/api/posts") {

        // =================================================================
        // 1. ƯU TIÊN ROUTE CỤ THỂ & CÓ AUTHENTICATION (Đưa lên đầu)
        // =================================================================
        authenticate("auth-jwt") {
            // Get feed of users that current user follows
            get("/feed/following") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val search = call.request.queryParameters["search"]?.takeIf { it.isNotBlank() }
                val postType = call.request.queryParameters["postType"]?.takeIf { it.isNotBlank() }

                postService.getFollowingFeed(page, principal.userId, search, postType)
                    .onSuccess { result ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get following feed"))
                    }
            }

            // Get saved posts (QUAN TRỌNG: Phải đặt trước get("/{postId}"))
            get("/saved") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1

                postService.getSavedPosts(principal.userId, page)
                    .onSuccess { result ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get saved posts"))
                    }
            }

            // Create new post
            post {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val request = runCatching { call.receive<CreatePostRequest>() }.getOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                postService.createPost(principal.userId, request)
                    .onSuccess { post ->
                        call.respond(HttpStatusCode.Created, ApiResponse.success(post, "Đăng bài thành công"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to create post"))
                    }
            }

            // Delete post
            delete("/{postId}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val postId = call.parameters["postId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid post id"))

                postService.deletePost(principal.userId, postId)
                    .onSuccess {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse.success(DeletePostData(true, postId, "Đã xóa bài viết"))
                        )
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to delete post"))
                    }
            }

            // Cập nhật bài viết của chính mình.
            put("/{postId}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val postId = call.parameters["postId"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid post id"))

                val request = runCatching { call.receive<UpdatePostRequest>() }.getOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                postService.updatePost(principal.userId, postId, request)
                    .onSuccess { updatedPost ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(updatedPost, "Đã cập nhật bài viết"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to update post"))
                    }
            }

            // Like/Unlike post
            post("/{postId}/like") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val postId = call.parameters["postId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid post id"))

                postService.likePost(principal.userId, postId)
                    .onSuccess { result ->
                        val message = if (result.isLiked) "Đã thích" else "Đã bỏ thích"
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result, message))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to like post"))
                    }
            }

            // Save/Unsave post
            post("/{postId}/save") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val postId = call.parameters["postId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid post id"))

                postService.savePost(principal.userId, postId)
                    .onSuccess { result ->
                        val message = if (result.isSaved) "Đã lưu bài viết" else "Đã bỏ lưu"
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result, message))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to save post"))
                    }
            }

            // Add comment to post
            post("/{postId}/comments") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val postId = call.parameters["postId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid post id"))

                val request = runCatching { call.receive<CreateCommentRequest>() }.getOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                postService.addComment(principal.userId, postId, request)
                    .onSuccess { comment ->
                        call.respond(HttpStatusCode.Created, ApiResponse.success(comment, "Đã thêm bình luận"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to add comment"))
                    }
            }
        }

        // =================================================================
        // 2. ROUTE PUBLIC (Đặt phía sau để tránh nuốt mất route cụ thể)
        // =================================================================

        authenticate("auth-jwt", optional = true) {
            // Get posts feed (public, nhưng có thể check liked/saved nếu đăng nhập)
            get {
                val principal = call.principal<JWTPrincipal>()
                val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val search = call.request.queryParameters["search"]?.takeIf { it.isNotBlank() }
                val postType = call.request.queryParameters["postType"]?.takeIf { it.isNotBlank() }
                val searchType = call.request.queryParameters["searchType"]?.trim()?.lowercase() ?: "post"

                // Parse boolean chặt để tránh gọi sai contract.
                val includeTrendingRaw = call.request.queryParameters["includeTrending"]
                val includeTrending = when (includeTrendingRaw?.trim()?.lowercase()) {
                    null -> false
                    "true" -> true
                    "false" -> false
                    else -> {
                        return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse.error<Unit>("includeTrending must be true or false")
                        )
                    }
                }

                postService.getPosts(page, principal?.userId, search, postType, searchType, includeTrending)
                    .onSuccess { result ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get posts"))
                    }
            }

            // Get comments of a post
            get("/{postId}/comments") {
                val principal = call.principal<JWTPrincipal>()
                val postId = call.parameters["postId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid post id"))

                val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 20

                postService.getComments(postId, page, limit, principal?.userId)
                    .onSuccess { result ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get comments"))
                    }
            }

            // Get single post (Đây là WILDCARD route, nên để CUỐI CÙNG trong nhóm GET)
            get("/{postId}") {
                val principal = call.principal<JWTPrincipal>()
                val postId = call.parameters["postId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid post id"))

                postService.getPostById(postId, principal?.userId)
                    .onSuccess { post ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(post))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get post"))
                    }
            }
        }
    }

    route("/api/comments") {
        authenticate("auth-jwt") {
            // Delete a comment/reply authored by current user
            delete("/{commentId}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val commentId = call.parameters["commentId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid comment id"))

                postService.deleteComment(principal.userId, commentId)
                    .onSuccess { result ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result, "Da xoa binh luan"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to delete comment"))
                    }
            }
        }

        authenticate("auth-jwt", optional = true) {
            // Get level-2 replies of a level-1 comment
            get("/{commentId}/replies") {
                val principal = call.principal<JWTPrincipal>()
                val commentId = call.parameters["commentId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid comment id"))

                val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 20

                postService.getReplies(commentId, page, limit, principal?.userId)
                    .onSuccess { result ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get replies"))
                    }
            }
        }
    }

    // =================================================================
    // 3. USER POSTS ROUTE (Riêng biệt, không ảnh hưởng)
    // =================================================================
    route("/api/users/{userId}/posts") {
        authenticate("auth-jwt", optional = true) {
            get {
                val principal = call.principal<JWTPrincipal>()

                val userId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid user id"))

                val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val startDate = call.request.queryParameters["startDate"]
                val endDate = call.request.queryParameters["endDate"]

                postService.getUserPosts(userId, page, principal?.userId, startDate, endDate)
                    .onSuccess { result ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get user posts"))
                    }
            }
        }
    }
}
