package com.foodfest.app.features.family.presentation.models

import com.foodfest.app.features.family.data.FamilyShoppingListActivity
import com.foodfest.app.features.family.data.FamilyShoppingListItem

/**
 * Screen state for Phase 4.3 shopping list and Phase 4.4 checklist detail.
 */
data class FamilyShoppingListState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val listTitle: String = "",
    val status: String = "",
    val items: List<FamilyShoppingListItem> = emptyList(),
    val activityLog: List<FamilyShoppingListActivity> = emptyList(),
    val usedQtyDrafts: Map<Int, String> = emptyMap(),
    val editedUsedQtyItemIds: Set<Int> = emptySet(),
    val showSyncPantryDialog: Boolean = false
)
