package com.foodfest.app.features.favorite.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.common.filter.DishFilter
import com.foodfest.app.common.filter.FilterCategory
import com.foodfest.app.common.filter.toFilterTag
import com.foodfest.app.features.dish.data.Dish
import com.foodfest.app.features.dish.data.DishRepository
import com.foodfest.app.features.favorite.data.FavoriteRepository
import com.foodfest.app.features.favorite.presentation.models.FavoriteDishesState
import com.foodfest.app.features.tag.data.TagRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoriteDishesViewModel(
    private val favoriteRepository: FavoriteRepository = FavoriteRepository(),
    private val dishRepository: DishRepository = DishRepository(),
    private val tagRepository: TagRepository = TagRepository()
) {
    var state by mutableStateOf(FavoriteDishesState())
        private set
    
    private val scope = CoroutineScope(Dispatchers.Main)
    
    fun loadFavorites(page: Int = 1) {
        if (state.isLoading) return
        
        state = state.copy(
            isLoading = true,
            errorMessage = null
        )
        
        scope.launch {
            favoriteRepository.getFavorites(page).fold(
                onSuccess = { response ->
                    val totalPages = if (response.limit > 0) {
                        (response.total + response.limit - 1) / response.limit
                    } else 1
                    
                    state = state.copy(
                        favorites = response.data,
                        isLoading = false,
                        currentPage = response.page,
                        totalPages = totalPages,
                        totalItems = response.total
                    )
                    
                    // Load full dish data for filtering
                    loadFullDishData(response.data.map { it.dishId })
                    applyFilters()
                },
                onFailure = { error ->
                    state = state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Không tải được danh sách yêu thích"
                    )
                }
            )
        }
    }
    
    private fun loadFullDishData(dishIds: List<Int>) {
        scope.launch {
            val dishMap = mutableMapOf<Int, Dish>()
            dishIds.forEach { dishId ->
                dishRepository.getDishById(dishId).onSuccess { dish ->
                    dishMap[dishId] = dish
                }
            }
            state = state.copy(fullDishes = dishMap)
            applyFilters()
        }
    }
    
    fun loadTags() {
        scope.launch {
            tagRepository.getAllTags().fold(
                onSuccess = { tags ->
                    val typeTags = tags
                        .filter { it.type.uppercase() == "TYPE" }
                        .map { it.toFilterTag(FilterCategory.TYPE) }
                    val tasteTags = tags
                        .filter { it.type.uppercase() == "TASTE" }
                        .map { it.toFilterTag(FilterCategory.TASTE) }
                    val ingredientTags = tags
                        .filter { it.type.uppercase() == "INGREDIENT" }
                        .map { it.toFilterTag(FilterCategory.INGREDIENT) }
                    
                    state = state.copy(
                        dishFilter = DishFilter(
                            typeTags = typeTags,
                            tasteTags = tasteTags,
                            ingredientTags = ingredientTags
                        )
                    )
                },
                onFailure = { /* Ignore tag loading errors */ }
            )
        }
    }
    
    fun updateSearchQuery(query: String) {
        state = state.copy(searchQuery = query)
        applyFilters()
    }
    
    fun updateFilter(filter: DishFilter) {
        state = state.copy(dishFilter = filter)
        applyFilters()
    }
    
    fun showFilterSheet(show: Boolean) {
        state = state.copy(showFilterSheet = show)
    }
    
    fun selectDish(dishId: Int?) {
        state = state.copy(selectedDishId = dishId)
    }
    
    fun goToPreviousPage() {
        if (state.currentPage > 1) {
            loadFavorites(state.currentPage - 1)
        }
    }
    
    fun goToNextPage() {
        if (state.currentPage < state.totalPages) {
            loadFavorites(state.currentPage + 1)
        }
    }
    
    fun clearError() {
        state = state.copy(errorMessage = null)
    }
    
    private fun applyFilters() {
        var result = state.favorites
        
        // Search filter
        if (state.searchQuery.isNotBlank()) {
            result = result.filter { fav ->
                fav.dishName.contains(state.searchQuery, ignoreCase = true)
            }
        }
        
        // Tag filters - only if we have dish data
        if (state.dishFilter.hasSelectedTags && state.fullDishes.isNotEmpty()) {
            val selectedTypes = state.dishFilter.getSelectedTypeTags()
            val selectedTastes = state.dishFilter.getSelectedTasteTags()
            val selectedIngredients = state.dishFilter.getSelectedIngredientTags()
            
            result = result.filter { fav ->
                val dish = state.fullDishes[fav.dishId] ?: return@filter true
                val dishTags = dish.tags?.map { it.name } ?: emptyList()
                
                val matchesType = selectedTypes.isEmpty() || selectedTypes.any { it in dishTags }
                val matchesTaste = selectedTastes.isEmpty() || selectedTastes.any { it in dishTags }
                val matchesIngredient = selectedIngredients.isEmpty() || selectedIngredients.any { it in dishTags }
                
                matchesType && matchesTaste && matchesIngredient
            }
        }
        
        state = state.copy(filteredFavorites = result)
    }
}
