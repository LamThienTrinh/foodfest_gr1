package com.foodfest.app.features.family.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.common.filter.DishFilter
import com.foodfest.app.common.filter.FilterCategory
import com.foodfest.app.common.filter.toFilterTag
import com.foodfest.app.features.dish.data.Dish
import com.foodfest.app.features.dish.data.PaginatedResponse
import com.foodfest.app.features.dish.data.DishRepository
import com.foodfest.app.features.family.data.FamilyMenuItem
import com.foodfest.app.features.family.data.FamilyPantryItem
import com.foodfest.app.features.family.data.FamilyRecentMenuItem
import com.foodfest.app.features.family.data.FamilyRepository
import com.foodfest.app.features.family.presentation.models.FamilyDayMenuState
import com.foodfest.app.features.family.presentation.models.FamilyMenuItemUi
import com.foodfest.app.features.family.presentation.models.FamilyMenuPickerTab
import com.foodfest.app.features.family.presentation.models.DishPantryMatchTone
import com.foodfest.app.features.family.presentation.models.DishPantryMatchUi
import com.foodfest.app.features.family.presentation.models.FamilyVoteItemUi
import com.foodfest.app.features.personaldish.data.PersonalDishRepository
import com.foodfest.app.core.cache.SharedDishNameCache
import com.foodfest.app.features.personaldish.data.PaginatedPersonalDishes
import com.foodfest.app.features.personaldish.data.PersonalDish
import com.foodfest.app.features.tag.data.TagRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * ViewModel for day menu detail screen.
 */
