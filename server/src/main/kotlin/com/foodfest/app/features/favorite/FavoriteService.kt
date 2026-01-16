package com.foodfest.app.features.favorite

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedFavorites(
    val data: List<FavoriteDish>,
    val page: Int,
    val limit: Int,
    val total: Int
)

class FavoriteService(
    private val repository: FavoriteRepository = FavoriteRepository()
) {
    suspend fun addFavorite(userId: Int, dishId: Int): Result<Boolean> = runCatching {
        repository.addFavorite(userId, dishId)
    }
    
    suspend fun removeFavorite(userId: Int, dishId: Int): Result<Boolean> = runCatching {
        repository.removeFavorite(userId, dishId)
    }
    
    suspend fun toggleFavorite(userId: Int, dishId: Int): Result<Boolean> = runCatching {
        val isFav = repository.isFavorite(userId, dishId)
        if (isFav) {
            repository.removeFavorite(userId, dishId)
            false // Now not favorited
        } else {
            repository.addFavorite(userId, dishId)
            true // Now favorited
        }
    }
    
    suspend fun isFavorite(userId: Int, dishId: Int): Result<Boolean> = runCatching {
        repository.isFavorite(userId, dishId)
    }
    
    suspend fun getFavorites(userId: Int, page: Int): Result<PaginatedFavorites> = runCatching {
        val limit = 20
        val (favorites, total) = repository.getFavorites(userId, page, limit)
        PaginatedFavorites(
            data = favorites,
            page = page,
            limit = limit,
            total = total
        )
    }
    
    suspend fun getFavoriteIds(userId: Int): Result<List<Int>> = runCatching {
        repository.getFavoriteIds(userId)
    }
}
