package com.foodfest.app.features.family.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.family.data.FamilyPantryItem
import com.foodfest.app.features.family.data.FamilyRepository
import com.foodfest.app.features.family.presentation.models.FamilyPantryState
import com.foodfest.app.features.family.presentation.models.PantryBulkEditDraft
import com.foodfest.app.features.family.presentation.models.PantryFormState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for family pantry management.
 */
class FamilyPantryViewModel(
    private val repository: FamilyRepository = FamilyRepository()
) {
    var state by mutableStateOf(FamilyPantryState())
        private set

    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Phase 4.1: Loads pantry list for the selected family.
     */
    fun loadPantry(familyId: Int) {
        if (familyId <= 0 || state.isLoading) return
        state = state.copy(isLoading = true, errorMessage = null)

        scope.launch {
            repository.getPantryItems(familyId).fold(
                onSuccess = { items ->
                    state = state.copy(
                        isLoading = false,
                        items = items
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Không tải được pantry"
                    )
                }
            )
        }
    }

    fun updateSearchQuery(value: String) {
        state = state.copy(searchQuery = value)
    }

    fun showAddDialog() {
        state = state.copy(
            showAddDialog = true,
            addForm = PantryFormState(),
            addFormError = null
        )
    }

    fun hideAddDialog() {
        state = state.copy(
            showAddDialog = false,
            addForm = PantryFormState(),
            addFormError = null
        )
    }

    fun updateAddIngredientName(value: String) {
        state = state.copy(addForm = state.addForm.copy(ingredientName = value), addFormError = null)
    }

    fun updateAddQuantity(value: String) {
        state = state.copy(addForm = state.addForm.copy(quantity = value), addFormError = null)
    }

    fun updateAddUnit(value: String) {
        state = state.copy(addForm = state.addForm.copy(unit = value), addFormError = null)
    }

    fun updateAddExpiryDate(value: String) {
        state = state.copy(addForm = state.addForm.copy(expiryDate = value), addFormError = null)
    }

    /**
     * Phase 4.1: Creates a pantry item from form input.
     */
    fun submitAddItem(familyId: Int) {
        if (familyId <= 0 || state.isSubmittingAdd) return
        val form = state.addForm
        val name = form.ingredientName.trim()
        val quantity = form.quantity.trim().toDoubleOrNull()
        val unit = form.unit.trim().ifBlank { null }
        val expiryDate = form.expiryDate.trim().ifBlank { null }

        when {
            name.isBlank() -> {
                state = state.copy(addFormError = "Tên nguyên liệu không được để trống")
                return
            }
            quantity == null || quantity <= 0.0 -> {
                state = state.copy(addFormError = "Số lượng phải là số > 0")
                return
            }
            expiryDate != null && !isIsoDate(expiryDate) -> {
                state = state.copy(addFormError = "Ngày hết hạn phải theo định dạng YYYY-MM-DD")
                return
            }
        }

        state = state.copy(isSubmittingAdd = true, addFormError = null)
        scope.launch {
            repository.createPantryItem(
                familyId = familyId,
                ingredientName = name,
                quantity = quantity,
                unit = unit,
                expiryDate = expiryDate
            ).fold(
                onSuccess = {
                    state = state.copy(
                        isSubmittingAdd = false,
                        showAddDialog = false,
                        actionMessage = "Đã thêm nguyên liệu"
                    )
                    loadPantry(familyId)
                },
                onFailure = { error ->
                    state = state.copy(
                        isSubmittingAdd = false,
                        addFormError = error.message ?: "Không thể thêm nguyên liệu"
                    )
                }
            )
        }
    }

    /**
     * Phase 4.1: Toggles bulk edit mode and prepares row drafts.
     */
    fun toggleBulkEditMode() {
        if (state.isSavingBulk) return
        if (state.isBulkEditMode) {
            state = state.copy(isBulkEditMode = false, bulkDrafts = emptyMap(), bulkEditError = null)
        } else {
            val drafts = state.items.associate { item ->
                item.id to item.toBulkDraft()
            }
            state = state.copy(isBulkEditMode = true, bulkDrafts = drafts, bulkEditError = null)
        }
    }

    fun updateBulkIngredientName(itemId: Int, value: String) {
        val draft = state.bulkDrafts[itemId] ?: return
        state = state.copy(
            bulkDrafts = state.bulkDrafts + (itemId to draft.copy(ingredientName = value)),
            bulkEditError = null
        )
    }

    fun updateBulkQuantity(itemId: Int, value: String) {
        val draft = state.bulkDrafts[itemId] ?: return
        state = state.copy(
            bulkDrafts = state.bulkDrafts + (itemId to draft.copy(quantity = value)),
            bulkEditError = null
        )
    }

    fun updateBulkUnit(itemId: Int, value: String) {
        val draft = state.bulkDrafts[itemId] ?: return
        state = state.copy(
            bulkDrafts = state.bulkDrafts + (itemId to draft.copy(unit = value)),
            bulkEditError = null
        )
    }

    fun updateBulkExpiryDate(itemId: Int, value: String) {
        val draft = state.bulkDrafts[itemId] ?: return
        state = state.copy(
            bulkDrafts = state.bulkDrafts + (itemId to draft.copy(expiryDate = value)),
            bulkEditError = null
        )
    }

    /**
     * Phase 4.1: Saves all modified pantry rows in bulk edit mode.
     */
    fun saveBulkEdits(familyId: Int) {
        if (familyId <= 0 || !state.isBulkEditMode || state.isSavingBulk) return
        val itemMap = state.items.associateBy { it.id }
        val drafts = state.bulkDrafts

        for ((itemId, draft) in drafts) {
            val source = itemMap[itemId] ?: continue
            val name = draft.ingredientName.trim()
            val quantity = draft.quantity.trim().toDoubleOrNull()
            val expiry = draft.expiryDate.trim()
            if (name.isBlank()) {
                state = state.copy(bulkEditError = "Tên nguyên liệu không được để trống")
                return
            }
            if (quantity == null || quantity <= 0.0) {
                state = state.copy(bulkEditError = "Số lượng phải là số > 0")
                return
            }
            if (expiry.isNotBlank() && !isIsoDate(expiry)) {
                state = state.copy(bulkEditError = "Ngày hết hạn phải theo định dạng YYYY-MM-DD")
                return
            }
            // Prevent expensive no-op calls when a row has no changes.
            if (
                source.ingredientName == name &&
                source.quantity == quantity &&
                (source.unit ?: "") == draft.unit.trim() &&
                (source.expiryDate ?: "") == expiry
            ) {
                continue
            }
        }

        state = state.copy(isSavingBulk = true, bulkEditError = null)
        scope.launch {
            val sourceById = state.items.associateBy { it.id }
            var updatedCount = 0
            var failureMessage: String? = null

            for ((itemId, draft) in state.bulkDrafts) {
                val source = sourceById[itemId] ?: continue
                val name = draft.ingredientName.trim()
                val quantity = draft.quantity.trim().toDoubleOrNull() ?: continue
                val unit = draft.unit.trim().ifBlank { null }
                val expiry = draft.expiryDate.trim().ifBlank { null }
                if (
                    source.ingredientName == name &&
                    source.quantity == quantity &&
                    source.unit == unit &&
                    source.expiryDate == expiry
                ) {
                    continue
                }

                val result = repository.updatePantryItem(
                    familyId = familyId,
                    itemId = itemId,
                    ingredientName = name,
                    quantity = quantity,
                    unit = unit,
                    expiryDate = expiry
                )
                if (result.isSuccess) {
                    updatedCount += 1
                } else {
                    failureMessage = result.exceptionOrNull()?.message ?: "Không thể cập nhật pantry"
                    break
                }
            }

            if (failureMessage != null) {
                state = state.copy(isSavingBulk = false, bulkEditError = failureMessage)
                return@launch
            }

            state = state.copy(
                isSavingBulk = false,
                isBulkEditMode = false,
                bulkDrafts = emptyMap(),
                actionMessage = if (updatedCount > 0) "Đã lưu $updatedCount thay đổi" else "Không có thay đổi"
            )
            loadPantry(familyId)
        }
    }

    /**
     * Phase 4.1: Deletes expired items using backend quick action.
     */
    fun deleteExpiredItems(familyId: Int) {
        if (familyId <= 0 || state.isDeletingExpired) return
        state = state.copy(isDeletingExpired = true, errorMessage = null)

        scope.launch {
            repository.deleteExpiredPantryItems(familyId).fold(
                onSuccess = { deletedCount ->
                    state = state.copy(
                        isDeletingExpired = false,
                        actionMessage = if (deletedCount > 0) {
                            "Đã xóa $deletedCount nguyên liệu hết hạn"
                        } else {
                            "Không có nguyên liệu hết hạn"
                        }
                    )
                    loadPantry(familyId)
                },
                onFailure = { error ->
                    state = state.copy(
                        isDeletingExpired = false,
                        errorMessage = error.message ?: "Không thể xóa nguyên liệu hết hạn"
                    )
                }
            )
        }
    }

    fun filteredItems(): List<FamilyPantryItem> {
        val query = state.searchQuery.trim()
        if (query.isBlank()) return state.items
        return state.items.filter { item ->
            item.ingredientName.contains(query, ignoreCase = true) ||
                (item.unit?.contains(query, ignoreCase = true) == true) ||
                (item.expiryDate?.contains(query, ignoreCase = true) == true)
        }
    }

    private fun FamilyPantryItem.toBulkDraft(): PantryBulkEditDraft {
        return PantryBulkEditDraft(
            itemId = id,
            ingredientName = ingredientName,
            quantity = quantity.toString(),
            unit = unit.orEmpty(),
            expiryDate = expiryDate.orEmpty()
        )
    }

    private fun isIsoDate(value: String): Boolean {
        return Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(value)
    }
}