class FamilyDayMenuViewModel(
    private val repository: FamilyRepository = FamilyRepository(),
    private val dishRepository: DishRepository = DishRepository(),
    private val personalDishRepository: PersonalDishRepository = PersonalDishRepository(),
    private val tagRepository: TagRepository = TagRepository()
) {
    var state by mutableStateOf(FamilyDayMenuState())
        private set

    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentFamilyId: Int? = null
    private var currentMenuId: Int? = null
    private var currentMenuDate: LocalDate? = null
    private val recentDishCache = mutableMapOf<Int, Dish>()
    private val recentPersonalDishCache = mutableMapOf<Int, PersonalDish>()
    

    /**
     * Loads menu items for a specific menu slot.
     */
    fun loadDayMenu(familyId: Int, menuId: Int, menuDate: String, openVoteAfterLoad: Boolean = false) {
        if (familyId <= 0 || menuId <= 0 || state.isLoading) return

        val parsedDate = runCatching { LocalDate.parse(menuDate) }.getOrNull()
        if (parsedDate == null) {
            state = state.copy(errorMessage = "Ngày menu không hợp lệ")
            return
        }

        currentFamilyId = familyId
        currentMenuId = menuId
        currentMenuDate = parsedDate
        state = state.copy(isLoading = true, errorMessage = null, savePresetSuccessMessage = null)

        scope.launch {
            val pantryResult = repository.getPantryItems(familyId)
            val pantryItems = pantryResult.getOrNull().orEmpty()
            val pantryError = pantryResult.exceptionOrNull()?.message

            val weekStart = getStartOfWeek(parsedDate)
            val result = repository.getWeeklyMenus(familyId, weekStart.toString())
            val menus = result.getOrNull()
            if (menus == null) {
                state = state.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Không tải được menu",
                    pantryItems = pantryItems,
                    pantryErrorMessage = pantryError
                )
                return@launch
            }

            val matched = menus.firstOrNull { it.menu.id == menuId }
            if (matched == null) {
                state = state.copy(
                    isLoading = false,
                    errorMessage = "Không tìm thấy menu",
                    pantryItems = pantryItems,
                    pantryErrorMessage = pantryError
                )
                return@launch
            }

            val mappedItems = mutableListOf<FamilyMenuItemUi>()
            for (menuItem in matched.items) {
                mappedItems.add(mapMenuItem(menuItem))
            }
            state = state.copy(
                isLoading = false,
                items = mappedItems,
                pantryItems = pantryItems,
                pantryErrorMessage = pantryError
            )
            if (openVoteAfterLoad) {
                showVoteSheet()
            }
        }
    }

    /**
     * Shows dialog to save current menu as preset.
     */
    fun showSavePresetDialog(defaultName: String = "") {
        val input = state.presetNameInput.ifBlank { defaultName }
        state = state.copy(
            showSavePresetDialog = true,
            presetNameInput = input,
            savePresetError = null
        )
    }

    /**
     * Hides the save preset dialog and resets input.
     */
    fun hideSavePresetDialog() {
        state = state.copy(
            showSavePresetDialog = false,
            presetNameInput = "",
            savePresetError = null
        )
    }

    /**
     * Updates preset name input.
     */
    fun updatePresetNameInput(value: String) {
        state = state.copy(presetNameInput = value, savePresetError = null)
    }

    /**
     * Saves the current menu as a family preset.
     */
    fun savePresetFromMenu() {
        val familyId = currentFamilyId ?: return
        val menuId = currentMenuId ?: return

        if (state.isSavingPreset) return

        val presetName = state.presetNameInput.trim()
        if (presetName.isBlank()) {
            state = state.copy(savePresetError = "Vui lòng nhập tên bữa ăn mẫu")
            return
        }

        state = state.copy(isSavingPreset = true, savePresetError = null)

        scope.launch {
            repository.createSavedMealFromMenu(familyId, menuId, presetName).fold(
                onSuccess = {
                    state = state.copy(
                        isSavingPreset = false,
                        savePresetSuccessMessage = "Đã lưu bữa ăn mẫu"
                    )
                    hideSavePresetDialog()
                },
                onFailure = { error ->
                    state = state.copy(
                        isSavingPreset = false,
                        savePresetError = error.message ?: "Không thể lưu bữa ăn mẫu"
                    )
                }
            )
        }
    }

    /**
     * Shows the menu item picker sheet and prepares data for selection.
     */
    fun showAddDialog() {
        state = state.copy(
            showAddDialog = true,
            pickerTab = FamilyMenuPickerTab.SYSTEM,
            addErrorMessage = null,
            showFilterSheet = false
        )
        if (state.systemDishes.isEmpty() || state.personalDishes.isEmpty() ||
            (state.recentSystemDishes.isEmpty() && state.recentPersonalDishes.isEmpty())
        ) {
            loadPickerData()
        }
    }

    /**
     * Hides the menu item picker sheet and resets inputs.
     */
    fun hideAddDialog() {
        state = state.copy(
            showAddDialog = false,
            pickerTab = FamilyMenuPickerTab.SYSTEM,
            pickerSearchQuery = "",
            noteInput = "",
            addErrorMessage = null,
            pickerErrorMessage = null,
            showFilterSheet = false
        )
    }

    /**
     * Updates the picker tab selection.
     */
    fun updatePickerTab(tab: FamilyMenuPickerTab) {
        state = state.copy(pickerTab = tab)
    }

    /**
     * Updates the picker search query.
     */
    fun updatePickerSearchQuery(value: String) {
        state = state.copy(pickerSearchQuery = value, addErrorMessage = null)
    }

    /**
     * Updates optional note input.
     */
    fun updateNoteInput(value: String) {
        state = state.copy(noteInput = value)
    }

    /**
     * Shows or hides the tag filter sheet for the picker.
     */
    fun showFilterSheet(show: Boolean) {
        state = state.copy(showFilterSheet = show)
    }

    /**
     * Updates the tag filter selection for the picker.
     */
    fun updateFilter(filter: DishFilter) {
        state = state.copy(dishFilter = filter)
    }

    /**
     * Applies filters by reloading the system dishes from page 1.
     */
    fun applyPickerFilter() {
        loadSystemPage(1)
    }

    /**
     * Loads the previous page of system dishes.
     */
    fun goToPreviousSystemPage() {
        if (state.systemPage > 1) {
            loadSystemPage(state.systemPage - 1)
        }
    }

    /**
     * Loads the next page of system dishes.
     */
    fun goToNextSystemPage() {
        if (state.systemPage < state.systemTotalPages) {
            loadSystemPage(state.systemPage + 1)
        }
    }

    /**
     * Loads the previous page of personal dishes.
     */
    fun goToPreviousPersonalPage() {
        if (state.personalPage > 1) {
            loadPersonalPage(state.personalPage - 1)
        }
    }

    /**
     * Loads the next page of personal dishes.
     */
    fun goToNextPersonalPage() {
        if (state.personalPage < state.personalTotalPages) {
            loadPersonalPage(state.personalPage + 1)
        }
    }

    /**
     * Loads the picker data sources so users can choose dishes to add.
     */
    private fun loadPickerData() {
        if (state.isPickerLoading) return

        val familyId = currentFamilyId ?: return

        state = state.copy(isPickerLoading = true, pickerErrorMessage = null)

        scope.launch {
            ensureTagsLoaded()

            val systemResult = fetchSystemDishes(page = 1)
            val personalResult = fetchPersonalDishes(page = 1)
            val recentResult = fetchRecentItems(familyId)

            val systemData = systemResult.getOrNull()
            val personalData = personalResult.getOrNull()
            val recentData = recentResult.getOrNull()
            val pantry = state.pantryItems
            val systemMatches = buildSystemDishMatches(
                (systemData?.data.orEmpty() + recentData?.first.orEmpty()),
                pantry
            )
            val personalMatches = buildPersonalDishMatches(
                (personalData?.data.orEmpty() + recentData?.second.orEmpty()),
                pantry
            )

            val error = systemResult.exceptionOrNull()?.message
                ?: personalResult.exceptionOrNull()?.message
                ?: recentResult.exceptionOrNull()?.message

            state = state.copy(
                isPickerLoading = false,
                systemDishes = systemData?.data.orEmpty(),
                systemPage = systemData?.page ?: 1,
                systemTotalPages = systemData?.let { (it.total + it.limit - 1) / it.limit } ?: 1,
                systemTotalItems = systemData?.total ?: 0,
                personalDishes = personalData?.data.orEmpty(),
                personalPage = personalData?.page ?: 1,
                personalTotalPages = personalData?.let { (it.total + it.limit - 1) / it.limit } ?: 1,
                personalTotalItems = personalData?.total ?: 0,
                recentSystemDishes = recentData?.first.orEmpty(),
                recentPersonalDishes = recentData?.second.orEmpty(),
                systemDishMatches = state.systemDishMatches + systemMatches,
                personalDishMatches = state.personalDishMatches + personalMatches,
                pickerErrorMessage = error
            )
        }
    }

    /**
     * Reloads dish data for the picker after a failed attempt.
     */
    fun reloadPickerData() {
        loadPickerData()
    }

    /**
     * Loads a specific page of system dishes for the picker.
     */
    private fun loadSystemPage(page: Int) {
        if (state.isPickerLoading) return

        state = state.copy(isPickerLoading = true, pickerErrorMessage = null)

        scope.launch {
            ensureTagsLoaded()

            val result = fetchSystemDishes(page)
            val data = result.getOrNull()
            val error = result.exceptionOrNull()?.message
            val newMatches = buildSystemDishMatches(data?.data.orEmpty(), state.pantryItems)

            state = state.copy(
                isPickerLoading = false,
                systemDishes = data?.data.orEmpty(),
                systemPage = data?.page ?: page,
                systemTotalPages = data?.let { (it.total + it.limit - 1) / it.limit } ?: state.systemTotalPages,
                systemTotalItems = data?.total ?: state.systemTotalItems,
                systemDishMatches = state.systemDishMatches + newMatches,
                pickerErrorMessage = error
            )
        }
    }

    /**
     * Loads a specific page of personal dishes for the picker.
     */
    private fun loadPersonalPage(page: Int) {
        if (state.isPickerLoading) return

        state = state.copy(isPickerLoading = true, pickerErrorMessage = null)

        scope.launch {
            val result = fetchPersonalDishes(page)
            val data = result.getOrNull()
            val error = result.exceptionOrNull()?.message
            val newMatches = buildPersonalDishMatches(data?.data.orEmpty(), state.pantryItems)

            state = state.copy(
                isPickerLoading = false,
                personalDishes = data?.data.orEmpty(),
                personalPage = data?.page ?: page,
                personalTotalPages = data?.let { (it.total + it.limit - 1) / it.limit } ?: state.personalTotalPages,
                personalTotalItems = data?.total ?: state.personalTotalItems,
                personalDishMatches = state.personalDishMatches + newMatches,
                pickerErrorMessage = error
            )
        }
    }

    /**
     * Opens vote sheet and loads vote summary.
     */
    fun showVoteSheet() {
        state = state.copy(showVoteSheet = true, voteErrorMessage = null, isVoteLoading = true)
        loadVoteSummary()
    }

    /**
     * Hides the vote sheet.
     */
    fun hideVoteSheet() {
        state = state.copy(showVoteSheet = false, voteErrorMessage = null)
    }

    /**
     * Loads vote summary for the current menu.
     */
    private fun loadVoteSummary() {
        val familyId = currentFamilyId ?: return
        val menuId = currentMenuId ?: return

        scope.launch {
            val result = repository.getMenuVoteSummary(familyId, menuId)
            val summary = result.getOrNull()
            if (summary == null) {
                state = state.copy(
                    isVoteLoading = false,
                    voteErrorMessage = result.exceptionOrNull()?.message ?: "Không tải được vote"
                )
                return@launch
            }

            val voteItems = mutableListOf<FamilyVoteItemUi>()
            for (item in summary) {
                voteItems.add(
                    FamilyVoteItemUi(
                        itemId = item.familyMenuItemId,
                        title = resolveMenuItemTitle(item.dishId, item.personalDishId),
                        subtitle = item.note?.takeIf { it.isNotBlank() },
                        upVotes = item.upVotes,
                        downVotes = item.downVotes,
                        userVoteType = item.userVoteType
                    )
                )
            }

            state = state.copy(
                isVoteLoading = false,
                voteItems = voteItems
            )
        }
    }

    /**
     * Votes for a menu item and updates the local summary.
     */
    fun voteOnItem(itemId: Int, voteType: String) {
        val familyId = currentFamilyId ?: return
        val menuId = currentMenuId ?: return

        scope.launch {
            repository.voteMenuItem(familyId, menuId, itemId, voteType).fold(
                onSuccess = { result ->
                    val updated = state.voteItems.map { item ->
                        if (item.itemId == itemId) {
                            item.copy(
                                upVotes = result.upVotes,
                                downVotes = result.downVotes,
                                userVoteType = if (result.voted) result.voteType else null
                            )
                        } else {
                            item
                        }
                    }
                    state = state.copy(voteItems = updated)
                },
                onFailure = { error ->
                    state = state.copy(voteErrorMessage = error.message ?: "Không thể vote")
                }
            )
        }
    }

    /**
     * Adds a menu item from the picker and refreshes the menu list.
     */
    fun addMenuItemFromPicker(dishId: Int?, personalDishId: Int?) {
        val familyId = currentFamilyId ?: return
        val menuId = currentMenuId ?: return

        if (state.isAddingItem) return

        // Validate exactly one item source before calling API.
        if ((dishId == null && personalDishId == null) || (dishId != null && personalDishId != null)) {
            state = state.copy(addErrorMessage = "Nhập 1 trong 2: dishId hoặc personalDishId")
            return
        }

        state = state.copy(isAddingItem = true, addErrorMessage = null)

        scope.launch {
            repository.addFamilyMenuItem(
                familyId = familyId,
                menuId = menuId,
                dishId = dishId,
                personalDishId = personalDishId,
                note = state.noteInput.trim().ifBlank { null }
            ).fold(
                onSuccess = {
                    state = state.copy(isAddingItem = false)
                    hideAddDialog()
                    currentMenuDate?.let { date ->
                        loadDayMenu(familyId, menuId, date.toString())
                    }
                },
                onFailure = { error ->
                    state = state.copy(
                        isAddingItem = false,
                        addErrorMessage = error.message ?: "Không thể thêm món"
                    )
                }
            )
        }
    }

    /**
     * Removes a menu item by id.
     */
    fun removeMenuItem(itemId: Int) {
        val familyId = currentFamilyId ?: return
        val menuId = currentMenuId ?: return
        if (itemId <= 0) return

        scope.launch {
            repository.removeFamilyMenuItem(familyId, menuId, itemId).fold(
                onSuccess = {
                    currentMenuDate?.let { date ->
                        loadDayMenu(familyId, menuId, date.toString())
                    }
                },
                onFailure = { error ->
                    state = state.copy(errorMessage = error.message ?: "Không thể xóa món")
                }
            )
        }
    }

    /**
     * Maps API menu item into a UI-friendly display row.
     */
    private suspend fun mapMenuItem(item: FamilyMenuItem): FamilyMenuItemUi {
        return FamilyMenuItemUi(
            id = item.id,
            title = resolveMenuItemTitle(item.dishId, item.personalDishId),
            subtitle = item.note?.takeIf { it.isNotBlank() },
            dishId = item.dishId,
            personalDishId = item.personalDishId
        )
    }

    /**
     * Resolves a display name for a menu item, preferring server-backed dish names.
     */
    private suspend fun resolveMenuItemTitle(dishId: Int?, personalDishId: Int?): String {
        return SharedDishNameCache.resolve(dishId, personalDishId, dishRepository, personalDishRepository)
    }

    /**
     * Ensures tag metadata is available for picker filtering.
     */
    private suspend fun ensureTagsLoaded() {
        if (state.dishFilter.typeTags.isNotEmpty() ||
            state.dishFilter.tasteTags.isNotEmpty() ||
            state.dishFilter.ingredientTags.isNotEmpty()
        ) {
            return
        }

        tagRepository.getAllTags().fold(
            onSuccess = { tags ->
                val typeTags = tags
                    .filter { it.type.uppercase() == "TYPE" }
                    .map { it.toFilterTag(FilterCategory.TYPE) }
                val tasteTags = tags
                    .filter { it.type.uppercase() == "TASTE" }
                    .map { it.toFilterTag(FilterCategory.TASTE) }
                val ingredientTags = tags
                    .filter { it.type.uppercase() == "INGREDIENT" }
                    .map { it.toFilterTag(FilterCategory.INGREDIENT) }

                state = state.copy(
                    dishFilter = DishFilter(
                        typeTags = typeTags,
                        tasteTags = tasteTags,
                        ingredientTags = ingredientTags
                    )
                )
            },
            onFailure = { /* Ignore tag loading errors */ }
        )
    }

    /**
     * Fetches a page of system dishes, applying tag filters when provided.
     */
    private suspend fun fetchSystemDishes(page: Int): Result<PaginatedResponse<Dish>> {
        val filter = state.dishFilter
        return if (filter.hasSelectedTags) {
            dishRepository.getDishesWithFilter(
                page = page,
                typeTags = filter.getSelectedTypeTags(),
                tasteTags = filter.getSelectedTasteTags(),
                ingredientTags = filter.getSelectedIngredientTags()
            )
        } else {
            dishRepository.getDishes(page = page)
        }
    }

    /**
     * Fetches a page of personal dishes for the picker.
     */
    private suspend fun fetchPersonalDishes(page: Int): Result<PaginatedPersonalDishes> {
        return personalDishRepository.getMyDishes(page = page)
    }

    /**
     * Loads recent menu items for the family and maps them to dish cards.
     */
    private suspend fun fetchRecentItems(
        familyId: Int,
        limit: Int = 12
    ): Result<Pair<List<Dish>, List<PersonalDish>>> {
        return repository.getRecentMenuItems(familyId, limit).mapCatching { items ->
            mapRecentItems(items)
        }
    }

    /**
     * Converts recent menu items into lists of dish and personal dish objects.
     */
    private suspend fun mapRecentItems(
        items: List<FamilyRecentMenuItem>
    ): Pair<List<Dish>, List<PersonalDish>> {
        val systemIds = mutableSetOf<Int>()
        val personalIds = mutableSetOf<Int>()
        val systemDishes = mutableListOf<Dish>()
        val personalDishes = mutableListOf<PersonalDish>()

        for (item in items) {
            val dishId = item.dishId
            val personalDishId = item.personalDishId

            if (dishId != null && systemIds.add(dishId)) {
                val cached = recentDishCache[dishId]
                if (cached != null) {
                    systemDishes.add(cached)
                } else {
                    dishRepository.getDishById(dishId).getOrNull()?.let { dish ->
                        recentDishCache[dishId] = dish
                        systemDishes.add(dish)
                    }
                }
            }

            if (personalDishId != null && personalIds.add(personalDishId)) {
                val cached = recentPersonalDishCache[personalDishId]
                if (cached != null) {
                    personalDishes.add(cached)
                } else {
                    personalDishRepository.getById(personalDishId).getOrNull()?.let { dish ->
                        recentPersonalDishCache[personalDishId] = dish
                        personalDishes.add(dish)
                    }
                }
            }
        }

        return systemDishes to personalDishes
    }

    /**
     * Converts a date to the Monday of its week.
     */
    private fun getStartOfWeek(date: LocalDate): LocalDate {
        val shift = date.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal
        return date.minus(DatePeriod(days = shift))
    }

    /**
     * Phase 4.2: Builds rule-based pantry match score for system dishes.
     */
    private fun buildSystemDishMatches(
        dishes: List<Dish>,
        pantryItems: List<FamilyPantryItem>
    ): Map<Int, DishPantryMatchUi> {
        val pantryNames = pantryItems.map { normalizeIngredient(it.ingredientName) }.toSet()
        return dishes.distinctBy { it.id }.mapNotNull { dish ->
            buildDishMatch(dish.ingredients, pantryNames)?.let { dish.id to it }
        }.toMap()
    }

    /**
     * Phase 4.2: Builds rule-based pantry match score for personal dishes.
     */
    private fun buildPersonalDishMatches(
        dishes: List<PersonalDish>,
        pantryItems: List<FamilyPantryItem>
    ): Map<Int, DishPantryMatchUi> {
        val pantryNames = pantryItems.map { normalizeIngredient(it.ingredientName) }.toSet()
        return dishes.distinctBy { it.id }.mapNotNull { dish ->
            buildDishMatch(dish.ingredients, pantryNames)?.let { dish.id to it }
        }.toMap()
    }

    private fun buildDishMatch(
        ingredientsText: String?,
        pantryNames: Set<String>
    ): DishPantryMatchUi? {
        val requiredIngredients = extractRequiredIngredients(ingredientsText)
        if (requiredIngredients.isEmpty()) return null

        val available = requiredIngredients.filter { ingredient ->
            isIngredientAvailable(ingredient, pantryNames)
        }
        val missing = requiredIngredients.filterNot { it in available }
        val requiredCount = requiredIngredients.size
        val availableCount = available.size
        val missingCount = missing.size
        val matchRatio = if (requiredCount == 0) 0.0 else availableCount.toDouble() / requiredCount.toDouble()
        val matchPercent = (matchRatio * 100).toInt()

        val (statusText, tone) = when {
            matchRatio >= 0.8 -> "Có ngay!" to DishPantryMatchTone.HIGH
            matchRatio >= 0.5 -> "Thiếu $missingCount items" to DishPantryMatchTone.MEDIUM
            else -> "Thiếu nhiều" to DishPantryMatchTone.LOW
        }

        return DishPantryMatchUi(
            availableCount = availableCount,
            requiredCount = requiredCount,
            missingIngredients = missing,
            statusText = statusText,
            matchPercent = matchPercent,
            tone = tone
        )
    }

    private fun extractRequiredIngredients(ingredientsText: String?): List<String> {
        if (ingredientsText.isNullOrBlank()) return emptyList()
        return ingredientsText
            .lines()
            .mapNotNull { raw ->
                val cleaned = raw.trim()
                    .removePrefix("-")
                    .removePrefix("*")
                    .trim()
                if (cleaned.isBlank()) return@mapNotNull null
                val candidate = cleaned.substringBefore(":").substringBefore(",").trim()
                val normalized = normalizeIngredient(candidate)
                normalized.takeIf { it.isNotBlank() }
            }
            .distinct()
    }

    private fun normalizeIngredient(name: String): String {
        return name
            .lowercase()
            .replace(Regex("\\(.*?\\)"), " ")
            .replace(Regex("\\d+[\\d.,/]*"), " ")
            .replace(Regex("[^\\p{L}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isIngredientAvailable(required: String, pantryNames: Set<String>): Boolean {
        if (required.isBlank()) return false
        return pantryNames.any { pantry ->
            pantry == required || pantry.contains(required) || required.contains(pantry)
        }
    }
}
