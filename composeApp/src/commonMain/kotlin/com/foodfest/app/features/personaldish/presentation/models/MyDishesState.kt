package com.foodfest.app.features.personaldish.presentation.models

import com.foodfest.app.common.filter.DishFilter
import com.foodfest.app.features.personaldish.data.PersonalDish

data class MyDishesState(
    val dishes: List<PersonalDish> = emptyList(),
    val filteredDishes: List<PersonalDish> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalItems: Int = 0,
    val searchQuery: String = "",
    val dishFilter: DishFilter = DishFilter(),
    val selectedDish: PersonalDish? = null,
    val showFilterSheet: Boolean = false
)
