package com.foodfest.app.features.family.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.common.filter.FilterBottomSheet
import com.foodfest.app.common.filter.FilterButton
import com.foodfest.app.common.filter.DishFilter
import com.foodfest.app.common.filter.SearchBar
import com.foodfest.app.components.FoodFestTextField
import com.foodfest.app.features.dish.data.Dish
import com.foodfest.app.features.dish.presentation.components.DishCard
import com.foodfest.app.features.dish.presentation.components.PaginationControls
import com.foodfest.app.features.family.presentation.models.FamilyDayMenuState
import com.foodfest.app.features.family.presentation.models.FamilyMenuItemUi
import com.foodfest.app.features.family.presentation.models.FamilyMenuPickerTab
import com.foodfest.app.features.family.presentation.models.FamilyVoteItemUi
import com.foodfest.app.features.family.presentation.models.DishPantryMatchTone
import com.foodfest.app.features.family.presentation.models.DishPantryMatchUi
import com.foodfest.app.features.personaldish.data.PersonalDish
import com.foodfest.app.features.personaldish.presentation.components.PersonalDishCard
import com.foodfest.app.theme.AppColors

/**
 * Day menu screen for a single meal slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyDayMenuScreen(
    familyId: Int,
    menuId: Int,
    menuDate: String,
    mealType: String,
    openVoteOnEntry: Boolean = false,
    viewModel: FamilyDayMenuViewModel = remember { FamilyDayMenuViewModel() },
    onOpenVoteEntryHandled: () -> Unit = {},
    onBack: () -> Unit
) {
    val state = viewModel.state
    val mealLabel = remember(mealType) { mealTypeLabel(mealType) }

    // Load menu items when the menu changes.
    LaunchedEffect(menuId, menuDate) {
        viewModel.loadDayMenu(familyId, menuId, menuDate, openVoteAfterLoad = openVoteOnEntry)
        if (openVoteOnEntry) {
            onOpenVoteEntryHandled()
        }
    }

    if (state.showAddDialog) {
        MenuItemPickerSheet(
            state = state,
            onDismiss = viewModel::hideAddDialog,
            onTabSelected = viewModel::updatePickerTab,
            onSearchChange = viewModel::updatePickerSearchQuery,
            onShowFilterSheet = viewModel::showFilterSheet,
            onFilterChange = viewModel::updateFilter,
            onApplyFilter = viewModel::applyPickerFilter,
            onNoteChange = viewModel::updateNoteInput,
            onSelectDish = { dishId ->
                viewModel.addMenuItemFromPicker(dishId = dishId, personalDishId = null)
            },
            onSelectPersonalDish = { personalDishId ->
                viewModel.addMenuItemFromPicker(dishId = null, personalDishId = personalDishId)
            },
            onPreviousSystemPage = viewModel::goToPreviousSystemPage,
            onNextSystemPage = viewModel::goToNextSystemPage,
            onPreviousPersonalPage = viewModel::goToPreviousPersonalPage,
            onNextPersonalPage = viewModel::goToNextPersonalPage,
            onRetry = viewModel::reloadPickerData
        )
    }

    if (state.showSavePresetDialog) {
        SavePresetDialog(
            state = state,
            onDismiss = viewModel::hideSavePresetDialog,
            onNameChange = viewModel::updatePresetNameInput,
            onSave = viewModel::savePresetFromMenu
        )
    }

    if (state.showVoteSheet) {
        VoteModal(
            state = state,
            onDismiss = viewModel::hideVoteSheet,
            onVote = viewModel::voteOnItem
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${mealLabel} • ${menuDate}",
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Brown
                        )
                        Text(
                            text = "Chi tiết bữa ăn",
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = AppColors.Brown
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::showVoteSheet) {
                        Icon(
                            imageVector = Icons.Default.HowToVote,
                            contentDescription = "Vote",
                            tint = AppColors.Orange
                        )
                    }
                    IconButton(onClick = { viewModel.showSavePresetDialog("$mealLabel $menuDate") }) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Lưu bữa ăn mẫu",
                            tint = AppColors.Orange
                        )
                    }
                    IconButton(onClick = viewModel::showAddDialog) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Thêm món",
                            tint = AppColors.Orange
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            when {
                state.isLoading -> {
                    DayMenuLoading()
                }
                state.errorMessage != null -> {
                    DayMenuError(
                        message = state.errorMessage,
                        onRetry = { viewModel.loadDayMenu(familyId, menuId, menuDate) }
                    )
                }
                else -> {
                    DayMenuContent(
                        state = state,
                        onRemove = viewModel::removeMenuItem
                    )
                }
            }
        }
    }
}

/**
 * Maps meal type to a Vietnamese label.
 */
