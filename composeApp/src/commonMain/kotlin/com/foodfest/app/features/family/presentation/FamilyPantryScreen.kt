package com.foodfest.app.features.family.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import com.foodfest.app.components.FoodFestTextField
import com.foodfest.app.features.family.data.FamilyPantryItem
import com.foodfest.app.features.family.presentation.models.FamilyPantryState
import com.foodfest.app.theme.AppColors
import kotlin.time.ExperimentalTime
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyPantryScreen(
    familyId: Int,
    viewModel: FamilyPantryViewModel = remember { FamilyPantryViewModel() },
    onBack: () -> Unit
) {
    val state = viewModel.state
    val filteredItems = viewModel.filteredItems()
    var bulkExpiryPickerItemId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(familyId) {
        viewModel.loadPantry(familyId)
    }

    if (state.showAddDialog) {
        PantryAddDialog(
            state = state,
            onDismiss = viewModel::hideAddDialog,
            onIngredientNameChange = viewModel::updateAddIngredientName,
            onQuantityChange = viewModel::updateAddQuantity,
            onUnitChange = viewModel::updateAddUnit,
            onExpiryDateChange = viewModel::updateAddExpiryDate,
            onSubmit = { viewModel.submitAddItem(familyId) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pantry / Tủ lạnh",
                        color = AppColors.Brown,
                        fontWeight = FontWeight.Bold
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Phase 4.1: Search / Filter pantry items.
            FoodFestTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = "Tìm theo tên nguyên liệu, đơn vị, ngày hết hạn"
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = viewModel::showAddDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Thêm nguyên liệu")
                }
                Button(
                    onClick = {
                        if (state.isBulkEditMode) viewModel.saveBulkEdits(familyId)
                        else viewModel.toggleBulkEditMode()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brown),
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSavingBulk
                ) {
                    Text(text = if (state.isBulkEditMode) "Lưu chỉnh sửa" else "📝 Chỉnh sửa")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.deleteExpiredItems(familyId) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error),
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isDeletingExpired
            ) {
                Text(
                    text = if (state.isDeletingExpired) "Đang xóa..." else "🗑️ Xóa hết hạn"
                )
            }

            if (!state.actionMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = state.actionMessage.orEmpty(),
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
            }
            if (!state.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.errorMessage.orEmpty(),
                    fontSize = 12.sp,
                    color = AppColors.Error
                )
            }
            if (!state.bulkEditError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.bulkEditError.orEmpty(),
                    fontSize = 12.sp,
                    color = AppColors.Error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Orange)
                    }
                }
                filteredItems.isEmpty() -> {
                    Text(
                        text = "Chưa có nguyên liệu nào",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            PantryItemCard(
                                item = item,
                                state = state,
                                onIngredientNameChange = viewModel::updateBulkIngredientName,
                                onQuantityChange = viewModel::updateBulkQuantity,
                                onUnitChange = viewModel::updateBulkUnit,
                                onExpiryDateChange = viewModel::updateBulkExpiryDate,
                                onPickExpiryDate = { bulkExpiryPickerItemId = item.id }
                            )
                        }
                    }
                }
            }
        }
    }

    val pickerItemId = bulkExpiryPickerItemId
    val pickerDraft = pickerItemId?.let { state.bulkDrafts[it] }
    if (pickerItemId != null && pickerDraft != null) {
        PantryExpiryDatePickerDialog(
            initialDate = pickerDraft.expiryDate,
            onDismiss = { bulkExpiryPickerItemId = null },
            onDateSelected = { selectedDate ->
                viewModel.updateBulkExpiryDate(pickerItemId, selectedDate)
                bulkExpiryPickerItemId = null
            }
        )
    }
}

