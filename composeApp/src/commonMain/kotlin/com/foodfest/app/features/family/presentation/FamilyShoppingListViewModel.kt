package com.foodfest.app.features.family.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.family.data.FamilyRepository
import com.foodfest.app.features.family.data.FamilyShoppingListDetail
import com.foodfest.app.features.family.presentation.models.FamilyShoppingListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for shopping list checklist detail.
 */
class FamilyShoppingListViewModel(
    private val repository: FamilyRepository = FamilyRepository()
) {
    var state by mutableStateOf(FamilyShoppingListState())
        private set

    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Phase 4.4: Loads shopping checklist detail with activity log.
     */
    fun loadShoppingList(familyId: Int, shoppingListId: Int, quiet: Boolean = false) {
        if (familyId <= 0 || shoppingListId <= 0 || (!quiet && state.isLoading)) return
        if (!quiet) {
            state = state.copy(isLoading = true, errorMessage = null)
        }

        scope.launch {
            repository.getShoppingListDetail(familyId, shoppingListId).fold(
                onSuccess = { detail ->
                    applyDetail(detail, isLoading = false)
                },
                onFailure = { error ->
                    if (!quiet) {
                        state = state.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Không tải được shopping list"
                        )
                    }
                }
            )
        }
    }

    /**
     * Phase 4.4: Optimistically sends purchased checkbox changes.
     */
    fun togglePurchased(familyId: Int, shoppingListId: Int, itemId: Int, isPurchased: Boolean) {
        if (state.isSaving) return
        state = state.copy(isSaving = true, errorMessage = null)
        scope.launch {
            repository.updateShoppingListItem(
                familyId = familyId,
                shoppingListId = shoppingListId,
                itemId = itemId,
                isPurchased = isPurchased
            ).fold(
                onSuccess = { detail ->
                    applyDetail(detail, isLoading = false)
                    state = state.copy(isSaving = false, actionMessage = "Đã cập nhật checklist")
                },
                onFailure = { error ->
                    state = state.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Không cập nhật được checklist"
                    )
                }
            )
        }
    }

    /**
     * Assigns a shopping item to the current user.
     */
    fun assignToMe(familyId: Int, shoppingListId: Int, itemId: Int, currentUserId: Int?) {
        val userId = currentUserId ?: return
        if (state.isSaving) return
        state = state.copy(isSaving = true, errorMessage = null)
        scope.launch {
            repository.updateShoppingListItem(
                familyId = familyId,
                shoppingListId = shoppingListId,
                itemId = itemId,
                assignedToUserId = userId
            ).fold(
                onSuccess = { detail ->
                    applyDetail(detail, isLoading = false)
                    state = state.copy(isSaving = false, actionMessage = "Đã nhận phụ trách item")
                },
                onFailure = { error ->
                    state = state.copy(isSaving = false, errorMessage = error.message ?: "Không phân công được")
                }
            )
        }
    }

    fun updateUsedQtyDraft(itemId: Int, value: String) {
        state = state.copy(
            usedQtyDrafts = state.usedQtyDrafts + (itemId to value),
            editedUsedQtyItemIds = state.editedUsedQtyItemIds + itemId,
            errorMessage = null
        )
    }

    /**
     * Phase 4.4: Saves "đã dùng bao nhiêu" for Pantry deduction prep.
     */
    fun saveUsedQty(familyId: Int, shoppingListId: Int, itemId: Int) {
        if (state.isSaving) return
        val quantity = state.usedQtyDrafts[itemId]?.trim()?.toDoubleOrNull()
        if (quantity == null || quantity < 0.0) {
            state = state.copy(errorMessage = "Số lượng đã dùng phải là số >= 0")
            return
        }

        state = state.copy(isSaving = true, errorMessage = null)
        scope.launch {
            repository.updateShoppingListItem(
                familyId = familyId,
                shoppingListId = shoppingListId,
                itemId = itemId,
                usedQty = quantity
            ).fold(
                onSuccess = { detail ->
                    state = state.copy(editedUsedQtyItemIds = state.editedUsedQtyItemIds - itemId)
                    applyDetail(detail, isLoading = false)
                    state = state.copy(isSaving = false, actionMessage = "Đã lưu số lượng đã dùng")
                },
                onFailure = { error ->
                    state = state.copy(isSaving = false, errorMessage = error.message ?: "Không lưu được số lượng")
                }
            )
        }
    }

    /**
     * Phase 4.3: Marks all items as purchased in one action.
     */
    fun markAllPurchased(familyId: Int, shoppingListId: Int) {
        if (state.isSaving) return
        state = state.copy(isSaving = true, errorMessage = null)
        scope.launch {
            repository.markAllShoppingItemsPurchased(familyId, shoppingListId).fold(
                onSuccess = { detail ->
                    applyDetail(detail, isLoading = false)
                    state = state.copy(isSaving = false, actionMessage = "Đã đánh dấu mua hết")
                },
                onFailure = { error ->
                    state = state.copy(isSaving = false, errorMessage = error.message ?: "Không đánh dấu được")
                }
            )
        }
    }

    fun showSyncPantryDialog(show: Boolean) {
        state = state.copy(showSyncPantryDialog = show)
    }

    /**
     * Phase 4.3: Adds purchased items into Pantry using required quantities.
     */
    fun syncPantry(familyId: Int, shoppingListId: Int) {
        if (state.isSaving) return
        state = state.copy(isSaving = true, errorMessage = null, showSyncPantryDialog = false)
        scope.launch {
            repository.syncShoppingListToPantry(familyId, shoppingListId).fold(
                onSuccess = { result ->
                    state = state.copy(
                        isSaving = false,
                        actionMessage = "Đã cập nhật ${result.updatedCount} item vào Pantry"
                    )
                    loadShoppingList(familyId, shoppingListId, quiet = true)
                },
                onFailure = { error ->
                    state = state.copy(isSaving = false, errorMessage = error.message ?: "Không cập nhật Pantry được")
                }
            )
        }
    }

    fun shareListMessage() {
        val total = state.items.size
        val remaining = state.items.count { !it.isPurchased }
        state = state.copy(actionMessage = "Shopping list có $total item, còn $remaining item chưa mua")
    }

    private fun applyDetail(detail: FamilyShoppingListDetail, isLoading: Boolean) {
        // Phase 5 quiet sync: accept remote checklist updates but keep any local "used qty" draft being edited.
        val drafts = detail.items.associate { item ->
            val hasLocalEdit = item.id in state.editedUsedQtyItemIds
            item.id to if (hasLocalEdit) {
                state.usedQtyDrafts[item.id].orEmpty()
            } else {
                item.usedQty?.toString().orEmpty()
            }
        }
        state = state.copy(
            isLoading = isLoading,
            errorMessage = null,
            listTitle = "Tuần ${detail.shoppingList.menuWeek}",
            status = detail.shoppingList.status,
            items = detail.items,
            activityLog = detail.activityLog,
            usedQtyDrafts = drafts
        )
    }
}
