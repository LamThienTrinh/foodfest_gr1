package com.foodfest.app.features.family.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.features.family.presentation.models.FamilyMenuDayUi
import com.foodfest.app.features.family.presentation.models.FamilyMenuSlotUi
import com.foodfest.app.features.family.presentation.models.FamilyWeeklyMenuState
import com.foodfest.app.theme.AppColors
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
 * Weekly menu screen that shows menu slots across the week.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyWeeklyMenuScreen(
    familyId: Int,
    viewModel: FamilyWeeklyMenuViewModel = remember { FamilyWeeklyMenuViewModel() },
    onBack: () -> Unit,
    onOpenDayMenu: (Int, String, String) -> Unit,
    onOpenSavedMeals: (Int, String, String) -> Unit,
    onOpenShoppingList: (Int) -> Unit
) {
    val state = viewModel.state
    var pendingSlot by remember { mutableStateOf<Pair<LocalDate, FamilyMenuSlotUi>?>(null) }
    var showWeekPicker by remember { mutableStateOf(false) }

    // Load weekly data when familyId changes.
    LaunchedEffect(familyId) {
        viewModel.loadWeeklyMenu(familyId)
    }

    if (pendingSlot != null) {
        CreateMenuDialog(
            slot = pendingSlot!!,
            isCreating = state.isCreatingMenu,
            onDismiss = { pendingSlot = null },
            onCreate = { date, mealType ->
                viewModel.createMenu(familyId, date, mealType) { menuId ->
                    pendingSlot = null
                    onOpenDayMenu(menuId, date.toString(), mealType)
                }
            },
            onPickFromSaved = { date, mealType ->
                viewModel.createMenu(familyId, date, mealType) { menuId ->
                    pendingSlot = null
                    onOpenSavedMeals(menuId, date.toString(), mealType)
                }
            }
        )
    }

    if (showWeekPicker) {
        WeekPickerDialog(
            weekStarts = upcomingWeekStarts(),
            onDismiss = { showWeekPicker = false },
            onSelect = { weekStart ->
                showWeekPicker = false
                viewModel.loadWeeklyMenu(familyId, weekStart)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Lịch tuần",
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Brown
                    )
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
                    IconButton(onClick = { showWeekPicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Chọn tuần",
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
                    WeeklyMenuLoading()
                }
                state.errorMessage != null -> {
                    WeeklyMenuError(
                        message = state.errorMessage,
                        onRetry = { viewModel.loadWeeklyMenu(familyId, state.weekStart) }
                    )
                }
                else -> {
                    WeeklyMenuContent(
                        state = state,
                        onGenerateShoppingList = {
                            viewModel.generateShoppingList(familyId, onOpenShoppingList)
                        },
                        onSlotClick = { date, slot ->
                            if (slot.menuId != null) {
                                onOpenDayMenu(slot.menuId, date.toString(), slot.mealType)
                            } else {
                                pendingSlot = date to slot
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Loading state for weekly menu screen.
 */
@Composable
private fun WeeklyMenuLoading() {
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
 * Error state with retry.
 */
@Composable
private fun WeeklyMenuError(
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
 * Main weekly menu list content.
 */
@Composable
private fun WeeklyMenuContent(
    state: FamilyWeeklyMenuState,
    onGenerateShoppingList: () -> Unit,
    onSlotClick: (LocalDate, FamilyMenuSlotUi) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Phase 4.3 entry point: create shopping checklist from all menu slots in this week.
        Button(
            onClick = onGenerateShoppingList,
            enabled = !state.isGeneratingShoppingList,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (state.isGeneratingShoppingList) "Đang tạo danh sách mua..." else "Tạo danh sách mua sắm")
        }
        if (!state.shoppingListError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.shoppingListError.orEmpty(),
                color = AppColors.Error,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        state.days.forEach { day ->
            WeeklyMenuDayCard(day = day, onSlotClick = onSlotClick)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Card row for a single day with meal slots.
 */
@Composable
private fun WeeklyMenuDayCard(
    day: FamilyMenuDayUi,
    onSlotClick: (LocalDate, FamilyMenuSlotUi) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${day.dayLabel} • ${day.date}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                day.slots.forEach { slot ->
                    WeeklyMenuSlotCard(
                        slot = slot,
                        modifier = Modifier.weight(1f),
                        onClick = { onSlotClick(day.date, slot) }
                    )
                }
            }
        }
    }
}

/**
 * Card representing a meal slot.
 */
@Composable
private fun WeeklyMenuSlotCard(
    slot: FamilyMenuSlotUi,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val hasMenu = slot.menuId != null
    val background = if (hasMenu) AppColors.Background else Color.White

    Card(
        modifier = modifier
            .height(90.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = slot.mealLabel,
                fontSize = 12.sp,
                color = AppColors.TextSecondary
            )
            Text(
                text = if (hasMenu) "${slot.itemsCount} món" else "Chưa chọn",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary
            )
        }
    }
}

/**
 * Dialog to create a new menu slot.
 */
@Composable
private fun CreateMenuDialog(
    slot: Pair<LocalDate, FamilyMenuSlotUi>,
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (LocalDate, String) -> Unit,
    onPickFromSaved: (LocalDate, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Tạo menu mới")
        },
        text = {
            Column {
                Text(
                    text = "${slot.first} • ${slot.second.mealLabel}",
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onCreate(slot.first, slot.second.mealType) },
                    enabled = !isCreating,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isCreating) "Đang tạo..." else "Tạo thủ công")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onPickFromSaved(slot.first, slot.second.mealType) },
                    enabled = !isCreating,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = AppColors.TextPrimary,
                        disabledContainerColor = Color.White,
                        disabledContentColor = AppColors.TextSecondary
                    ),
                    border = BorderStroke(1.dp, AppColors.Orange.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isCreating) "Đang tạo..." else "Chọn từ bữa đã lưu")
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
 * Dialog that shows the next 3 weeks (excluding current week) for selection.
 */
@Composable
private fun WeekPickerDialog(
    weekStarts: List<LocalDate>,
    onDismiss: () -> Unit,
    onSelect: (LocalDate) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Chọn tuần") },
        text = {
            Column {
                weekStarts.forEach { weekStart ->
                    val weekEnd = weekStart.plus(DatePeriod(days = 6))
                    Button(
                        onClick = { onSelect(weekStart) },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "${weekStart} → ${weekEnd}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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
 * Builds a list of week starts for the next 3 weeks, excluding the current week.
 */
@OptIn(ExperimentalTime::class)
private fun upcomingWeekStarts(): List<LocalDate> {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val currentWeekStart = startOfWeek(today)
    return (1..3).map { offset ->
        currentWeekStart.plus(DatePeriod(days = offset * 7))
    }
}

/**
 * Converts a date to the Monday of its week.
 */
private fun startOfWeek(date: LocalDate): LocalDate {
    val shift = date.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal
    return date.minus(DatePeriod(days = shift))
}
