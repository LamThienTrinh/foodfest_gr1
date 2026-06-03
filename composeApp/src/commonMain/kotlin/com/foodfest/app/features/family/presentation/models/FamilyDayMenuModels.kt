package com.foodfest.app.features.family.presentation.models

import com.foodfest.app.common.filter.DishFilter
import com.foodfest.app.features.dish.data.Dish
import com.foodfest.app.features.family.data.FamilyPantryItem
import com.foodfest.app.features.personaldish.data.PersonalDish

/**
 * UI model for a menu item inside a day menu.
 */
data class FamilyMenuItemUi(
    val id: Int,
    val title: String,
    val subtitle: String? = null,
    val dishId: Int? = null,
    val personalDishId: Int? = null
)

/**
 * Tabs shown in the menu item picker.
 */
enum class FamilyMenuPickerTab {
    SYSTEM,
    PERSONAL,
    RECENT
}

/**
 * Vote summary row for a menu item.
 */
data class FamilyVoteItemUi(
    val itemId: Int,
    val title: String,
    val subtitle: String? = null,
    val upVotes: Int = 0,
    val downVotes: Int = 0,
    val userVoteType: String? = null
)

/**
 * Rule-based pantry match summary for a dish card in the picker.
 */
data class DishPantryMatchUi(
    val availableCount: Int,
    val requiredCount: Int,
    val missingIngredients: List<String>,
    val statusText: String,
    val matchPercent: Int,
    val tone: DishPantryMatchTone
)

enum class DishPantryMatchTone {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Screen state for day menu details.
 */
data class FamilyDayMenuState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val items: List<FamilyMenuItemUi> = emptyList(),
    val showSavePresetDialog: Boolean = false,
    val presetNameInput: String = "",
    val isSavingPreset: Boolean = false,
    val savePresetError: String? = null,
    val savePresetSuccessMessage: String? = null,
    val showAddDialog: Boolean = false,
    val pickerTab: FamilyMenuPickerTab = FamilyMenuPickerTab.SYSTEM,
    val pickerSearchQuery: String = "",
    val isPickerLoading: Boolean = false,
    val pickerErrorMessage: String? = null,
    val systemDishes: List<Dish> = emptyList(),
    val personalDishes: List<PersonalDish> = emptyList(),
    val recentSystemDishes: List<Dish> = emptyList(),
    val recentPersonalDishes: List<PersonalDish> = emptyList(),
    val systemPage: Int = 1,
    val systemTotalPages: Int = 1,
    val systemTotalItems: Int = 0,
    val personalPage: Int = 1,
    val personalTotalPages: Int = 1,
    val personalTotalItems: Int = 0,
    val dishFilter: DishFilter = DishFilter(),
    val showFilterSheet: Boolean = false,
    val noteInput: String = "",
    val isAddingItem: Boolean = false,
    val addErrorMessage: String? = null,
    val showVoteSheet: Boolean = false,
    val isVoteLoading: Boolean = false,
    val voteErrorMessage: String? = null,
    val voteItems: List<FamilyVoteItemUi> = emptyList(),
    val pantryItems: List<FamilyPantryItem> = emptyList(),
    val pantryErrorMessage: String? = null,
    val systemDishMatches: Map<Int, DishPantryMatchUi> = emptyMap(),
    val personalDishMatches: Map<Int, DishPantryMatchUi> = emptyMap()
)
