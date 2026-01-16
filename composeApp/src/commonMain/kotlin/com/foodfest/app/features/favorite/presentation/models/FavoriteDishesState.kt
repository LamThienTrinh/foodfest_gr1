package com.foodfest.app.features.favorite.presentation.models

import com.foodfest.app.common.filter.DishFilter
import com.foodfest.app.features.dish.data.Dish
import com.foodfest.app.features.favorite.data.FavoriteDish

data class FavoriteDishesState(
    val favorites: List<FavoriteDish> = emptyList(),
    val filteredFavorites: List<FavoriteDish> = emptyList(),
    val fullDishes: Map<Int, Dish> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalItems: Int = 0,
    val searchQuery: String = "",
    val dishFilter: DishFilter = DishFilter(),
    val selectedDishId: Int? = null,
    val showFilterSheet: Boolean = false
)
