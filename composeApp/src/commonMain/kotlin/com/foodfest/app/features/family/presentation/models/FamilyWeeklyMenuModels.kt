package com.foodfest.app.features.family.presentation.models

import kotlinx.datetime.LocalDate

/**
 * Slot data for a specific meal type in weekly menu.
 */
data class FamilyMenuSlotUi(
    val mealType: String,
    val mealLabel: String,
    val menuId: Int? = null,
    val itemsCount: Int = 0,
    val status: String? = null
)

/**
 * Day row containing menu slots.
 */
data class FamilyMenuDayUi(
    val date: LocalDate,
    val dayLabel: String,
    val slots: List<FamilyMenuSlotUi>
)

/**
 * Screen state for weekly menu view.
 */
data class FamilyWeeklyMenuState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val weekStart: LocalDate? = null,
    val days: List<FamilyMenuDayUi> = emptyList(),
    val isCreatingMenu: Boolean = false,
    val isGeneratingShoppingList: Boolean = false,
    val shoppingListError: String? = null
)
