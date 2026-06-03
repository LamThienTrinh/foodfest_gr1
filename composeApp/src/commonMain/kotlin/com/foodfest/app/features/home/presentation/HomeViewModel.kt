package com.foodfest.app.features.home.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.follow.data.FollowRepository
import com.foodfest.app.features.home.data.Comment
import com.foodfest.app.features.home.data.PostRepository
import com.foodfest.app.features.home.data.PostListResponse
import com.foodfest.app.features.home.presentation.models.CommentThreadState
import com.foodfest.app.features.home.presentation.models.HomeFeedMode
import com.foodfest.app.features.home.presentation.models.HomeState

class HomeViewModel {
    private companion object {
        const val COMMENTS_PAGE_LIMIT = 20
        const val REPLIES_PAGE_LIMIT = 10
    }

    private val postRepository = PostRepository()
    private val followRepository = FollowRepository()
    
    var state by mutableStateOf(HomeState())
        private set

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

    private fun setAuthorFollowState(authorId: Int, isFollowing: Boolean) {
        state = state.copy(
            authorFollowStates = state.authorFollowStates + (authorId to isFollowing)
        )
    }

    private fun setFollowLoading(authorId: Int, isLoading: Boolean) {
        val updated = state.followLoadingAuthorIds.toMutableSet()
        if (isLoading) {
            updated.add(authorId)
        } else {
            updated.remove(authorId)
        }
        state = state.copy(followLoadingAuthorIds = updated)
    }

    private fun markFollowingAuthorsAsKnown() {
        if (state.feedMode != HomeFeedMode.FOLLOWING) return
        if (state.posts.isEmpty()) return

        val followMap = state.posts.associate { post ->
            post.userId to true
        }
        state = state.copy(authorFollowStates = state.authorFollowStates + followMap)
    }

    private suspend fun loadFeedByMode(page: Int): Result<PostListResponse> {
        return when (state.feedMode) {
            HomeFeedMode.ALL -> {
                postRepository.getPosts(
                    page = page,
                    search = state.searchQuery.takeIf { it.isNotBlank() },
                    postType = state.selectedPostType,
                    searchType = state.searchType, // Gọi API với kiểu tìm kiếm (post/user)
                    includeTrending = true // Yêu cầu backend áp dụng window function đẩy trending 7 ngày lên trước
                )
            }

            HomeFeedMode.FOLLOWING -> {
                postRepository.getFollowingPosts(
                    page = page,
                    search = state.searchQuery.takeIf { it.isNotBlank() },
                    postType = state.selectedPostType
                )
            }
        }
    }
    
    suspend fun loadPosts(refresh: Boolean = false) {
        if (state.isLoading) return
        
        val page = if (refresh) 1 else state.currentPage
        
        state = state.copy(isLoading = true, isRefreshing = refresh, errorMessage = null)

        val result = loadFeedByMode(page)
        
        result.fold(
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
                    currentPage = if (refresh) 2 else state.currentPage + 1,
                    hasMorePages = response.data.size >= response.limit
                )
                markFollowingAuthorsAsKnown()
            },
            onFailure = { e ->
                state = state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = e.message ?: "Không thể tải bài viết"
                )
            }
        )
    }
    
    suspend fun refreshPosts() {
        loadPosts(refresh = true)
    }
    
    fun updateSearchQuery(query: String) {
        state = state.copy(searchQuery = query)
    }
    
    // Cập nhật chế độ tìm kiếm
    fun updateSearchType(type: String) {
        state = state.copy(searchType = type)
    }
    
    suspend fun searchPosts() {
        state = state.copy(currentPage = 1, posts = emptyList(), hasMorePages = true)
        loadPosts(refresh = true)
    }

    suspend fun updateFeedMode(mode: HomeFeedMode) {
        if (state.feedMode == mode) return

        state = state.copy(
            feedMode = mode,
            currentPage = 1,
            posts = emptyList(),
            hasMorePages = true,
            errorMessage = null
        )

        loadPosts(refresh = true)
    }
    
    fun updatePostTypeFilter(postType: String?) {
        state = state.copy(selectedPostType = postType)
    }
    
    suspend fun likePost(postId: Int) {
        val result = postRepository.likePost(postId)
        
        result.fold(
            onSuccess = { likeResult ->
                state = state.copy(
                    posts = state.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(
                                isLiked = likeResult.isLiked,
                                likeCount = likeResult.likeCount
                            )
                        } else post
                    }
                )
            },
            onFailure = { e ->
                state = state.copy(errorMessage = e.message)
            }
        )
    }
    
    suspend fun savePost(postId: Int) {
        val result = postRepository.savePost(postId)
        
        result.fold(
            onSuccess = { saveResult ->
                state = state.copy(
                    posts = state.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(isSaved = saveResult.isSaved)
                        } else post
                    }
                )
            },
            onFailure = { e ->
                state = state.copy(errorMessage = e.message)
            }
        )
    }

    suspend fun toggleFollowAuthor(authorId: Int) {
        if (state.followLoadingAuthorIds.contains(authorId)) return

        if (state.authorFollowStates[authorId] == null) {
            setFollowLoading(authorId, true)
            val statusResult = followRepository.isFollowing(authorId)
            setFollowLoading(authorId, false)

            val resolved = statusResult.getOrElse { error ->
                state = state.copy(errorMessage = error.message ?: "Không thể kiểm tra theo dõi")
                return
            }

            setAuthorFollowState(authorId, resolved)

            // If already following, first tap should only sync state to avoid accidental unfollow.
            if (resolved) return
        }

        setFollowLoading(authorId, true)
        followRepository.toggleFollow(authorId).fold(
            onSuccess = { result ->
                setAuthorFollowState(authorId, result.isFollowing)

                if (state.feedMode == HomeFeedMode.FOLLOWING) {
                    loadPosts(refresh = true)
                }
            },
            onFailure = { error ->
                state = state.copy(errorMessage = error.message ?: "Không thể cập nhật theo dõi")
            }
        )
        setFollowLoading(authorId, false)
    }

    suspend fun openComments(postId: Int, totalCommentCount: Int) {
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

        loadCommentsForSelectedPost()
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

    suspend fun retryLoadComments() {
        loadCommentsForSelectedPost()
    }

    suspend fun toggleCommentThread(comment: Comment) {
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
            loadRepliesForThread(comment.id)
        }
    }

    suspend fun loadMoreReplies(parentCommentId: Int) {
        loadRepliesForThread(parentCommentId)
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

    suspend fun submitComment() {
        val postId = state.selectedCommentPostId ?: return
        val content = state.commentInput.trim()
        if (content.isEmpty() || state.isCommentSubmitting) return

        val parentCommentId = state.selectedReplyCommentId

        state = state.copy(
            isCommentSubmitting = true,
            commentsErrorMessage = null
        )

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
            onFailure = { e ->
                state = state.copy(
                    isCommentSubmitting = false,
                    commentsErrorMessage = e.message ?: "Không thể gửi bình luận"
                )
            }
        )
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
            onFailure = { e ->
                state = state.copy(
                    isCommentsLoading = false,
                    commentsErrorMessage = e.message ?: "Không thể tải bình luận"
                )
            }
        )
    }
    
    fun clearError() {
        state = state.copy(errorMessage = null)
    }
}
