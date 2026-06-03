package com.foodfest.app.features.profile.presentation

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.foodfest.app.features.home.data.Post
import com.foodfest.app.features.home.data.PostRepository
import com.foodfest.app.features.home.data.UpdatePostRequest
import com.foodfest.app.components.AppImage
import com.foodfest.app.features.home.presentation.components.PostCard
import com.foodfest.app.theme.AppColors
import com.foodfest.app.utils.readBytesFromPath
import com.foodfest.app.utils.resizeImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

data class MyPostsState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true,
    val startDate: String = "",
    val endDate: String = "",
    val isDeleteConfirmationVisible: Boolean = false,
    val deletingPostId: Int? = null,
    val isEditDialogVisible: Boolean = false,
    val editingPost: Post? = null,
    val editTitle: String = "",
    val editContent: String = "",
    val isActionLoading: Boolean = false,
    val actionErrorMessage: String? = null
)

class MyPostsViewModel(
    private val postRepository: PostRepository = PostRepository()
) {
    var state by mutableStateOf(MyPostsState())
        private set

    private val scope = CoroutineScope(Dispatchers.Main)

    fun loadMyPosts(userId: Int, refresh: Boolean = false) {
        if (state.isLoading) return

        val page = if (refresh) 1 else state.currentPage

        state = state.copy(
            isLoading = true,
            errorMessage = if (refresh) null else state.errorMessage
        )

        scope.launch {
            postRepository.getUserPosts(
                userId = userId, 
                page = page,
                startDate = state.startDate.takeIf { it.isNotBlank() },
                endDate = state.endDate.takeIf { it.isNotBlank() }
            ).fold(
                onSuccess = { response ->
                    val mergedPosts = if (refresh) {
                        response.data
                    } else {
                        state.posts + response.data
                    }

                    val hasMorePages = mergedPosts.size < response.total
                    state = state.copy(
                        posts = mergedPosts,
                        isLoading = false,
                        errorMessage = null,
                        currentPage = if (hasMorePages) page + 1 else page,
                        hasMorePages = hasMorePages
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Không thể tải lịch sử bài đăng"
                    )
                }
            )
        }
    }

    // Cập nhật bộ lọc ngày tháng và tải lại
    fun updateDateFilter(start: String, end: String, userId: Int) {
        state = state.copy(startDate = start, endDate = end, currentPage = 1, posts = emptyList())
        loadMyPosts(userId, refresh = true)
    }

    // Xóa Bài
    fun showDeleteConfirmation(postId: Int) { state = state.copy(isDeleteConfirmationVisible = true, deletingPostId = postId) }
    fun hideDeleteConfirmation() { state = state.copy(isDeleteConfirmationVisible = false, deletingPostId = null) }
    
    fun deletePost(userId: Int) {
        val postId = state.deletingPostId ?: return
        state = state.copy(isActionLoading = true, actionErrorMessage = null)
        scope.launch {
            postRepository.deletePost(postId).fold(
                onSuccess = {
                    hideDeleteConfirmation()
                    state = state.copy(isActionLoading = false)
                    loadMyPosts(userId, refresh = true)
                },
                onFailure = { error ->
                    state = state.copy(isActionLoading = false, actionErrorMessage = error.message ?: "Lỗi khi xoá bài đăng")
                }
            )
        }
    }

    // Sửa bài
    fun showEditDialog(post: Post) {
        state = state.copy(
            isEditDialogVisible = true, 
            editingPost = post,
            editTitle = post.title ?: "",
            editContent = post.content ?: ""
        )
    }
    fun hideEditDialog() { state = state.copy(isEditDialogVisible = false, editingPost = null) }
    
    fun updateEditForm(title: String, content: String) { 
        state = state.copy(editTitle = title, editContent = content) 
    }
    
    fun submitEditPost(userId: Int, newImageBytes: ByteArray?, newImageFileName: String?) {
        val post = state.editingPost ?: return
        state = state.copy(isActionLoading = true, actionErrorMessage = null)
        scope.launch {
            var finalImageUrl = post.imageUrl

            if (newImageBytes != null) {
                state = state.copy(actionErrorMessage = "Đang upload ảnh mới...")
                val fileName = newImageFileName ?: "edit_post_${post.id}_${Random.nextInt(100000, 999999)}.jpg"

                postRepository.uploadImage(newImageBytes, fileName).fold(
                    onSuccess = { uploadedUrl ->
                        finalImageUrl = uploadedUrl
                    },
                    onFailure = { error ->
                        state = state.copy(
                            isActionLoading = false,
                            actionErrorMessage = error.message ?: "Lỗi upload ảnh mới"
                        )
                        return@launch
                    }
                )
            }

            val request = UpdatePostRequest(
                title = state.editTitle.takeIf { it.isNotBlank() },
                content = state.editContent.takeIf { it.isNotBlank() },
                imageUrl = finalImageUrl
            )
            postRepository.updatePost(post.id, request).fold(
                onSuccess = {
                    hideEditDialog()
                    state = state.copy(isActionLoading = false)
                    loadMyPosts(userId, refresh = true)
                },
                onFailure = { error ->
                    state = state.copy(isActionLoading = false, actionErrorMessage = error.message ?: "Lỗi khi cập nhật bài đăng")
                }
            )
        }
    }

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

    fun retry(userId: Int) {
        loadMyPosts(userId = userId, refresh = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class)
@Composable
fun MyPostsScreen(
    userId: Int?,
    onBack: () -> Unit,
    viewModel: MyPostsViewModel = remember { MyPostsViewModel() }
) {
    val state = viewModel.state
    val listState = rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showEditImagePicker by remember { mutableStateOf(false) }
    var selectedEditImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedEditImageFileName by remember { mutableStateOf<String?>(null) }
    var editImageProcessingMessage by remember { mutableStateOf<String?>(null) }

    FilePicker(
        show = showEditImagePicker,
        fileExtensions = listOf("jpg", "jpeg", "png", "gif", "webp")
    ) { mpFile ->
        showEditImagePicker = false

        if (mpFile != null) {
            scope.launch {
                try {
                    editImageProcessingMessage = "Đang xử lý ảnh..."
                    val originalBytes = readBytesFromPath(mpFile.path)
                    val resizedBytes = resizeImage(
                        imageBytes = originalBytes,
                        maxWidth = 1024,
                        maxHeight = 1024,
                        quality = 85
                    )

                    selectedEditImageBytes = resizedBytes
                    selectedEditImageFileName = mpFile.path.let { path ->
                        when {
                            path.contains("/") -> path.substringAfterLast("/")
                            path.contains("\\") -> path.substringAfterLast("\\")
                            else -> "image_${Random.nextInt(100000, 999999)}.jpg"
                        }
                    }
                    editImageProcessingMessage = "✅ Đã chọn ảnh mới"
                } catch (e: Exception) {
                    selectedEditImageBytes = null
                    selectedEditImageFileName = null
                    editImageProcessingMessage = "❌ Xử lý ảnh thất bại: ${e.message}"
                }
            }
        }
    }

    LaunchedEffect(state.isEditDialogVisible, state.editingPost?.id) {
        if (!state.isEditDialogVisible) {
            selectedEditImageBytes = null
            selectedEditImageFileName = null
            editImageProcessingMessage = null
        }
    }

    LaunchedEffect(userId) {
        if (userId != null && userId > 0) {
            viewModel.loadMyPosts(userId = userId, refresh = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bài đăng của tôi",
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
                StateMessage(
                    title = "Không xác định được người dùng",
                    description = "Vui lòng đăng nhập lại để xem lịch sử bài đăng.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            state.isLoading && state.posts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Orange)
                }
            }

            state.errorMessage != null && state.posts.isEmpty() -> {
                StateMessage(
                    title = "Không thể tải lịch sử bài đăng",
                    description = state.errorMessage,
                    actionLabel = "Thử lại",
                    onAction = { viewModel.retry(userId) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            // Xóa file MyPostsScreen's items mapping ở dưới
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
                    item {
                        var tempStart by remember { mutableStateOf(state.startDate) }
                        var tempEnd by remember { mutableStateOf(state.endDate) }
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = tempStart,
                                    onValueChange = { tempStart = it },
                                    placeholder = { Text("YYYY-MM-DD") },
                                    label = { Text("Từ ngày") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = tempEnd,
                                    onValueChange = { tempEnd = it },
                                    placeholder = { Text("YYYY-MM-DD") },
                                    label = { Text("Đến ngày") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            Button(
                                onClick = { userId?.let { viewModel.updateDateFilter(tempStart, tempEnd, it) } },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Lọc ngày")
                            }
                        }
                    }
                    
                    item {
                        Text(
                            text = "${state.posts.size} bài đăng",
                            fontSize = 14.sp,
                            color = AppColors.GrayPlaceholder
                        )
                    }

                    // Giữ bộ lọc luôn hiển thị, kể cả khi danh sách đang rỗng sau khi lọc.
                    if (state.posts.isEmpty()) {
                        item {
                            StateMessage(
                                title = "Không có bài đăng trong khoảng ngày",
                                description = "Hãy đổi bộ lọc ngày hoặc xóa điều kiện để xem lại dữ liệu.",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    items(state.posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            onLikeClick = { viewModel.likePost(post.id) },
                            onCommentClick = { /* MyPostsScreen chưa hỗ trợ popup comment */ },
                            onSaveClick = { },
                            onEditClick = { viewModel.showEditDialog(post) },
                            onDeleteClick = { viewModel.showDeleteConfirmation(post.id) }
                        )
                    }

                    if (state.isLoading && state.posts.isNotEmpty()) {
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

                LaunchedEffect(listState, state.hasMorePages, state.isLoading, state.posts.size, userId) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { lastVisibleIndex ->
                            if (
                                lastVisibleIndex != null &&
                                lastVisibleIndex >= state.posts.size - 2 &&
                                !state.isLoading &&
                                state.hasMorePages
                            ) {
                                viewModel.loadMyPosts(userId = userId, refresh = false)
                            }
                        }
                }
            }
        }
        
        // Hộp thoại xác nhận xóa bài viết
        if (state.isDeleteConfirmationVisible) {
            AlertDialog(
                onDismissRequest = { viewModel.hideDeleteConfirmation() },
                title = { Text("Xác nhận xoá") },
                text = { Text("Bạn có chắc chắn muốn xoá bài viết này không? Hành động này không thể hoàn tác.") },
                confirmButton = {
                    TextButton(onClick = { userId?.let { viewModel.deletePost(it) } }, enabled = !state.isActionLoading) {
                        if (state.isActionLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Red)
                        } else {
                            Text("Xoá", color = Color.Red)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideDeleteConfirmation() }, enabled = !state.isActionLoading) {
                        Text("Hủy")
                    }
                }
            )
        }

        // Hộp thoại popup sửa bài viết
        if (state.isEditDialogVisible) {
            AlertDialog(
                onDismissRequest = { viewModel.hideEditDialog() },
                title = { Text("Sửa bài viết") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.editTitle,
                            onValueChange = { viewModel.updateEditForm(it, state.editContent) },
                            label = { Text("Tiêu đề") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.editContent,
                            onValueChange = { viewModel.updateEditForm(state.editTitle, it) },
                            label = { Text("Nội dung") },
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )

                        Text(
                            text = "Ảnh bài viết",
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Medium
                        )

                        val editPreviewDataUrl = remember(selectedEditImageBytes) {
                            selectedEditImageBytes?.let { "data:image/jpeg;base64,${Base64.encode(it)}" }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Color(0xFFF7F7F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                editPreviewDataUrl != null -> {
                                    AppImage(
                                        url = editPreviewDataUrl,
                                        contentDescription = "Ảnh mới",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                !state.editingPost?.imageUrl.isNullOrBlank() -> {
                                    AppImage(
                                        url = state.editingPost?.imageUrl.orEmpty(),
                                        contentDescription = "Ảnh hiện tại",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                else -> {
                                    Text("Bài viết chưa có ảnh", color = AppColors.GrayPlaceholder)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showEditImagePicker = true },
                                enabled = !state.isActionLoading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Chọn ảnh mới")
                            }

                            if (selectedEditImageBytes != null) {
                                TextButton(
                                    onClick = {
                                        selectedEditImageBytes = null
                                        selectedEditImageFileName = null
                                        editImageProcessingMessage = null
                                    },
                                    enabled = !state.isActionLoading
                                ) {
                                    Text("Bỏ ảnh mới")
                                }
                            }
                        }

                        if (!selectedEditImageFileName.isNullOrBlank()) {
                            Text(
                                text = "File: ${selectedEditImageFileName}",
                                color = AppColors.GrayPlaceholder,
                                fontSize = 12.sp
                            )
                        }

                        if (!editImageProcessingMessage.isNullOrBlank()) {
                            Text(
                                text = editImageProcessingMessage.orEmpty(),
                                color = if (editImageProcessingMessage?.startsWith("✅") == true) Color(0xFF2E7D32) else AppColors.GrayPlaceholder,
                                fontSize = 12.sp
                            )
                        }

                        if (state.actionErrorMessage != null) {
                            Text(state.actionErrorMessage, color = Color.Red, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            userId?.let {
                                viewModel.submitEditPost(
                                    userId = it,
                                    newImageBytes = selectedEditImageBytes,
                                    newImageFileName = selectedEditImageFileName
                                )
                            }
                        },
                        enabled = !state.isActionLoading
                    ) {
                        if (state.isActionLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        else Text("Lưu")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideEditDialog() }, enabled = !state.isActionLoading) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}

@Composable
private fun StateMessage(
    title: String,
    description: String?,
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

// Đã xóa bỏ MyPostHistoryCard vì sử dụng chung PostCard
