package com.foodfest.app.features.family.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.family.data.FamilyMenuWithItems
import com.foodfest.app.features.family.data.FamilyRepository
import com.foodfest.app.features.family.presentation.models.FamilyMenuDayUi
import com.foodfest.app.features.family.presentation.models.FamilyMenuSlotUi
import com.foodfest.app.features.family.presentation.models.FamilyWeeklyMenuState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * ViewModel for weekly menu screen.
 */
class FamilyWeeklyMenuViewModel(
    private val repository: FamilyRepository = FamilyRepository()
) {
    var state by mutableStateOf(FamilyWeeklyMenuState())
        private set

    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Loads weekly menu data for the given family.
     */
    fun loadWeeklyMenu(familyId: Int, weekStart: LocalDate? = null) {
        if (familyId <= 0 || state.isLoading) return

        val resolvedWeekStart = weekStart ?: getStartOfWeek(today())
        state = state.copy(isLoading = true, errorMessage = null)

        scope.launch {
            repository.getWeeklyMenus(familyId, resolvedWeekStart.toString()).fold(
                onSuccess = { menus ->
                    val days = buildWeekDays(resolvedWeekStart, menus)
                    state = state.copy(
                        isLoading = false,
                        weekStart = resolvedWeekStart,
                        days = days
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Không tải được lịch tuần"
                    )
                }
            )
        }
    }

    /**
     * Creates a menu slot and triggers callback for navigation.
     */
    fun createMenu(
        familyId: Int,
        date: LocalDate,
        mealType: String,
        onCreated: (Int) -> Unit
    ) {
        if (familyId <= 0 || state.isCreatingMenu) return

        state = state.copy(isCreatingMenu = true, errorMessage = null)

        scope.launch {
            repository.createFamilyMenu(
                familyId = familyId,
                menuDate = date.toString(),
                mealType = mealType,
                status = "draft"
            ).fold(
                onSuccess = { menu ->
                    state = state.copy(isCreatingMenu = false)
                    onCreated(menu.id)
                    loadWeeklyMenu(familyId, state.weekStart)
                },
                onFailure = { error ->
                    state = state.copy(
                        isCreatingMenu = false,
                        errorMessage = error.message ?: "Không tạo được menu"
                    )
                }
            )
        }
    }

    /**
     * Phase 4.3: Generates shopping list for the currently selected week.
     */
    fun generateShoppingList(
        familyId: Int,
        onGenerated: (Int) -> Unit
    ) {
        val weekStart = state.weekStart ?: return
        if (familyId <= 0 || state.isGeneratingShoppingList) return

        state = state.copy(isGeneratingShoppingList = true, shoppingListError = null)
        scope.launch {
            repository.generateShoppingList(familyId, weekStart.toString()).fold(
                onSuccess = { detail ->
                    state = state.copy(isGeneratingShoppingList = false)
                    onGenerated(detail.shoppingList.id)
                },
                onFailure = { error ->
                    state = state.copy(
                        isGeneratingShoppingList = false,
                        shoppingListError = error.message ?: "Không tạo được shopping list"
                    )
                }
            )
        }
    }

    /**
     * Returns list of day rows with menu slots for the week.
     */
    private fun buildWeekDays(
        weekStart: LocalDate,
        menus: List<FamilyMenuWithItems>
    ): List<FamilyMenuDayUi> {
        val menuMap = menus.associateBy { menu ->
            menu.menu.menuDate to menu.menu.mealType
        }

        val mealTypes = listOf("breakfast", "lunch", "dinner")
        val mealLabels = mapOf(
            "breakfast" to "Sáng",
            "lunch" to "Trưa",
            "dinner" to "Tối"
        )

        return (0..6).map { offset ->
            val date = weekStart.plus(DatePeriod(days = offset))
            val dateKey = date.toString()
            val slots = mealTypes.map { mealType ->
                val menu = menuMap[dateKey to mealType]
                FamilyMenuSlotUi(
                    mealType = mealType,
                    mealLabel = mealLabels[mealType] ?: mealType,
                    menuId = menu?.menu?.id,
                    itemsCount = menu?.items?.size ?: 0,
                    status = menu?.menu?.status
                )
            }

            FamilyMenuDayUi(
                date = date,
                dayLabel = dayLabel(date.dayOfWeek),
                slots = slots
            )
        }
    }

    /**
     * Gets today's date in local timezone.
     */
    @OptIn(ExperimentalTime::class)
    private fun today(): LocalDate {
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    /**
     * Converts a date to the Monday of its week.
     */
    private fun getStartOfWeek(date: LocalDate): LocalDate {
        val shift = date.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal
        return date.minus(DatePeriod(days = shift))
    }

    /**
     * Maps day-of-week to Vietnamese label.
     */
    private fun dayLabel(day: DayOfWeek): String {
        return when (day) {
            DayOfWeek.MONDAY -> "Thứ 2"
            DayOfWeek.TUESDAY -> "Thứ 3"
            DayOfWeek.WEDNESDAY -> "Thứ 4"
            DayOfWeek.THURSDAY -> "Thứ 5"
            DayOfWeek.FRIDAY -> "Thứ 6"
            DayOfWeek.SATURDAY -> "Thứ 7"
            DayOfWeek.SUNDAY -> "Chủ nhật"
        }
    }
}