private fun mealTypeLabel(mealType: String): String {
    return when (mealType.lowercase()) {
        "breakfast" -> "Sáng"
        "lunch" -> "Trưa"
        "dinner" -> "Tối"
        "snack" -> "Ăn vặt"
        "other" -> "Khác"
        else -> mealType
    }
}

/**
 * Loading indicator for day menu.
 */
@Composable
private fun DayMenuLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = AppColors.Orange)
    }
}

/**
 * Error state with retry action.
 */
@Composable
private fun DayMenuError(
    message: String?,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message ?: "Có lỗi xảy ra",
            fontSize = 14.sp,
            color = AppColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange)
        ) {
            Text(text = "Thử lại")
        }
    }
}

/**
 * Day menu list content.
 */
@Composable
private fun DayMenuContent(
    state: FamilyDayMenuState,
    onRemove: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (!state.savePresetSuccessMessage.isNullOrBlank()) {
            Text(
                text = state.savePresetSuccessMessage.orEmpty(),
                fontSize = 12.sp,
                color = AppColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.items.isEmpty()) {
            Text(
                text = "Chưa có món nào",
                fontSize = 14.sp,
                color = AppColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        state.items.forEach { item ->
            DayMenuItemCard(item = item, onRemove = onRemove)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Card for a single menu item.
 */
@Composable
private fun DayMenuItemCard(
    item: FamilyMenuItemUi,
    onRemove: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                if (!item.subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.subtitle,
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                }
            }
            IconButton(onClick = { onRemove(item.id) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xóa",
                    tint = AppColors.Orange
                )
            }
        }
    }
}

/**
 * Bottom sheet picker for adding items to the menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuItemPickerSheet(
    state: FamilyDayMenuState,
    onDismiss: () -> Unit,
    onTabSelected: (FamilyMenuPickerTab) -> Unit,
    onSearchChange: (String) -> Unit,
    onShowFilterSheet: (Boolean) -> Unit,
    onFilterChange: (DishFilter) -> Unit,
    onApplyFilter: () -> Unit,
    onNoteChange: (String) -> Unit,
    onSelectDish: (Int) -> Unit,
    onSelectPersonalDish: (Int) -> Unit,
    onPreviousSystemPage: () -> Unit,
    onNextSystemPage: () -> Unit,
    onPreviousPersonalPage: () -> Unit,
    onNextPersonalPage: () -> Unit,
    onRetry: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val query = state.pickerSearchQuery.trim()
    val filteredSystem = filterSystemDishes(state.systemDishes, state.dishFilter, query)
    val filteredPersonal = filterPersonalDishes(state.personalDishes, state.dishFilter, query)
    val filteredRecentSystem = filterSystemDishes(state.recentSystemDishes, state.dishFilter, query)
    val filteredRecentPersonal = filterPersonalDishes(state.recentPersonalDishes, state.dishFilter, query)
    val systemMatches = state.systemDishMatches
    val personalMatches = state.personalDishMatches

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Chọn món",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            TabRow(selectedTabIndex = state.pickerTab.ordinal) {
                FamilyMenuPickerTab.values().forEach { tab ->
                    Tab(
                        selected = tab == state.pickerTab,
                        onClick = { onTabSelected(tab) },
                        text = { Text(text = pickerTabLabel(tab)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SearchBar(
                query = state.pickerSearchQuery,
                onQueryChange = onSearchChange,
                placeholder = "Tìm theo tên món"
            )
            Spacer(modifier = Modifier.height(8.dp))
            FilterButton(
                selectedCount = state.dishFilter.selectedCount,
                onClick = { onShowFilterSheet(true) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            FoodFestTextField(
                value = state.noteInput,
                onValueChange = onNoteChange,
                placeholder = "Ghi chú (tuỳ chọn)"
            )
            if (!state.pantryErrorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.pantryErrorMessage.orEmpty(),
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
            }

            if (!state.addErrorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.addErrorMessage.orEmpty(),
                    fontSize = 12.sp,
                    color = AppColors.Error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.isPickerLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Orange)
                    }
                }
                state.pickerErrorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.pickerErrorMessage.orEmpty(),
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange)
                        ) {
                            Text(text = "Thử lại")
                        }
                    }
                }
                else -> {
                    when (state.pickerTab) {
                        FamilyMenuPickerTab.SYSTEM -> {
                            PickerGrid(
                                isEmpty = filteredSystem.isEmpty(),
                                emptyText = "Không tìm thấy món hệ thống",
                                content = {
                                    items(filteredSystem) { dish ->
                                        MatchableSystemDishCard(
                                            dish = dish,
                                            match = systemMatches[dish.id],
                                            onClick = { onSelectDish(dish.id) }
                                        )
                                    }
                                }
                            )
                            if (state.systemTotalPages > 1) {
                                PaginationControls(
                                    currentPage = state.systemPage,
                                    totalPages = state.systemTotalPages,
                                    onPreviousPage = onPreviousSystemPage,
                                    onNextPage = onNextSystemPage
                                )
                            }
                        }
                        FamilyMenuPickerTab.PERSONAL -> {
                            PickerGrid(
                                isEmpty = filteredPersonal.isEmpty(),
                                emptyText = "Không tìm thấy món cá nhân",
                                content = {
                                    items(filteredPersonal) { dish ->
                                        MatchablePersonalDishCard(
                                            dish = dish,
                                            match = personalMatches[dish.id],
                                            onClick = { onSelectPersonalDish(dish.id) }
                                        )
                                    }
                                }
                            )
                            if (state.personalTotalPages > 1) {
                                PaginationControls(
                                    currentPage = state.personalPage,
                                    totalPages = state.personalTotalPages,
                                    onPreviousPage = onPreviousPersonalPage,
                                    onNextPage = onNextPersonalPage
                                )
                            }
                        }
                        FamilyMenuPickerTab.RECENT -> {
                            if (filteredRecentSystem.isEmpty() && filteredRecentPersonal.isEmpty()) {
                                Text(
                                    text = "Chưa có dữ liệu gần đây",
                                    fontSize = 12.sp,
                                    color = AppColors.TextSecondary
                                )
                            } else {
                                if (filteredRecentSystem.isNotEmpty()) {
                                    Text(
                                        text = "System Dishes",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    PickerGrid(
                                        isEmpty = false,
                                        emptyText = "",
                                        content = {
                                            items(filteredRecentSystem) { dish ->
                                                MatchableSystemDishCard(
                                                    dish = dish,
                                                    match = systemMatches[dish.id],
                                                    onClick = { onSelectDish(dish.id) }
                                                )
                                            }
                                        }
                                    )
                                }

                                if (filteredRecentPersonal.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "My Dishes",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    PickerGrid(
                                        isEmpty = false,
                                        emptyText = "",
                                        content = {
                                            items(filteredRecentPersonal) { dish ->
                                                MatchablePersonalDishCard(
                                                    dish = dish,
                                                    match = personalMatches[dish.id],
                                                    onClick = { onSelectPersonalDish(dish.id) }
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onDismiss) {
                Text(text = "Đóng")
            }
        }
    }

    if (state.showFilterSheet) {
        FilterBottomSheet(
            filter = state.dishFilter,
            onFilterChange = onFilterChange,
            onApplyFilter = onApplyFilter,
            onDismiss = { onShowFilterSheet(false) }
        )
    }
}

@Composable
private fun MatchableSystemDishCard(
    dish: Dish,
    match: DishPantryMatchUi?,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Phase 4.2: Rule-based pantry match badge in Menu Item Picker.
        match?.let { PantryMatchBadge(match = it) }
        DishCard(dish = dish, onClick = onClick)
    }
}

@Composable
private fun MatchablePersonalDishCard(
    dish: PersonalDish,
    match: DishPantryMatchUi?,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Phase 4.2: Rule-based pantry match badge in Menu Item Picker.
        match?.let { PantryMatchBadge(match = it) }
        PersonalDishCard(dish = dish, onClick = onClick)
    }
}

@Composable
private fun PantryMatchBadge(match: DishPantryMatchUi) {
    val background = when (match.tone) {
        DishPantryMatchTone.HIGH -> Color(0xFFE6F6EA)
        DishPantryMatchTone.MEDIUM -> Color(0xFFFFF4D9)
        DishPantryMatchTone.LOW -> Color(0xFFFFE9E9)
    }
    val textColor = when (match.tone) {
        DishPantryMatchTone.HIGH -> Color(0xFF1E7A3D)
        DishPantryMatchTone.MEDIUM -> Color(0xFF9A6A00)
        DishPantryMatchTone.LOW -> Color(0xFFB33A3A)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${match.matchPercent}% • ${match.statusText}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
        Text(
            text = "${match.availableCount}/${match.requiredCount}",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

/**
 * Shared grid layout for the picker tabs.
 */
@Composable
private fun PickerGrid(
    isEmpty: Boolean,
    emptyText: String,
    content: LazyGridScope.() -> Unit
) {
    if (isEmpty) {
        Text(
            text = emptyText,
            fontSize = 12.sp,
            color = AppColors.TextSecondary
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

/**
 * Maps picker tab to its Vietnamese label.
 */
private fun pickerTabLabel(tab: FamilyMenuPickerTab): String {
    return when (tab) {
        FamilyMenuPickerTab.SYSTEM -> "System Dishes"
        FamilyMenuPickerTab.PERSONAL -> "My Dishes"
        FamilyMenuPickerTab.RECENT -> "Recent"
    }
}

/**
 * Filters system dishes by search text and tag selections.
 */
private fun filterSystemDishes(
    dishes: List<Dish>,
    filter: DishFilter,
    query: String
): List<Dish> {
    var result = dishes

    if (query.isNotBlank()) {
        result = result.filter { dish ->
            dish.name.contains(query, ignoreCase = true) ||
                (dish.description?.contains(query, ignoreCase = true) == true)
        }
    }

    if (filter.hasSelectedTags) {
        result = result.filter { dish ->
            val tagNames = dish.tags?.map { it.name }.orEmpty()
            matchesFilterTags(tagNames, filter)
        }
    }

    return result
}

/**
 * Filters personal dishes by search text and tag selections.
 */
private fun filterPersonalDishes(
    dishes: List<PersonalDish>,
    filter: DishFilter,
    query: String
): List<PersonalDish> {
    var result = dishes

    if (query.isNotBlank()) {
        result = result.filter { dish ->
            dish.dishName.contains(query, ignoreCase = true) ||
                (dish.description?.contains(query, ignoreCase = true) == true)
        }
    }

    if (filter.hasSelectedTags) {
        result = result.filter { dish ->
            matchesFilterTags(dish.tags, filter)
        }
    }

    return result
}

/**
 * Checks if a dish's tags satisfy the selected tag filters.
 */
private fun matchesFilterTags(tags: List<String>, filter: DishFilter): Boolean {
    val selectedTypes = filter.getSelectedTypeTags()
    val selectedTastes = filter.getSelectedTasteTags()
    val selectedIngredients = filter.getSelectedIngredientTags()

    val matchesType = selectedTypes.isEmpty() || selectedTypes.any { it in tags }
    val matchesTaste = selectedTastes.isEmpty() || selectedTastes.any { it in tags }
    val matchesIngredient = selectedIngredients.isEmpty() || selectedIngredients.any { it in tags }

    return matchesType && matchesTaste && matchesIngredient
}

/**
 * Dialog for saving the current menu as a preset.
 */
@Composable
private fun SavePresetDialog(
    state: FamilyDayMenuState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Lưu bữa ăn mẫu")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FoodFestTextField(
                    value = state.presetNameInput,
                    onValueChange = onNameChange,
                    placeholder = "Nhập tên bữa ăn mẫu"
                )
                if (!state.savePresetError.isNullOrBlank()) {
                    Text(
                        text = state.savePresetError.orEmpty(),
                        fontSize = 12.sp,
                        color = AppColors.Error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !state.isSavingPreset,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange)
            ) {
                Text(text = if (state.isSavingPreset) "Đang lưu..." else "Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Hủy")
            }
        }
    )
}

/**
 * Phase 5.2 compact vote modal for quick menu item reactions.
 */
@Composable
private fun VoteModal(
    state: FamilyDayMenuState,
    onDismiss: () -> Unit,
    onVote: (Int, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Bạn chọn gì cho bữa này?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                state.isVoteLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Orange)
                    }
                }
                state.voteErrorMessage != null -> {
                    Text(
                        text = state.voteErrorMessage.orEmpty(),
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                }
                state.voteItems.isEmpty() -> {
                    Text(
                        text = "Chưa có món để vote",
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                }
                else -> {
                    state.voteItems.forEach { item ->
                            VoteItemRow(
                                item = item,
                                onVote = { itemId, voteType ->
                                    onVote(itemId, voteType)
                                    onDismiss()
                                }
                            )
                    }
                }
            }
        }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Đóng")
            }
        }
    )
}

/**
 * Row for a single vote item.
 */
@Composable
private fun VoteItemRow(
    item: FamilyVoteItemUi,
    onVote: (Int, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Background),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary
            )
            if (!item.subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.subtitle,
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoteButton(
                    label = "👍 ${item.upVotes}",
                    isSelected = item.userVoteType == "up",
                    onClick = { onVote(item.itemId, "up") }
                )
                VoteButton(
                    label = "👎 ${item.downVotes}",
                    isSelected = item.userVoteType == "down",
                    onClick = { onVote(item.itemId, "down") }
                )
            }
        }
    }
}

/**
 * Small toggle button for vote actions.
 */
@Composable
private fun VoteButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) AppColors.Orange else AppColors.White,
            contentColor = if (isSelected) Color.White else AppColors.TextPrimary
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = label, fontSize = 12.sp)
    }
}