@Composable
private fun PantryItemCard(
    item: FamilyPantryItem,
    state: FamilyPantryState,
    onIngredientNameChange: (Int, String) -> Unit,
    onQuantityChange: (Int, String) -> Unit,
    onUnitChange: (Int, String) -> Unit,
    onExpiryDateChange: (Int, String) -> Unit,
    onPickExpiryDate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        if (state.isBulkEditMode) {
            val draft = state.bulkDrafts[item.id]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FoodFestTextField(
                    value = draft?.ingredientName ?: item.ingredientName,
                    onValueChange = { onIngredientNameChange(item.id, it) },
                    placeholder = "Tên nguyên liệu"
                )
                FoodFestTextField(
                    value = draft?.quantity ?: item.quantity.toString(),
                    onValueChange = { onQuantityChange(item.id, it) },
                    placeholder = "Số lượng"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FoodFestTextField(
                        value = draft?.unit ?: item.unit.orEmpty(),
                        onValueChange = { onUnitChange(item.id, it) },
                        placeholder = "Đơn vị",
                        modifier = Modifier.weight(1f)
                    )
                    ExpiryDateField(
                        value = draft?.expiryDate ?: item.expiryDate.orEmpty(),
                        placeholder = "Hết hạn",
                        onClick = onPickExpiryDate,
                        onClear = { onExpiryDateChange(item.id, "") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = item.ingredientName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append("Số lượng: ${item.quantity}")
                        if (!item.unit.isNullOrBlank()) append(" ${item.unit}")
                    },
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary
                )
                Text(
                    text = "Hết hạn: ${item.expiryDate ?: "Chưa cập nhật"}",
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun PantryAddDialog(
    state: FamilyPantryState,
    onDismiss: () -> Unit,
    onIngredientNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onExpiryDateChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    var showExpiryPicker by remember { mutableStateOf(false) }

    if (showExpiryPicker) {
        PantryExpiryDatePickerDialog(
            initialDate = state.addForm.expiryDate,
            onDismiss = { showExpiryPicker = false },
            onDateSelected = { selectedDate ->
                onExpiryDateChange(selectedDate)
                showExpiryPicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Thêm nguyên liệu")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FoodFestTextField(
                    value = state.addForm.ingredientName,
                    onValueChange = onIngredientNameChange,
                    placeholder = "Tên nguyên liệu"
                )
                FoodFestTextField(
                    value = state.addForm.quantity,
                    onValueChange = onQuantityChange,
                    placeholder = "Số lượng"
                )
                FoodFestTextField(
                    value = state.addForm.unit,
                    onValueChange = onUnitChange,
                    placeholder = "Đơn vị (vd: g, kg, bó)"
                )
                ExpiryDateField(
                    value = state.addForm.expiryDate,
                    placeholder = "Chọn ngày hết hạn",
                    onClick = { showExpiryPicker = true },
                    onClear = { onExpiryDateChange("") }
                )
                if (!state.addFormError.isNullOrBlank()) {
                    Text(
                        text = state.addFormError.orEmpty(),
                        fontSize = 12.sp,
                        color = AppColors.Error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = !state.isSubmittingAdd,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange)
            ) {
                Text(text = if (state.isSubmittingAdd) "Đang lưu..." else "Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Hủy")
            }
        }
    )
}

@Composable
private fun ExpiryDateField(
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = value.ifBlank { placeholder },
                color = if (value.isBlank()) AppColors.TextSecondary else AppColors.TextPrimary,
                fontSize = 13.sp
            )
        }
        if (value.isNotBlank()) {
            TextButton(onClick = onClear) {
                Text(text = "Xóa", color = AppColors.Error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantryExpiryDatePickerDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toEpochMillisOrNull()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(millis.toIsoDate())
                    } ?: onDismiss()
                }
            ) {
                Text(text = "Chọn")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Hủy")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalTime::class)
private fun Long.toIsoDate(): String {
    return Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.UTC)
        .date
        .toString()
}

@OptIn(ExperimentalTime::class)
private fun String.toEpochMillisOrNull(): Long? {
    if (isBlank()) return null
    return runCatching {
        LocalDate.parse(this).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    }.getOrNull()
}
