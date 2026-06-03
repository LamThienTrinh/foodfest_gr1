package com.foodfest.app.features.family.presentation.models

import com.foodfest.app.features.family.data.FamilyRecentVote

/**
 * State for the Family Home "Vote gần đây" history screen.
 */
data class FamilyRecentVotesState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val votes: List<FamilyRecentVote> = emptyList()
)
