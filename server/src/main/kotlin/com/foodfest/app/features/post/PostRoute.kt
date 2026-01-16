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
data class DeletePostData(val deleted: Boolean, val message: String)

fun Route.postRoutes(postService: PostService) {
    route("/api/posts") {

        // =================================================================
        // 1. ƯU TIÊN ROUTE CỤ THỂ & CÓ AUTHENTICATION (Đưa lên đầu)
        // =================================================================
        authenticate("auth-jwt") {
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
                        call.respond(HttpStatusCode.OK, ApiResponse.success(DeletePostData(true, "Đã xóa bài viết")))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to delete post"))
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

        // Get posts feed (public, nhưng có thể check liked/saved nếu đăng nhập)
        get {
            val principal = call.principal<JWTPrincipal>()
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val search = call.request.queryParameters["search"]?.takeIf { it.isNotBlank() }
            val postType = call.request.queryParameters["postType"]?.takeIf { it.isNotBlank() }

            postService.getPosts(page, principal?.userId, search, postType)
                .onSuccess { result ->
                    call.respond(HttpStatusCode.OK, ApiResponse.success(result))
                }
                .onFailure { error ->
                    call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get posts"))
                }
        }

        // Get comments of a post
        get("/{postId}/comments") {
            val postId = call.parameters["postId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid post id"))

            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1

            postService.getComments(postId, page)
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

    // =================================================================
    // 3. USER POSTS ROUTE (Riêng biệt, không ảnh hưởng)
    // =================================================================
    route("/api/users/{userId}/posts") {
        get {
            val principal = call.principal<JWTPrincipal>()

            val userId = call.parameters["userId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid user id"))

            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1

            postService.getUserPosts(userId, page, principal?.userId)
                .onSuccess { result ->
                    call.respond(HttpStatusCode.OK, ApiResponse.success(result))
                }
                .onFailure { error ->
                    call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get user posts"))
                }
        }
    }
}