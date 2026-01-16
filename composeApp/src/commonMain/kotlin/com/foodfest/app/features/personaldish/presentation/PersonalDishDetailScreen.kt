package com.foodfest.app.features.personaldish.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.foodfest.app.features.dish.presentation.components.*
import com.foodfest.app.features.personaldish.data.PersonalDish
import com.foodfest.app.features.personaldish.data.PersonalDishRepository
import com.foodfest.app.features.personaldish.data.UpdatePersonalDishRequest
import com.foodfest.app.theme.AppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDishDetailScreen(
    dish: PersonalDish,
    onBack: () -> Unit,
    onDeleted: () -> Unit = {}
) {
    val repository = remember { PersonalDishRepository() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var currentDish by remember { mutableStateOf(dish) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    
    // Edit fields
    var editIngredients by remember { mutableStateOf(dish.ingredients ?: "") }
    var editInstructions by remember { mutableStateOf(dish.instructions ?: "") }
    var editNote by remember { mutableStateOf(dish.note ?: "") }
    
    fun updateDish() {
        scope.launch {
            isUpdating = true
            val request = UpdatePersonalDishRequest(
                ingredients = editIngredients.ifBlank { null },
                instructions = editInstructions.ifBlank { null },
                note = editNote.ifBlank { null }
            )
            repository.update(currentDish.id, request)
                .onSuccess { updated ->
                    currentDish = updated
                    showEditDialog = false
                    snackbarHostState.showSnackbar("Đã cập nhật công thức! ✅")
                }
                .onFailure { e ->
                    snackbarHostState.showSnackbar(e.message ?: "Lỗi khi cập nhật")
                }
            isUpdating = false
        }
    }
    
    fun deleteDish() {
        scope.launch {
            isDeleting = true
            repository.delete(currentDish.id)
                .onSuccess {
                    showDeleteDialog = false
                    snackbarHostState.showSnackbar("Đã xóa món ăn")
                    onDeleted()
                }
                .onFailure { e ->
                    snackbarHostState.showSnackbar(e.message ?: "Lỗi khi xóa")
                }
            isDeleting = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
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
                    // Edit button
                    IconButton(onClick = { 
                        editIngredients = currentDish.ingredients ?: ""
                        editInstructions = currentDish.instructions ?: ""
                        editNote = currentDish.note ?: ""
                        showEditDialog = true 
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Chỉnh sửa",
                            tint = AppColors.Brown
                        )
                    }
                    
                    // Delete button
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xóa",
                            tint = AppColors.Brown
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppColors.Background
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Dish Image
            DishImagePlaceholder(
                dishName = currentDish.dishName,
                imageUrl = currentDish.imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            
            Spacer(Modifier.height(20.dp))
            
            // Dish Name
            Text(
                text = currentDish.dishName,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Brown,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            // "Món của tôi" badge
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Orange.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AppColors.Orange
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Món ăn của tôi",
                        fontSize = 14.sp,
                        color = AppColors.Orange,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Info Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    icon = Icons.Default.Timer,
                    label = "Chuẩn bị",
                    value = if (currentDish.prepTime != null) "${currentDish.prepTime} phút" else "—",
                    modifier = Modifier.weight(1f)
                )
                
                InfoCard(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Nấu",
                    value = if (currentDish.cookTime != null) "${currentDish.cookTime} phút" else "—",
                    modifier = Modifier.weight(1f)
                )
                
                InfoCard(
                    icon = Icons.Default.People,
                    label = "Khẩu phần",
                    value = if (currentDish.serving != null) "${currentDish.serving} người" else "—",
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Note (if any)
            if (!currentDish.note.isNullOrBlank()) {
                SectionCard(
                    title = "Ghi chú của tôi",
                    icon = Icons.Default.Notes
                ) {
                    Text(
                        text = currentDish.note!!,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(Modifier.height(16.dp))
            }
            
            // Description
            if (!currentDish.description.isNullOrBlank()) {
                SectionCard(
                    title = "Mô tả",
                    icon = Icons.Default.Info
                ) {
                    Text(
                        text = currentDish.description!!,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = AppColors.TextPrimary
                    )
                }
                
                Spacer(Modifier.height(16.dp))
            }
            
            // Ingredients
            if (!currentDish.ingredients.isNullOrBlank()) {
                SectionCard(
                    title = "Nguyên liệu",
                    icon = Icons.Default.ShoppingCart
                ) {
                    IngredientsList(ingredients = currentDish.ingredients!!)
                }
                
                Spacer(Modifier.height(16.dp))
            }
            
            // Instructions
            if (!currentDish.instructions.isNullOrBlank()) {
                SectionCard(
                    title = "Hướng dẫn",
                    icon = Icons.Default.MenuBook
                ) {
                    InstructionsList(instructions = currentDish.instructions!!)
                }
                
                Spacer(Modifier.height(16.dp))
            }
            
            // Bottom spacing
            Spacer(Modifier.height(32.dp))
        }
    }
    
    // Edit Dialog
    if (showEditDialog) {
        Dialog(
            onDismissRequest = { if (!isUpdating) showEditDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chỉnh sửa công thức",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Brown
                        )
                        IconButton(
                            onClick = { if (!isUpdating) showEditDialog = false }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = AppColors.Brown
                            )
                        }
                    }
                    
                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(Modifier.height(16.dp))
                        
                        // Ingredients field
                        Text(
                            text = "Nguyên liệu",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Brown,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = editIngredients,
                            onValueChange = { editIngredients = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            placeholder = { Text("Nhập nguyên liệu...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColors.Orange,
                                cursorColor = AppColors.Orange
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Instructions field
                        Text(
                            text = "Cách nấu",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Brown,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = editInstructions,
                            onValueChange = { editInstructions = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 150.dp),
                            placeholder = { Text("Nhập hướng dẫn nấu...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColors.Orange,
                                cursorColor = AppColors.Orange
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Note field
                        Text(
                            text = "Ghi chú của tôi",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Brown,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = editNote,
                            onValueChange = { editNote = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp),
                            placeholder = { Text("Mẹo nấu, thay đổi khẩu phần...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColors.Orange,
                                cursorColor = AppColors.Orange
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditDialog = false },
                            enabled = !isUpdating,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AppColors.Brown
                            )
                        ) {
                            Text("Hủy")
                        }
                        
                        Button(
                            onClick = { updateDish() },
                            enabled = !isUpdating,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Orange
                            )
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = AppColors.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Lưu thay đổi")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { 
                Text(
                    "Xóa món ăn?",
                    color = AppColors.Brown,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Text(
                    "Bạn có chắc muốn xóa \"${currentDish.dishName}\" khỏi danh sách món của mình?",
                    color = AppColors.TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = { deleteDish() },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AppColors.White
                        )
                    } else {
                        Text("Xóa")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Hủy", color = AppColors.Brown)
                }
            },
            containerColor = AppColors.Background
        )
    }
}
