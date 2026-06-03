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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.components.FoodFestTextField
import com.foodfest.app.features.family.data.FamilyShoppingListActivity
import com.foodfest.app.features.family.data.FamilyShoppingListItem
import com.foodfest.app.features.family.presentation.models.FamilyShoppingListState
import com.foodfest.app.theme.AppColors
import kotlinx.coroutines.delay

/**
 * Phase 4.3/4.4 screen for generated shopping list and cooking-prep checklist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyShoppingListScreen(
    familyId: Int,
    shoppingListId: Int,
    currentUserId: Int?,
    viewModel: FamilyShoppingListViewModel = remember { FamilyShoppingListViewModel() },
    onBack: () -> Unit
) {
    val state = viewModel.state

    LaunchedEffect(familyId, shoppingListId) {
        viewModel.loadShoppingList(familyId, shoppingListId)
    }

    // Phase 5 MVP: poll every 5s so checklist changes from other family members appear without WebSocket yet.
    LaunchedEffect(familyId, shoppingListId) {
        while (familyId > 0 && shoppingListId > 0) {
            delay(5000)
            viewModel.loadShoppingList(familyId, shoppingListId, quiet = true)
        }
    }

    if (state.showSyncPantryDialog) {
        SyncPantryDialog(
            purchasedCount = state.items.count { it.isPurchased },
            onDismiss = { viewModel.showSyncPantryDialog(false) },
            onConfirm = { viewModel.syncPantry(familyId, shoppingListId) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Shopping List",
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Brown
                        )
                        Text(
                            text = state.listTitle.ifBlank { "Checklist mua sắm" },
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Orange)
                }
            }
            else -> {
                ShoppingListContent(
                    state = state,
                    currentUserId = currentUserId,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    onTogglePurchased = { item, checked ->
                        viewModel.togglePurchased(familyId, shoppingListId, item.id, checked)
                    },
                    onAssignToMe = { item ->
                        viewModel.assignToMe(familyId, shoppingListId, item.id, currentUserId)
                    },
                    onUsedQtyChange = viewModel::updateUsedQtyDraft,
                    onSaveUsedQty = { item ->
                        viewModel.saveUsedQty(familyId, shoppingListId, item.id)
                    },
                    onShare = viewModel::shareListMessage,
                    onMarkAllPurchased = { viewModel.markAllPurchased(familyId, shoppingListId) },
                    onSyncPantry = { viewModel.showSyncPantryDialog(true) }
                )
            }
        }
    }
}

@Composable
private fun ShoppingListContent(
    state: FamilyShoppingListState,
    currentUserId: Int?,
    modifier: Modifier,
    onTogglePurchased: (FamilyShoppingListItem, Boolean) -> Unit,
    onAssignToMe: (FamilyShoppingListItem) -> Unit,
    onUsedQtyChange: (Int, String) -> Unit,
    onSaveUsedQty: (FamilyShoppingListItem) -> Unit,
    onShare: () -> Unit,
    onMarkAllPurchased: () -> Unit,
    onSyncPantry: () -> Unit
) {
    val grouped = state.items.groupBy { it.category }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ShoppingActions(
                state = state,
                onShare = onShare,
                onMarkAllPurchased = onMarkAllPurchased,
                onSyncPantry = onSyncPantry
            )
        }

        if (!state.errorMessage.isNullOrBlank()) {
            item {
                Text(text = state.errorMessage.orEmpty(), color = AppColors.Error, fontSize = 12.sp)
            }
        }
        if (!state.actionMessage.isNullOrBlank()) {
            item {
                Text(text = state.actionMessage.orEmpty(), color = AppColors.TextSecondary, fontSize = 12.sp)
            }
        }

        if (state.items.isEmpty()) {
            item {
                Text(
                    text = "Không có item cần mua. Pantry có thể đã đủ cho menu tuần này.",
                    color = AppColors.TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        grouped.forEach { (category, itemsInCategory) ->
            item {
                Text(
                    text = category,
                    color = AppColors.Brown,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
            items(itemsInCategory, key = { it.id }) { item ->
                ShoppingItemCard(
                    item = item,
                    currentUserId = currentUserId,
                    usedQtyDraft = state.usedQtyDrafts[item.id].orEmpty(),
                    onTogglePurchased = { checked -> onTogglePurchased(item, checked) },
                    onAssignToMe = { onAssignToMe(item) },
                    onUsedQtyChange = { value -> onUsedQtyChange(item.id, value) },
                    onSaveUsedQty = { onSaveUsedQty(item) }
                )
            }
        }

        item {
            ActivityLogCard(activityLog = state.activityLog)
        }
    }
}

@Composable
private fun ShoppingActions(
    state: FamilyShoppingListState,
    onShare: () -> Unit,
    onMarkAllPurchased: () -> Unit,
    onSyncPantry: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Tiến độ: ${state.items.count { it.isPurchased }}/${state.items.size} đã mua",
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Tự đồng bộ checklist mỗi 5 giây",
                color = AppColors.TextSecondary,
                fontSize = 12.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Background)
                ) {
                    Text(text = "Chia sẻ", color = AppColors.TextPrimary)
                }
                Button(
                    onClick = onMarkAllPurchased,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brown)
                ) {
                    Text(text = "Mua hết")
                }
            }
            Button(
                onClick = onSyncPantry,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving && state.items.any { it.isPurchased },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange)
            ) {
                Text(text = "Cập nhật Pantry")
            }
        }
    }
}

@Composable
private fun ShoppingItemCard(
    item: FamilyShoppingListItem,
    currentUserId: Int?,
    usedQtyDraft: String,
    onTogglePurchased: (Boolean) -> Unit,
    onAssignToMe: () -> Unit,
    onUsedQtyChange: (String) -> Unit,
    onSaveUsedQty: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.isPurchased, onCheckedChange = onTogglePurchased)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.ingredientName,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary,
                        textDecoration = if (item.isPurchased) TextDecoration.LineThrough else null
                    )
                    Text(
                        text = "${item.requiredQty.formatQty()} ${item.unit.orEmpty()}".trim(),
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                }
            }

            if (!item.note.isNullOrBlank()) {
                Text(text = item.note.orEmpty(), fontSize = 12.sp, color = AppColors.TextSecondary)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Assigned: ${item.assignedToName ?: "Chưa phân công"}",
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
                if (currentUserId != null && item.assignedToUserId != currentUserId) {
                    TextButton(onClick = onAssignToMe) {
                        Text(text = "Tôi mua")
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FoodFestTextField(
                    value = usedQtyDraft,
                    onValueChange = onUsedQtyChange,
                    placeholder = "Đã dùng bao nhiêu?",
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onSaveUsedQty,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brown)
                ) {
                    Text(text = "Lưu")
                }
            }
        }
    }
}

@Composable
private fun ActivityLogCard(activityLog: List<FamilyShoppingListActivity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Activity log",
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (activityLog.isEmpty()) {
                Text(text = "Chưa có hoạt động", fontSize = 12.sp, color = AppColors.TextSecondary)
            } else {
                activityLog.take(8).forEach { entry ->
                    Text(
                        text = "${entry.actorName}: ${entry.message}",
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun SyncPantryDialog(
    purchasedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Cập nhật Pantry") },
        text = {
            Text(
                text = "Thêm $purchasedCount item đã mua vào Pantry với số lượng trong shopping list?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange)
            ) {
                Text(text = "Cập nhật")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Hủy")
            }
        }
    )
}

private fun Double.formatQty(): String {
    return if (this % 1.0 == 0.0) toInt().toString() else toString()
}
