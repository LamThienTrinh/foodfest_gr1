package com.foodfest.app.features.family.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.family.data.FamilyRepository
import com.foodfest.app.features.family.presentation.models.FamilyNotesState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for Phase 5 family notes/chat MVP.
 */
class FamilyNotesViewModel(
    private val repository: FamilyRepository = FamilyRepository()
) {
    var state by mutableStateOf(FamilyNotesState())
        private set

    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Loads notes for a family; quiet mode is used by polling refresh.
     */
    fun loadNotes(familyId: Int, quiet: Boolean = false) {
        if (familyId <= 0 || (!quiet && state.isLoading)) return
        if (!quiet) {
            state = state.copy(isLoading = true, errorMessage = null)
        }

        scope.launch {
            repository.getFamilyNotes(familyId).fold(
                onSuccess = { notes ->
                    state = state.copy(isLoading = false, notes = notes, errorMessage = null)
                },
                onFailure = { error ->
                    if (!quiet) {
                        state = state.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Không tải được ghi chú gia đình"
                        )
                    }
                }
            )
        }
    }

    fun updateInput(value: String) {
        state = state.copy(input = value, errorMessage = null)
    }

    /**
     * Sends a short family note and reloads the thread after success.
     */
    fun sendNote(familyId: Int) {
        val message = state.input.trim()
        if (familyId <= 0 || state.isSending) return
        if (message.isBlank()) {
            state = state.copy(errorMessage = "Vui lòng nhập nội dung")
            return
        }
        if (message.length > 500) {
            state = state.copy(errorMessage = "Ghi chú tối đa 500 ký tự")
            return
        }

        state = state.copy(isSending = true, errorMessage = null)
        scope.launch {
            repository.createFamilyNote(familyId, message).fold(
                onSuccess = {
                    state = state.copy(isSending = false, input = "")
                    loadNotes(familyId, quiet = true)
                },
                onFailure = { error ->
                    state = state.copy(
                        isSending = false,
                        errorMessage = error.message ?: "Không gửi được ghi chú"
                    )
                }
            )
        }
    }
}
