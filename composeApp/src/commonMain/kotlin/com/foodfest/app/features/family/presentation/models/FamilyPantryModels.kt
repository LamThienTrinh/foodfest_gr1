package com.foodfest.app.features.family.presentation.models

import com.foodfest.app.features.family.data.FamilyPantryItem

/**
 * Form state for creating a pantry item.
 */
data class PantryFormState(
    val ingredientName: String = "",
    val quantity: String = "",
    val unit: String = "",
    val expiryDate: String = ""
)

/**
 * Editable row draft for bulk edit mode.
 */
data class PantryBulkEditDraft(
    val itemId: Int,
    val ingredientName: String,
    val quantity: String,
    val unit: String,
    val expiryDate: String
)

/**
 * Screen state for family pantry management.
 */
data class FamilyPantryState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val items: List<FamilyPantryItem> = emptyList(),
    val searchQuery: String = "",
    val showAddDialog: Boolean = false,
    val addForm: PantryFormState = PantryFormState(),
    val addFormError: String? = null,
    val isSubmittingAdd: Boolean = false,
    val isBulkEditMode: Boolean = false,
    val bulkDrafts: Map<Int, PantryBulkEditDraft> = emptyMap(),
    val bulkEditError: String? = null,
    val isSavingBulk: Boolean = false,
    val isDeletingExpired: Boolean = false
)
