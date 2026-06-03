package com.foodfest.app.features.family.presentation.models

import com.foodfest.app.features.family.data.FamilyNote

/**
 * Screen state for Phase 5 family notes/chat MVP.
 */
data class FamilyNotesState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val input: String = "",
    val notes: List<FamilyNote> = emptyList()
)
