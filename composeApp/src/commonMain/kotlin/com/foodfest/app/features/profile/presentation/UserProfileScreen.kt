package com.foodfest.app.features.profile.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.features.auth.data.AuthRepository
import com.foodfest.app.features.auth.data.PublicUserProfile
import com.foodfest.app.features.follow.data.FollowRepository
import com.foodfest.app.features.home.data.Post
import com.foodfest.app.features.home.data.PostRepository
import com.foodfest.app.theme.AppColors
import foodfest.composeapp.generated.resources.Res
import foodfest.composeapp.generated.resources.default_avatar
import com.foodfest.app.features.home.data.Comment
import com.foodfest.app.features.home.presentation.models.CommentThreadState
import com.foodfest.app.features.home.presentation.components.CommentBottomSheet
import com.foodfest.app.features.home.presentation.components.PostCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

data class UserProfileState(
    val profile: PublicUserProfile? = null,
    val isProfileLoading: Boolean = false,
    val profileErrorMessage: String? = null,
    val isFollowLoading: Boolean = false,
    val followErrorMessage: String? = null,
    val posts: List<Post> = emptyList(),
    val isPostsLoading: Boolean = false,
    val postsErrorMessage: String? = null,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true,
    val isCommentSheetVisible: Boolean = false,
    val selectedCommentPostId: Int? = null,
    val selectedPostCommentCount: Int = 0,
    val comments: List<Comment> = emptyList(),
    val isCommentsLoading: Boolean = false,
    val commentsErrorMessage: String? = null,
    val commentInput: String = "",
    val isCommentSubmitting: Boolean = false,
    val commentThreadStates: Map<Int, CommentThreadState> = emptyMap(),
    val selectedReplyCommentId: Int? = null,
    val selectedReplyUserName: String? = null
)

class UserProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val postRepository: PostRepository = PostRepository(),
    private val followRepository: FollowRepository = FollowRepository()
) {
    private companion object {
        const val COMMENTS_PAGE_LIMIT = 20
        const val REPLIES_PAGE_LIMIT = 10
    }
    var state by mutableStateOf(UserProfileState())
        private set

    private val scope = CoroutineScope(Dispatchers.Main)

    fun loadInitial(userId: Int, currentUserId: Int?) {
        state = UserProfileState(
            isProfileLoading = true,
            isPostsLoading = true,
            currentPage = 1,
            hasMorePages = true
        )

        loadProfile(userId, currentUserId)
        loadPosts(userId = userId, refresh = true, force = true)
    }

    fun retryProfile(userId: Int, currentUserId: Int?) {
        loadProfile(userId, currentUserId)
    }

    fun retryPosts(userId: Int) {
        loadPosts(userId = userId, refresh = true, force = true)
    }

    fun loadMorePosts(userId: Int) {
        loadPosts(userId = userId, refresh = false, force = false)
    }

    private fun loadProfile(userId: Int, currentUserId: Int?) {
        state = state.copy(
            isProfileLoading = true,
            profileErrorMessage = null,
            followErrorMessage = null
        )

        scope.launch {
            authRepository.getPublicProfile(userId).fold(
                onSuccess = { profile ->
                    state = state.copy(
                        profile = profile,
                        isProfileLoading = false,
                        profileErrorMessage = null
                    )

                    if (shouldShowFollowButton(currentUserId, profile.id)) {
                        syncFollowState(profile.id)
                    }
                },
                onFailure = { error ->
                    state = state.copy(
                        isProfileLoading = false,
                        profileErrorMessage = error.message ?: "Không thể tải trang cá nhân"
                    )
                }
            )
        }
    }

    fun toggleFollow(currentUserId: Int?) {
        val profile = state.profile ?: return
        if (state.isFollowLoading) return
        if (!shouldShowFollowButton(currentUserId, profile.id)) return

        state = state.copy(
            isFollowLoading = true,
            followErrorMessage = null
        )

        scope.launch {
            followRepository.toggleFollow(profile.id).fold(
                onSuccess = { result ->
                    val currentProfile = state.profile
                    if (currentProfile == null || currentProfile.id != profile.id) {
                        state = state.copy(isFollowLoading = false)
                        return@fold
                    }

                    state = state.copy(
                        isFollowLoading = false,
                        followErrorMessage = null,
                        profile = currentProfile.copy(
                            isFollowing = result.isFollowing,
                            followerCount = result.followerCount
                        )
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        isFollowLoading = false,
                        followErrorMessage = error.message ?: "Không thể cập nhật theo dõi"
                    )
                }
            )
        }
    }

    private fun syncFollowState(targetUserId: Int) {
        scope.launch {
            followRepository.isFollowing(targetUserId).onSuccess { isFollowing ->
                val currentProfile = state.profile ?: return@onSuccess
                if (currentProfile.id != targetUserId) return@onSuccess

                state = state.copy(
                    profile = currentProfile.copy(isFollowing = isFollowing)
                )
            }
        }
    }

    private fun shouldShowFollowButton(currentUserId: Int?, profileUserId: Int): Boolean {
        return currentUserId != null && currentUserId > 0 && currentUserId != profileUserId
    }

    private fun loadPosts(userId: Int, refresh: Boolean, force: Boolean) {
        if (state.isPostsLoading && !force) return

        val page = if (refresh) 1 else state.currentPage

        state = state.copy(
            isPostsLoading = true,
            postsErrorMessage = if (refresh) null else state.postsErrorMessage
        )

        scope.launch {
            postRepository.getUserPosts(userId = userId, page = page).fold(
                onSuccess = { response ->
                    val mergedPosts = if (refresh) {
                        response.data
                    } else {
                        state.posts + response.data
                    }

                    val hasMorePages = mergedPosts.size < response.total
                    state = state.copy(
                        posts = mergedPosts,
                        isPostsLoading = false,
                        postsErrorMessage = null,
                        currentPage = if (hasMorePages) page + 1 else page,
                        hasMorePages = hasMorePages
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        isPostsLoading = false,
                        postsErrorMessage = error.message ?: "Không thể tải bài đăng"
                    )
                }
            )
        }
    }

    // --- Xử lý Like bài đăng ---
    fun likePost(postId: Int) {
        scope.launch {
            postRepository.likePost(postId).onSuccess { likeResult ->
                state = state.copy(
                    posts = state.posts.map { post ->
                        if (post.id == postId) post.copy(isLiked = likeResult.isLiked, likeCount = likeResult.likeCount) else post
                    }
                )
            }
        }
    }

    // --- State helpers cho Comment ---
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

    fun updateCommentInput(input: String) { state = state.copy(commentInput = input) }

    fun startReply(comment: Comment) {
        if (comment.parentCommentId != null || comment.depth != 0) return
        state = state.copy(selectedReplyCommentId = comment.id, selectedReplyUserName = comment.userName)
    }

    fun cancelReply() {
        state = state.copy(selectedReplyCommentId = null, selectedReplyUserName = null)
    }

    suspend fun retryLoadComments() { loadCommentsForSelectedPost() }

    suspend fun toggleCommentThread(comment: Comment) {
        if (comment.replyCount <= 0) return
        val currentThread = state.commentThreadStates[comment.id] ?: defaultThreadState(comment)
        if (currentThread.expanded) {
            updateThreadState(comment.id) { thread -> thread.copy(expanded = false, repliesErrorMessage = null) }
            return
        }

        updateThreadState(comment.id) { thread -> thread.copy(expanded = true, repliesErrorMessage = null) }
        val latest = state.commentThreadStates[comment.id] ?: defaultThreadState(comment)
        if (latest.replies.isEmpty() && latest.hasMoreReplies && !latest.isLoadingReplies) {
            loadRepliesForThread(comment.id)
        }
    }

    suspend fun loadMoreReplies(parentCommentId: Int) { loadRepliesForThread(parentCommentId) }

    private suspend fun loadRepliesForThread(parentCommentId: Int) {
        val parentComment = state.comments.firstOrNull { it.id == parentCommentId } ?: return
        if (parentComment.replyCount <= 0) return

        val currentThread = state.commentThreadStates[parentCommentId] ?: defaultThreadState(parentComment)
        if (currentThread.isLoadingReplies || !currentThread.hasMoreReplies) return

        val page = currentThread.currentRepliesPage
        updateThreadState(parentCommentId) { thread -> thread.copy(expanded = true, isLoadingReplies = true, repliesErrorMessage = null) }

        postRepository.getReplies(commentId = parentCommentId, page = page, limit = REPLIES_PAGE_LIMIT).fold(
            onSuccess = { response ->
                val mergedReplies = (currentThread.replies + response.data).distinctBy { it.id }
                val hasMoreReplies = mergedReplies.size < response.total
                val nextPage = if (hasMoreReplies) page + 1 else page
                updateThreadState(parentCommentId) { thread ->
                    thread.copy(expanded = true, replies = mergedReplies, isLoadingReplies = false, hasMoreReplies = hasMoreReplies, currentRepliesPage = nextPage, repliesErrorMessage = null)
                }
            },
            onFailure = { error ->
                updateThreadState(parentCommentId) { thread ->
                    thread.copy(expanded = true, isLoadingReplies = false, repliesErrorMessage = error.message ?: "Không thể tải phản hồi")
                }
            }
        )
    }

    suspend fun submitComment() {
        val postId = state.selectedCommentPostId ?: return
        val content = state.commentInput.trim()
        if (content.isEmpty() || state.isCommentSubmitting) return

        val parentCommentId = state.selectedReplyCommentId
        state = state.copy(isCommentSubmitting = true, commentsErrorMessage = null)

        postRepository.addComment(postId, content, parentCommentId).fold(
            onSuccess = { createdComment ->
                if (parentCommentId == null) {
                    state = state.copy(
                        comments = state.comments + createdComment,
                        commentThreadStates = state.commentThreadStates + (createdComment.id to defaultThreadState(createdComment)),
                        commentInput = "",
                        isCommentSubmitting = false,
                        commentsErrorMessage = null,
                        selectedPostCommentCount = state.selectedPostCommentCount + 1,
                        selectedReplyCommentId = null,
                        selectedReplyUserName = null,
                        posts = state.posts.map { post ->
                            if (post.id == postId) post.copy(commentCount = post.commentCount + 1) else post
                        }
                    )
                } else {
                    val updatedComments = state.comments.map { comment ->
                        if (comment.id == parentCommentId) comment.copy(replyCount = comment.replyCount + 1) else comment
                    }
                    val parentComment = updatedComments.firstOrNull { it.id == parentCommentId }
                    val currentThread = state.commentThreadStates[parentCommentId] ?: parentComment?.let { defaultThreadState(it) } ?: CommentThreadState()
                    val updatedReplies = (currentThread.replies + createdComment).distinctBy { it.id }
                    val totalReplies = parentComment?.replyCount ?: updatedReplies.size
                    val hasMoreReplies = updatedReplies.size < totalReplies
                    val updatedThread = currentThread.copy(expanded = true, replies = updatedReplies, isLoadingReplies = false, hasMoreReplies = hasMoreReplies, repliesErrorMessage = null)

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
                            if (post.id == postId) post.copy(commentCount = post.commentCount + 1) else post
                        }
                    )
                }
            },
            onFailure = { e ->
                state = state.copy(isCommentSubmitting = false, commentsErrorMessage = e.message ?: "Không thể gửi bình luận")
            }
        )
    }

    private suspend fun loadCommentsForSelectedPost() {
        val postId = state.selectedCommentPostId
        if (postId == null) {
            state = state.copy(isCommentsLoading = false)
            return
        }
        state = state.copy(isCommentsLoading = true, commentsErrorMessage = null)
        postRepository.getComments(postId = postId, page = 1, limit = COMMENTS_PAGE_LIMIT).fold(
            onSuccess = { response ->
                val derivedTotalCommentCount = if (state.selectedPostCommentCount > 0) state.selectedPostCommentCount else response.data.size + response.data.sumOf { it.replyCount }
                state = state.copy(
                    comments = response.data,
                    commentThreadStates = buildInitialThreadStates(response.data),
                    isCommentsLoading = false,
                    commentsErrorMessage = null,
                    selectedPostCommentCount = derivedTotalCommentCount
                )
            },
            onFailure = { e ->
                state = state.copy(isCommentsLoading = false, commentsErrorMessage = e.message ?: "Không thể tải bình luận")
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: Int?,
    currentUserId: Int?,
    onBack: () -> Unit,
    viewModel: UserProfileViewModel = remember { UserProfileViewModel() }
) {
    val state = viewModel.state
    val listState = rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(userId, currentUserId) {
        if (userId != null && userId > 0) {
            viewModel.loadInitial(userId, currentUserId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.profile?.fullName ?: "Trang cá nhân",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Brown
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = AppColors.Brown
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        when {
            userId == null || userId <= 0 -> {
                ProfileStateMessage(
                    title = "User không hợp lệ",
                    description = "Không thể mở trang cá nhân với userId hiện tại.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            state.isProfileLoading && state.profile == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Orange)
                }
            }

            state.profileErrorMessage != null && state.profile == null -> {
                ProfileStateMessage(
                    title = "Không thể tải profile",
                    description = state.profileErrorMessage,
                    actionLabel = "Thử lại",
                                    onAction = { viewModel.retryProfile(userId, currentUserId) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(AppColors.Background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.profile?.let { profile ->
                        item {
                            PublicProfileHeader(
                                profile = profile,
                                showFollowButton = currentUserId != null && currentUserId > 0 && currentUserId != profile.id,
                                isFollowLoading = state.isFollowLoading,
                                onFollowClick = { viewModel.toggleFollow(currentUserId) }
                            )
                        }

                        if (!state.followErrorMessage.isNullOrBlank()) {
                            item {
                                Text(
                                    text = state.followErrorMessage,
                                    color = Color(0xFFB00020),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }

                    when {
                        state.isPostsLoading && state.posts.isEmpty() -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = AppColors.Orange)
                                }
                            }
                        }

                        state.postsErrorMessage != null && state.posts.isEmpty() -> {
                            item {
                                ProfileStateMessage(
                                    title = "Không thể tải bài đăng",
                                    description = state.postsErrorMessage,
                                    actionLabel = "Thử lại",
                                    onAction = { viewModel.retryPosts(userId) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        state.posts.isEmpty() -> {
                            item {
                                ProfileStateMessage(
                                    title = "Chưa có bài đăng",
                                    description = "Người dùng này chưa đăng bài nào.",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        else -> {
                            item {
                                Text(
                                    text = "${state.posts.size} bài đăng",
                                    fontSize = 14.sp,
                                    color = AppColors.GrayPlaceholder
                                )
                            }

                            items(state.posts, key = { it.id }) { post ->
                                PostCard(
                                    post = post,
                                    onLikeClick = { viewModel.likePost(post.id) },
                                    onCommentClick = { 
                                        scope.launch { viewModel.openComments(post.id, post.commentCount) }
                                    },
                                    onSaveClick = { }, // Đã bị ẩn trên MyPosts/Profile
                                    onUserClick = { },
                                    showFollowButton = false
                                )
                            }

                            if (state.isPostsLoading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(30.dp),
                                            color = AppColors.Orange
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                LaunchedEffect(listState, state.hasMorePages, state.isPostsLoading, state.posts.size, userId) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { lastVisibleIndex ->
                            if (
                                lastVisibleIndex != null &&
                                lastVisibleIndex >= state.posts.size - 2 &&
                                !state.isPostsLoading &&
                                state.hasMorePages
                            ) {
                                viewModel.loadMorePosts(userId)
                            }
                        }
                }
            }
        }

        // Tích hợp thanh Comment Bottom Sheet
        if (state.isCommentSheetVisible) {
            CommentBottomSheet(
                comments = state.comments,
                totalCommentCount = state.selectedPostCommentCount,
                threadStates = state.commentThreadStates,
                inputText = state.commentInput,
                replyingToUserName = state.selectedReplyUserName,
                isLoading = state.isCommentsLoading,
                isSubmitting = state.isCommentSubmitting,
                errorMessage = state.commentsErrorMessage,
                onToggleThread = { scope.launch { viewModel.toggleCommentThread(it) } },
                onReplyClick = { viewModel.startReply(it) },
                onLoadMoreReplies = { scope.launch { viewModel.loadMoreReplies(it) } },
                onInputTextChange = { viewModel.updateCommentInput(it) },
                onCancelReply = { viewModel.cancelReply() },
                onSubmit = { scope.launch { viewModel.submitComment() } },
                onDismiss = { viewModel.closeComments() },
                onRetryLoad = { scope.launch { viewModel.retryLoadComments() } }
            )
        }
    }
}

@Composable
private fun PublicProfileHeader(
    profile: PublicUserProfile,
    showFollowButton: Boolean,
    isFollowLoading: Boolean,
    onFollowClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.default_avatar),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.fullName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "@${profile.username}",
                        fontSize = 13.sp,
                        color = AppColors.GrayPlaceholder
                    )
                }

                if (showFollowButton) {
                    val isFollowing = profile.isFollowing == true
                    Button(
                        onClick = onFollowClick,
                        enabled = !isFollowLoading,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) Color(0xFFE0E0E0) else Color(0xFF2E7D32),
                            contentColor = if (isFollowing) Color(0xFF616161) else Color.White
                        )
                    ) {
                        if (isFollowLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = if (isFollowing) Color(0xFF616161) else Color.White
                            )
                        } else {
                            Text(
                                text = if (isFollowing) "Followed" else "Follow",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProfileStatChip(label = "Followers", value = profile.followerCount)
                ProfileStatChip(label = "Following", value = profile.followingCount)
                ProfileStatChip(label = "Likes", value = profile.totalReceivedLikes)
                ProfileStatChip(label = "Posts", value = profile.postCount)
            }
        }
    }
}

@Composable
private fun ProfileStatChip(label: String, value: Int) {
    Surface(
        color = AppColors.Background,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = AppColors.GrayPlaceholder
            )
        }
    }
}

// Đã xóa UserPostHistoryCard do chuyển sang dùng chung PostCard

@Composable
private fun ProfileStateMessage(
    title: String,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        )
        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                color = AppColors.GrayPlaceholder,
                fontSize = 14.sp
            )
        }
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
