package com.foodfest.app.features.home.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.home.data.CreatePostRequest
import com.foodfest.app.features.home.data.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class CreatePostState(
    val title: String = "",
    val content: String = "",
    val postType: String = "share",
    val isLoading: Boolean = false,
    val message: String? = null,
    val isSuccess: Boolean = false
)

class CreatePostViewModel {
    private val postRepository = PostRepository()
    
    var state by mutableStateOf(CreatePostState())
        private set
    
    fun updateTitle(title: String) {
        state = state.copy(title = title)
    }
    
    fun updateContent(content: String) {
        state = state.copy(content = content)
    }
    
    fun updatePostType(postType: String) {
        state = state.copy(postType = postType)
    }
    
    fun clearMessage() {
        state = state.copy(message = null)
    }
    
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun createPost(imageBytes: ByteArray?, fileName: String?) {
        // Validate
        if (state.content.isBlank() && imageBytes == null) {
            state = state.copy(message = "❌ Vui lòng nhập nội dung hoặc chọn ảnh")
            return
        }
        
        state = state.copy(isLoading = true, message = null)
        
        try {
            var imageUrl: String? = null
            
            // Upload ảnh lên Cloudinary nếu có
            if (imageBytes != null) {
                state = state.copy(message = "Đang upload ảnh...")
                
                val uploadResult = postRepository.uploadImage(
                    imageBytes = imageBytes,
                    fileName = fileName ?: "post_${System.currentTimeMillis()}.jpg"
                )
                
                uploadResult.fold(
                    onSuccess = { url ->
                        imageUrl = url
                    },
                    onFailure = { error ->
                        state = state.copy(
                            isLoading = false,
                            message = "❌ Upload ảnh thất bại: ${error.message}"
                        )
                        return
                    }
                )
            }
            
            // Tạo bài viết
            state = state.copy(message = "Đang đăng bài...")
            
            val request = CreatePostRequest(
                postType = state.postType,
                title = state.title.takeIf { it.isNotBlank() },
                content = state.content.takeIf { it.isNotBlank() },
                imageUrl = imageUrl
            )
            
            val result = postRepository.createPost(request)
            
            result.fold(
                onSuccess = { post ->
                    state = state.copy(
                        isLoading = false,
                        message = "✅ Đăng bài thành công!",
                        isSuccess = true
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        isLoading = false,
                        message = "❌ Đăng bài thất bại: ${error.message}"
                    )
                }
            )
        } catch (e: Exception) {
            state = state.copy(
                isLoading = false,
                message = "❌ Lỗi: ${e.message}"
            )
        }
    }
}
