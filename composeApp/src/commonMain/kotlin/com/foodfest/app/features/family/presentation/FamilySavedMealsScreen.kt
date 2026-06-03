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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.features.family.presentation.models.FamilySavedMealCardUi
import com.foodfest.app.features.family.presentation.models.FamilySavedMealDetailUi
import com.foodfest.app.features.family.presentation.models.FamilySavedMealsState
import com.foodfest.app.theme.AppColors

/**
 * Saved meals screen listing family presets and allowing apply/delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilySavedMealsScreen(
    familyId: Int,
    menuId: Int?,
    menuDate: String?,
    mealType: String?,
    viewModel: FamilySavedMealsViewModel = remember { FamilySavedMealsViewModel() },
    onBack: () -> Unit,
    onAppliedToMenu: (Int, String?, String?) -> Unit
) {
    val state = viewModel.state

    // Load saved meals when entering the screen or when family changes.
    LaunchedEffect(familyId, menuId) {
        viewModel.loadSavedMeals(familyId, menuId)
    }

    if (state.showDetailDialog) {
        SavedMealDetailDialog(state = state, onDismiss = viewModel::closeDetail)
    }

    if (state.showDeleteDialog) {
        SavedMealDeleteDialog(
            state = state,
            onDismiss = viewModel::cancelDelete,
            onConfirm = viewModel::confirmDelete
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Preset đã lưu",
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (menuId != null) {
                SavedMealContextCard(menuDate = menuDate, mealType = mealType)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!state.applySuccessMessage.isNullOrBlank()) {
                Text(
                    text = state.applySuccessMessage.orEmpty(),
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!state.applyErrorMessage.isNullOrBlank()) {
                Text(
                    text = state.applyErrorMessage.orEmpty(),
                    fontSize = 12.sp,
                    color = AppColors.Error
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            when {
                state.isLoading -> {
                    LoadingState()
                }
                state.errorMessage != null -> {
                    ErrorState(message = state.errorMessage)
                }
                state.meals.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    state.meals.forEach { meal ->
                        SavedMealCard(
                            meal = meal,
                            canApply = menuId != null,
                            isApplying = state.isApplying,
                            onApply = {
                                viewModel.applySavedMeal(meal.id) {
                                    onAppliedToMenu(menuId ?: 0, menuDate, mealType)
                                }
                            },
                            onShowDetail = { viewModel.openDetail(meal.id) },
                            onDelete = { viewModel.requestDelete(meal.id) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

/**
 * Small context card when applying preset to a menu slot.
 */
@Composable
private fun SavedMealContextCard(menuDate: String?, mealType: String?) {
    val label = when (mealType) {
        "breakfast" -> "Sáng"
        "lunch" -> "Trưa"
        "dinner" -> "Tối"
        else -> mealType ?: ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Áp dụng preset cho menu",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            if (!menuDate.isNullOrBlank() || label.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOf(menuDate, label).filter { !it.isNullOrBlank() }.joinToString(" • "),
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Loading placeholder for saved meals.
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = AppColors.Orange)
    }
}

/**
 * Error placeholder when list fails to load.
 */
@Composable
private fun ErrorState(message: String?) {
    Text(
        text = message ?: "Không tải được preset",
        fontSize = 13.sp,
        color = AppColors.TextSecondary
    )
}

/**
 * Empty placeholder when no saved meals exist.
 */
@Composable
private fun EmptyState() {
    Text(
        text = "Chưa có preset nào",
        fontSize = 13.sp,
        color = AppColors.TextSecondary
    )
}

/**
 * Card row for a saved meal preset.
 */
@Composable
private fun SavedMealCard(
    meal: FamilySavedMealCardUi,
    canApply: Boolean,
    isApplying: Boolean,
    onApply: () -> Unit,
    onShowDetail: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = meal.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa preset",
                        tint = AppColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${meal.itemsCount} món",
                fontSize = 12.sp,
                color = AppColors.TextSecondary
            )

            if (!meal.createdByName.isNullOrBlank() || !meal.createdAtLabel.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                val info = listOfNotNull(meal.createdByName, meal.createdAtLabel)
                Text(
                    text = info.joinToString(" • "),
                    fontSize = 11.sp,
                    color = AppColors.GrayPlaceholder
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onShowDetail) {
                    Text(text = "Chi tiết", color = AppColors.TextSecondary)
                }
                if (canApply) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = onApply,
                        enabled = !isApplying,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = if (isApplying) "Đang áp dụng..." else "Áp dụng")
                    }
                }
            }
        }
    }
}

/**
 * Detail dialog showing items in a saved meal preset.
 */
@Composable
private fun SavedMealDetailDialog(
    state: FamilySavedMealsState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = state.detail?.name ?: "Chi tiết preset")
        },
        text = {
            when {
                state.isDetailLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Orange, strokeWidth = 2.dp)
                    }
                }
                state.detailErrorMessage != null -> {
                    Text(
                        text = state.detailErrorMessage.orEmpty(),
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                }
                state.detail != null -> {
                    SavedMealDetailContent(detail = state.detail)
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
 * Detail content for preset items.
 */
@Composable
private fun SavedMealDetailContent(detail: FamilySavedMealDetailUi) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        detail.items.forEach { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Background, RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )
                if (!item.subtitle.isNullOrBlank()) {
                    Text(
                        text = item.subtitle.orEmpty(),
                        fontSize = 11.sp,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Delete confirmation dialog for presets.
 */
@Composable
private fun SavedMealDeleteDialog(
    state: FamilySavedMealsState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Xóa preset")
        },
        text = {
            Column {
                Text(
                    text = "Bạn chắc chắn muốn xóa preset này?",
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
                if (!state.deleteErrorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.deleteErrorMessage.orEmpty(),
                        fontSize = 12.sp,
                        color = AppColors.Error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !state.isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange)
            ) {
                Text(text = if (state.isDeleting) "Đang xóa..." else "Xóa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Hủy")
            }
        }
    )
}
