package com.foodfest.app.features.family.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.family.data.FamilyRepository
import com.foodfest.app.features.family.presentation.models.FamilyRecentVotesState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for recent family vote history.
 */
class FamilyRecentVotesViewModel(
    private val repository: FamilyRepository = FamilyRepository()
) {
    var state by mutableStateOf(FamilyRecentVotesState())
        private set

    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Loads newest vote events for the selected family.
     */
    fun loadVotes(familyId: Int) {
        if (familyId <= 0 || state.isLoading) return

        state = state.copy(isLoading = true, errorMessage = null)
        scope.launch {
            repository.getRecentVotes(familyId).fold(
                onSuccess = { votes ->
                    state = state.copy(isLoading = false, votes = votes)
                },
                onFailure = { error ->
                    state = state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Không tải được vote gần đây"
                    )
                }
            )
        }
    }
}
