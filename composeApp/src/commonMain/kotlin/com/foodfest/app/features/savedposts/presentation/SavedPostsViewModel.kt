package com.foodfest.app.features.savedposts.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.savedposts.data.SavedPostsRepository
import com.foodfest.app.features.savedposts.presentation.models.SavedPostsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SavedPostsViewModel(
    private val repository: SavedPostsRepository = SavedPostsRepository()
) {
    var state by mutableStateOf(SavedPostsState())
        private set
    
    private val scope = CoroutineScope(Dispatchers.Main)
    
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
    
    fun clearError() {
        state = state.copy(errorMessage = null)
    }
}
