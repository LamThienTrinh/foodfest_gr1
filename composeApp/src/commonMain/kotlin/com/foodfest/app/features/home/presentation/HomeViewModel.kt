package com.foodfest.app.features.home.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.home.data.Post
import com.foodfest.app.features.home.data.PostRepository
import com.foodfest.app.features.home.presentation.models.HomeState

class HomeViewModel {
    private val postRepository = PostRepository()
    
    var state by mutableStateOf(HomeState())
        private set
    
    suspend fun loadPosts(refresh: Boolean = false) {
        if (state.isLoading) return
        
        val page = if (refresh) 1 else state.currentPage
        
        state = state.copy(
            isLoading = !refresh,
            isRefreshing = refresh,
            errorMessage = null
        )
        
        val result = postRepository.getPosts(
            page = page,
            search = state.searchQuery.takeIf { it.isNotBlank() },
            postType = state.selectedPostType
        )
        
        println("DEBUG: loadPosts called - search='${state.searchQuery}', postType='${state.selectedPostType}'")
        
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
    
    suspend fun searchPosts() {
        state = state.copy(currentPage = 1, posts = emptyList())
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
    
    fun clearError() {
        state = state.copy(errorMessage = null)
    }
}
