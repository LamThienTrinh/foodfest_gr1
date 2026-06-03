package com.foodfest.app.features.family.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.dish.data.DishRepository
import com.foodfest.app.features.family.data.FamilyRepository
import com.foodfest.app.features.family.data.FamilySavedMealDetail
import com.foodfest.app.features.family.data.FamilySavedMealItem
import com.foodfest.app.features.family.data.FamilySavedMealSummary
import com.foodfest.app.features.family.presentation.models.FamilySavedMealCardUi
import com.foodfest.app.features.family.presentation.models.FamilySavedMealDetailUi
import com.foodfest.app.features.family.presentation.models.FamilySavedMealItemUi
import com.foodfest.app.features.family.presentation.models.FamilySavedMealsState
import com.foodfest.app.features.personaldish.data.PersonalDishRepository
import com.foodfest.app.core.cache.SharedDishNameCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for the saved meals screen.
 */
class FamilySavedMealsViewModel(
    private val repository: FamilyRepository = FamilyRepository(),
    private val dishRepository: DishRepository = DishRepository(),
    private val personalDishRepository: PersonalDishRepository = PersonalDishRepository()
) {
    var state by mutableStateOf(FamilySavedMealsState())
        private set

    private val scope = CoroutineScope(Dispatchers.Main)

    private var currentFamilyId: Int? = null
    private var currentMenuId: Int? = null

    /**
     * Loads saved meals for a family and stores optional menu context.
     */
    fun loadSavedMeals(familyId: Int, menuId: Int?) {
        if (familyId <= 0 || state.isLoading) return

        currentFamilyId = familyId
        currentMenuId = menuId
        state = state.copy(isLoading = true, errorMessage = null, applySuccessMessage = null)

        scope.launch {
            val result = repository.getSavedMeals(familyId)
            val meals = result.getOrNull()
            if (meals == null) {
                state = state.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Không tải được preset"
                )
                return@launch
            }

            state = state.copy(
                isLoading = false,
                meals = meals.map { mapSavedMeal(it) }
            )
        }
    }

    /**
     * Requests opening detail dialog and fetches detail.
     */
    fun openDetail(savedMealId: Int) {
        val familyId = currentFamilyId ?: return
        if (state.isDetailLoading) return

        state = state.copy(
            showDetailDialog = true,
            isDetailLoading = true,
            detailErrorMessage = null,
            detail = null
        )

        scope.launch {
            val result = repository.getSavedMealDetail(familyId, savedMealId)
            val detail = result.getOrNull()
            if (detail == null) {
                state = state.copy(
                    isDetailLoading = false,
                    detailErrorMessage = result.exceptionOrNull()?.message ?: "Không tải được preset"
                )
                return@launch
            }

            state = state.copy(
                isDetailLoading = false,
                detail = mapDetail(detail)
            )
        }
    }

    /**
     * Closes the detail dialog.
     */
    fun closeDetail() {
        state = state.copy(
            showDetailDialog = false,
            detail = null,
            detailErrorMessage = null,
            isDetailLoading = false
        )
    }

    /**
     * Requests delete confirmation.
     */
    fun requestDelete(savedMealId: Int) {
        state = state.copy(showDeleteDialog = true, deleteTargetId = savedMealId, deleteErrorMessage = null)
    }

    /**
     * Cancels delete confirmation.
     */
    fun cancelDelete() {
        state = state.copy(showDeleteDialog = false, deleteTargetId = null, deleteErrorMessage = null)
    }

    /**
     * Deletes a saved meal preset.
     */
    fun confirmDelete() {
        val familyId = currentFamilyId ?: return
        val savedMealId = state.deleteTargetId ?: return

        if (state.isDeleting) return

        state = state.copy(isDeleting = true, deleteErrorMessage = null)

        scope.launch {
            repository.deleteSavedMeal(familyId, savedMealId).fold(
                onSuccess = {
                    state = state.copy(isDeleting = false, showDeleteDialog = false, deleteTargetId = null)
                    loadSavedMeals(familyId, currentMenuId)
                },
                onFailure = { error ->
                    state = state.copy(
                        isDeleting = false,
                        deleteErrorMessage = error.message ?: "Không thể xóa preset"
                    )
                }
            )
        }
    }

    /**
     * Applies a saved meal preset to the current menu.
     */
    fun applySavedMeal(savedMealId: Int, onApplied: () -> Unit) {
        val familyId = currentFamilyId ?: return
        val menuId = currentMenuId

        if (menuId == null) {
            state = state.copy(applyErrorMessage = "Chưa chọn menu để áp dụng")
            return
        }

        if (state.isApplying) return

        state = state.copy(isApplying = true, applyErrorMessage = null)

        scope.launch {
            repository.applySavedMealToMenu(familyId, savedMealId, menuId).fold(
                onSuccess = {
                    state = state.copy(
                        isApplying = false,
                        applySuccessMessage = "Đã áp dụng preset"
                    )
                    onApplied()
                },
                onFailure = { error ->
                    state = state.copy(
                        isApplying = false,
                        applyErrorMessage = error.message ?: "Không thể áp dụng preset"
                    )
                }
            )
        }
    }

    /**
     * Maps API summary to UI card model.
     */
    private fun mapSavedMeal(item: FamilySavedMealSummary): FamilySavedMealCardUi {
        return FamilySavedMealCardUi(
            id = item.id,
            name = item.presetName,
            itemsCount = item.itemsCount,
            createdByName = item.createdByName,
            createdAtLabel = formatDateLabel(item.createdAt)
        )
    }

    /**
     * Maps API detail to UI detail model.
     */
    private suspend fun mapDetail(detail: FamilySavedMealDetail): FamilySavedMealDetailUi {
        val mappedItems = mutableListOf<FamilySavedMealItemUi>()
        for (item in detail.items) {
            mappedItems.add(mapDetailItem(item))
        }

        return FamilySavedMealDetailUi(
            id = detail.savedMeal.id,
            name = detail.savedMeal.presetName,
            items = mappedItems
        )
    }

    /**
     * Maps API detail item to UI item row.
     */
    private suspend fun mapDetailItem(item: FamilySavedMealItem): FamilySavedMealItemUi {
        return FamilySavedMealItemUi(
            title = resolveSavedMealItemTitle(item.dishId, item.personalDishId),
            subtitle = item.note?.takeIf { it.isNotBlank() }
        )
    }

    /**
     * Resolves a readable title for saved meal items.
     */
    private suspend fun resolveSavedMealItemTitle(dishId: Int?, personalDishId: Int?): String {
        return SharedDishNameCache.resolve(dishId, personalDishId, dishRepository, personalDishRepository)
    }

    /**
     * Formats createdAt to a short date label.
     */
    private fun formatDateLabel(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return value.split("T").firstOrNull() ?: value
    }
}
