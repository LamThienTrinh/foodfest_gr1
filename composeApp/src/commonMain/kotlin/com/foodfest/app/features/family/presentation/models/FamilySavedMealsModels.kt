package com.foodfest.app.features.family.presentation.models

/**
 * UI model for a saved meal preset card.
 */
data class FamilySavedMealCardUi(
    val id: Int,
    val name: String,
    val itemsCount: Int,
    val createdByName: String? = null,
    val createdAtLabel: String? = null
)

/**
 * UI model for a saved meal item row.
 */
data class FamilySavedMealItemUi(
    val title: String,
    val subtitle: String? = null
)

/**
 * UI model for saved meal detail.
 */
data class FamilySavedMealDetailUi(
    val id: Int,
    val name: String,
    val items: List<FamilySavedMealItemUi>
)

/**
 * State container for saved meals screen.
 */
data class FamilySavedMealsState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val meals: List<FamilySavedMealCardUi> = emptyList(),
    val showDetailDialog: Boolean = false,
    val isDetailLoading: Boolean = false,
    val detailErrorMessage: String? = null,
    val detail: FamilySavedMealDetailUi? = null,
    val showDeleteDialog: Boolean = false,
    val deleteTargetId: Int? = null,
    val isDeleting: Boolean = false,
    val deleteErrorMessage: String? = null,
    val isApplying: Boolean = false,
    val applyErrorMessage: String? = null,
    val applySuccessMessage: String? = null
)
