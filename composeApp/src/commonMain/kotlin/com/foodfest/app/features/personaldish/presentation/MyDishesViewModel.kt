package com.foodfest.app.features.personaldish.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.common.filter.DishFilter
import com.foodfest.app.common.filter.FilterCategory
import com.foodfest.app.common.filter.toFilterTag
import com.foodfest.app.features.personaldish.data.PersonalDish
import com.foodfest.app.features.personaldish.data.PersonalDishRepository
import com.foodfest.app.features.personaldish.presentation.models.MyDishesState
import com.foodfest.app.features.tag.data.TagRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyDishesViewModel(
    private val repository: PersonalDishRepository = PersonalDishRepository(),
    private val tagRepository: TagRepository = TagRepository()
) {
    var state by mutableStateOf(MyDishesState())
        private set
    
    private val scope = CoroutineScope(Dispatchers.Main)
    
    fun loadDishes(page: Int = 1) {
        if (state.isLoading) return
        
        state = state.copy(
            isLoading = true,
            errorMessage = null
        )
        
        scope.launch {
            repository.getMyDishes(page).fold(
                onSuccess = { response ->
                    val totalPages = (response.total + response.limit - 1) / response.limit
                    state = state.copy(
                        dishes = response.data,
                        isLoading = false,
                        currentPage = response.page,
                        totalPages = totalPages,
                        totalItems = response.total
                    )
                    applyFilters()
                },
                onFailure = { error ->
                    state = state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Không tải được danh sách món"
                    )
                }
            )
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
    
    fun selectDish(dish: PersonalDish?) {
        state = state.copy(selectedDish = dish)
    }
    
    fun refreshAfterDetailClose() {
        state = state.copy(selectedDish = null)
        loadDishes(state.currentPage)
    }
    
    fun onDishDeleted() {
        state = state.copy(selectedDish = null)
        loadDishes(state.currentPage)
    }
    
    fun goToPreviousPage() {
        if (state.currentPage > 1) {
            loadDishes(state.currentPage - 1)
        }
    }
    
    fun goToNextPage() {
        if (state.currentPage < state.totalPages) {
            loadDishes(state.currentPage + 1)
        }
    }
    
    fun clearError() {
        state = state.copy(errorMessage = null)
    }
    
    private fun applyFilters() {
        var result = state.dishes
        
        // Search filter
        if (state.searchQuery.isNotBlank()) {
            result = result.filter { dish ->
                dish.dishName.contains(state.searchQuery, ignoreCase = true) ||
                dish.description?.contains(state.searchQuery, ignoreCase = true) == true
            }
        }
        
        // Tag filters
        if (state.dishFilter.hasSelectedTags) {
            val selectedTypes = state.dishFilter.getSelectedTypeTags()
            val selectedTastes = state.dishFilter.getSelectedTasteTags()
            val selectedIngredients = state.dishFilter.getSelectedIngredientTags()
            
            result = result.filter { dish ->
                val dishTags = dish.tags
                
                val matchesType = selectedTypes.isEmpty() || selectedTypes.any { it in dishTags }
                val matchesTaste = selectedTastes.isEmpty() || selectedTastes.any { it in dishTags }
                val matchesIngredient = selectedIngredients.isEmpty() || selectedIngredients.any { it in dishTags }
                
                matchesType && matchesTaste && matchesIngredient
            }
        }
        
        state = state.copy(filteredDishes = result)
    }
}
