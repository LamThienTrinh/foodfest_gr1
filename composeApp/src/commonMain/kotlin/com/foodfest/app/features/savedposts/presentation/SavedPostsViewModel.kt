package com.foodfest.app.features.savedposts.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.home.data.Comment
import com.foodfest.app.features.home.data.PostRepository
import com.foodfest.app.features.home.presentation.models.CommentThreadState
import com.foodfest.app.features.savedposts.data.SavedPostsRepository
import com.foodfest.app.features.savedposts.presentation.models.SavedPostsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SavedPostsViewModel(
    private val repository: SavedPostsRepository = SavedPostsRepository(),
    private val postRepository: PostRepository = PostRepository()
) {
    private companion object {
        const val COMMENTS_PAGE_LIMIT = 20
        const val REPLIES_PAGE_LIMIT = 10
    }

    var state by mutableStateOf(SavedPostsState())
        private set
    
    private val scope = CoroutineScope(Dispatchers.Main)

    private fun defaultThreadState(comment: Comment): CommentThreadState {
        return CommentThreadState(
            expanded = false,
            replies = emptyList(),
            isLoadingReplies = false,
            hasMoreReplies = comment.replyCount > 0,
            currentRepliesPage = 1,
            repliesErrorMessage = null
        )
    }

    private fun buildInitialThreadStates(comments: List<Comment>): Map<Int, CommentThreadState> {
        return comments.associate { comment ->
            comment.id to defaultThreadState(comment)
        }
    }

    private fun updateThreadState(
        commentId: Int,
        transform: (CommentThreadState) -> CommentThreadState
    ) {
        val parentComment = state.comments.firstOrNull { it.id == commentId }
        val current = state.commentThreadStates[commentId]
            ?: parentComment?.let { defaultThreadState(it) }
            ?: CommentThreadState()

        state = state.copy(
            commentThreadStates = state.commentThreadStates + (commentId to transform(current))
        )
    }
    
    fun loadSavedPosts(refresh: Boolean = false) {
        if (state.isLoading) return
        
        val page = if (refresh) 1 else state.currentPage
        
        state = state.copy(
            isLoading = true,
            isRefreshing = refresh,
            errorMessage = null
        )
        
        scope.launch {
            repository.getSavedPosts(page = page).fold(
                onSuccess = { response ->
                    val newPosts = if (refresh) {
                        response.data
                    } else {
                        state.posts + response.data
                    }
                    
                    state = state.copy(
                        posts = newPosts,
                        isLoading = false,
                        isRefreshing = false,
                        currentPage = response.page + 1,
                        hasMorePages = response.page < response.totalPages
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "Có lỗi xảy ra"
                    )
                }
            )
        }
    }
    
    fun refreshPosts() {
        loadSavedPosts(refresh = true)
    }
    
    fun likePost(postId: Int) {
        scope.launch {
            repository.likePost(postId).fold(
                onSuccess = {
                    // Toggle like status locally
                    state = state.copy(
                        posts = state.posts.map { post ->
                            if (post.id == postId) {
                                post.copy(
                                    isLiked = !post.isLiked,
                                    likeCount = if (post.isLiked) post.likeCount - 1 else post.likeCount + 1
                                )
                            } else post
                        }
                    )
                },
                onFailure = { error ->
                    state = state.copy(errorMessage = error.message)
                }
            )
        }
    }
    
    fun unsavePost(postId: Int) {
        scope.launch {
            repository.unsavePost(postId).fold(
                onSuccess = {
                    // Remove post from saved list
                    state = state.copy(
                        posts = state.posts.filter { it.id != postId }
                    )
                },
                onFailure = { error ->
                    state = state.copy(errorMessage = error.message)
                }
            )
        }
    }

    fun openComments(postId: Int, totalCommentCount: Int) {
        state = state.copy(
            isCommentSheetVisible = true,
            selectedCommentPostId = postId,
            selectedPostCommentCount = totalCommentCount,
            selectedReplyCommentId = null,
            selectedReplyUserName = null,
            comments = emptyList(),
            commentThreadStates = emptyMap(),
            isCommentsLoading = true,
            isCommentSubmitting = false,
            commentsErrorMessage = null,
            commentInput = ""
        )

        scope.launch {
            loadCommentsForSelectedPost()
        }
    }

    fun closeComments() {
        state = state.copy(
            isCommentSheetVisible = false,
            selectedCommentPostId = null,
            selectedPostCommentCount = 0,
            selectedReplyCommentId = null,
            selectedReplyUserName = null,
            comments = emptyList(),
            commentThreadStates = emptyMap(),
            isCommentsLoading = false,
            isCommentSubmitting = false,
            commentsErrorMessage = null,
            commentInput = ""
        )
    }

    fun updateCommentInput(input: String) {
        state = state.copy(commentInput = input)
    }

    fun startReply(comment: Comment) {
        if (comment.parentCommentId != null || comment.depth != 0) return

        state = state.copy(
            selectedReplyCommentId = comment.id,
            selectedReplyUserName = comment.userName
        )
    }

    fun cancelReply() {
        state = state.copy(
            selectedReplyCommentId = null,
            selectedReplyUserName = null
        )
    }

    fun retryLoadComments() {
        scope.launch {
            loadCommentsForSelectedPost()
        }
    }

    fun toggleCommentThread(comment: Comment) {
        if (comment.replyCount <= 0) {
            return
        }

        val currentThread = state.commentThreadStates[comment.id] ?: defaultThreadState(comment)
        if (currentThread.expanded) {
            updateThreadState(comment.id) { thread ->
                thread.copy(expanded = false, repliesErrorMessage = null)
            }
            return
        }

        updateThreadState(comment.id) { thread ->
            thread.copy(expanded = true, repliesErrorMessage = null)
        }

        val latest = state.commentThreadStates[comment.id] ?: defaultThreadState(comment)
        if (latest.replies.isEmpty() && latest.hasMoreReplies && !latest.isLoadingReplies) {
            scope.launch {
                loadRepliesForThread(comment.id)
            }
        }
    }

    fun loadMoreReplies(parentCommentId: Int) {
        scope.launch {
            loadRepliesForThread(parentCommentId)
        }
    }

    private suspend fun loadRepliesForThread(parentCommentId: Int) {
        val parentComment = state.comments.firstOrNull { it.id == parentCommentId } ?: return
        if (parentComment.replyCount <= 0) return

        val currentThread = state.commentThreadStates[parentCommentId] ?: defaultThreadState(parentComment)
        if (currentThread.isLoadingReplies || !currentThread.hasMoreReplies) return

        val page = currentThread.currentRepliesPage

        updateThreadState(parentCommentId) { thread ->
            thread.copy(
                expanded = true,
                isLoadingReplies = true,
                repliesErrorMessage = null
            )
        }

        postRepository.getReplies(
            commentId = parentCommentId,
            page = page,
            limit = REPLIES_PAGE_LIMIT
        ).fold(
            onSuccess = { response ->
                val mergedReplies = (currentThread.replies + response.data).distinctBy { it.id }
                val hasMoreReplies = mergedReplies.size < response.total
                val nextPage = if (hasMoreReplies) page + 1 else page

                updateThreadState(parentCommentId) { thread ->
                    thread.copy(
                        expanded = true,
                        replies = mergedReplies,
                        isLoadingReplies = false,
                        hasMoreReplies = hasMoreReplies,
                        currentRepliesPage = nextPage,
                        repliesErrorMessage = null
                    )
                }
            },
            onFailure = { error ->
                updateThreadState(parentCommentId) { thread ->
                    thread.copy(
                        expanded = true,
                        isLoadingReplies = false,
                        repliesErrorMessage = error.message ?: "Không thể tải phản hồi"
                    )
                }
            }
        )
    }

    fun submitComment() {
        val postId = state.selectedCommentPostId ?: return
        val content = state.commentInput.trim()
        if (content.isEmpty() || state.isCommentSubmitting) return

        val parentCommentId = state.selectedReplyCommentId

        state = state.copy(
            isCommentSubmitting = true,
            commentsErrorMessage = null
        )

        scope.launch {
            postRepository.addComment(postId, content, parentCommentId).fold(
                onSuccess = { createdComment ->
                    if (parentCommentId == null) {
                        state = state.copy(
                            comments = state.comments + createdComment,
                            commentThreadStates = state.commentThreadStates + (
                                createdComment.id to defaultThreadState(createdComment)
                            ),
                            commentInput = "",
                            isCommentSubmitting = false,
                            commentsErrorMessage = null,
                            selectedPostCommentCount = state.selectedPostCommentCount + 1,
                            selectedReplyCommentId = null,
                            selectedReplyUserName = null,
                            posts = state.posts.map { post ->
                                if (post.id == postId) {
                                    post.copy(commentCount = post.commentCount + 1)
                                } else {
                                    post
                                }
                            }
                        )
                    } else {
                        val updatedComments = state.comments.map { comment ->
                            if (comment.id == parentCommentId) {
                                comment.copy(replyCount = comment.replyCount + 1)
                            } else {
                                comment
                            }
                        }

                        val parentComment = updatedComments.firstOrNull { it.id == parentCommentId }
                        val currentThread = state.commentThreadStates[parentCommentId]
                            ?: parentComment?.let { defaultThreadState(it) }
                            ?: CommentThreadState()

                        val updatedReplies = (currentThread.replies + createdComment).distinctBy { it.id }
                        val totalReplies = parentComment?.replyCount ?: updatedReplies.size
                        val hasMoreReplies = updatedReplies.size < totalReplies

                        val updatedThread = currentThread.copy(
                            expanded = true,
                            replies = updatedReplies,
                            isLoadingReplies = false,
                            hasMoreReplies = hasMoreReplies,
                            repliesErrorMessage = null
                        )

                        state = state.copy(
                            comments = updatedComments,
                            commentThreadStates = state.commentThreadStates + (parentCommentId to updatedThread),
                            commentInput = "",
                            isCommentSubmitting = false,
                            commentsErrorMessage = null,
                            selectedPostCommentCount = state.selectedPostCommentCount + 1,
                            selectedReplyCommentId = null,
                            selectedReplyUserName = null,
                            posts = state.posts.map { post ->
                                if (post.id == postId) {
                                    post.copy(commentCount = post.commentCount + 1)
                                } else {
                                    post
                                }
                            }
                        )
                    }
                },
                onFailure = { error ->
                    state = state.copy(
                        isCommentSubmitting = false,
                        commentsErrorMessage = error.message ?: "Không thể gửi bình luận"
                    )
                }
            )
        }
    }

    private suspend fun loadCommentsForSelectedPost() {
        val postId = state.selectedCommentPostId
        if (postId == null) {
            state = state.copy(isCommentsLoading = false)
            return
        }

        state = state.copy(
            isCommentsLoading = true,
            commentsErrorMessage = null
        )

        postRepository.getComments(postId = postId, page = 1, limit = COMMENTS_PAGE_LIMIT).fold(
            onSuccess = { response ->
                val derivedTotalCommentCount = if (state.selectedPostCommentCount > 0) {
                    state.selectedPostCommentCount
                } else {
                    response.data.size + response.data.sumOf { it.replyCount }
                }

                state = state.copy(
                    comments = response.data,
                    commentThreadStates = buildInitialThreadStates(response.data),
                    isCommentsLoading = false,
                    commentsErrorMessage = null,
                    selectedPostCommentCount = derivedTotalCommentCount
                )
            },
            onFailure = { error ->
                state = state.copy(
                    isCommentsLoading = false,
                    commentsErrorMessage = error.message ?: "Không thể tải bình luận"
                )
            }
        )
    }
    
    fun clearError() {
        state = state.copy(errorMessage = null)
    }
}
