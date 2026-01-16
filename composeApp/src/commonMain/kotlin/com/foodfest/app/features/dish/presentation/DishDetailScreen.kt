package com.foodfest.app.features.dish.presentation

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
import com.foodfest.app.features.dish.data.Dish
import com.foodfest.app.features.dish.data.DishRepository
import com.foodfest.app.features.dish.presentation.components.*
import com.foodfest.app.features.favorite.data.FavoriteRepository
import com.foodfest.app.features.personaldish.data.CreatePersonalDishRequest
import com.foodfest.app.features.personaldish.data.PersonalDishRepository
import com.foodfest.app.theme.AppColors
import kotlinx.coroutines.launch

/**
 * DishDetailScreen - Version nhận dishId và tự load data từ API
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishDetailScreen(
    dishId: Int,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = remember { DishRepository() }
    val favoriteRepository = remember { FavoriteRepository() }
    val personalDishRepository = remember { PersonalDishRepository() }
    
    var dish by remember { mutableStateOf<Dish?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Favorite state
    var isFavorite by remember { mutableStateOf(false) }
    var isFavoriteLoading by remember { mutableStateOf(false) }
    
    // Save state
    var hasSavedVersion by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }
    
    // Editable fields for saving
    var editIngredients by remember { mutableStateOf("") }
    var editInstructions by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }
    
    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load dish data
    LaunchedEffect(dishId) {
        isLoading = true
        error = null
        
        repository.getDishById(dishId)
            .onSuccess { loadedDish ->
                dish = loadedDish
                isLoading = false
                
                // Check favorite status
                favoriteRepository.isFavorite(dishId).onSuccess { isFav ->
                    isFavorite = isFav
                }
                
                // Check if already saved
                personalDishRepository.checkSaved(dishId).onSuccess { result ->
                    hasSavedVersion = result.hasSaved
                }
            }
            .onFailure { e ->
                error = e.message ?: "Lỗi không xác định"
                isLoading = false
            }
    }
    
    // Toggle favorite handler
    fun toggleFavorite() {
        if (isFavoriteLoading) return
        scope.launch {
            isFavoriteLoading = true
            favoriteRepository.toggleFavorite(dishId)
                .onSuccess { nowFavorite ->
                    isFavorite = nowFavorite
                    snackbarHostState.showSnackbar(
                        if (nowFavorite) "Đã thêm vào yêu thích ❤️" else "Đã bỏ yêu thích"
                    )
                }
                .onFailure { e ->
                    snackbarHostState.showSnackbar(e.message ?: "Lỗi")
                }
            isFavoriteLoading = false
        }
    }
    
    // Save handler with custom ingredients/instructions
    fun saveDish() {
        if (dish == null || isSaving) return
        scope.launch {
            isSaving = true
            val request = CreatePersonalDishRequest(
                originalDishId = dishId,
                dishName = dish!!.name,
                imageUrl = dish!!.imageUrl,
                description = dish!!.description,
                ingredients = editIngredients.ifBlank { dish!!.ingredients },
                instructions = editInstructions.ifBlank { dish!!.instructions },
                prepTime = dish!!.prepTime,
                cookTime = dish!!.cookTime,
                serving = dish!!.serving,
                note = editNote.ifBlank { null }
            )
            personalDishRepository.create(request)
                .onSuccess {
                    hasSavedVersion = true
                    saveSuccess = true
                    showSaveDialog = false
                    snackbarHostState.showSnackbar("Đã lưu món ăn của bạn! 🎉")
                }
                .onFailure { e ->
                    snackbarHostState.showSnackbar(e.message ?: "Lỗi khi lưu")
                }
            isSaving = false
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
                    // Save button (replace Share)
                    IconButton(
                        onClick = { 
                            if (!hasSavedVersion && dish != null) {
                                // Initialize editable fields with original data
                                editIngredients = dish?.ingredients ?: ""
                                editInstructions = dish?.instructions ?: ""
                                editNote = ""
                                showSaveDialog = true 
                            }
                        },
                        enabled = !hasSavedVersion
                    ) {
                        Icon(
                            imageVector = if (hasSavedVersion) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (hasSavedVersion) "Đã lưu" else "Lưu món của tôi",
                            tint = if (hasSavedVersion) AppColors.Orange else AppColors.Brown
                        )
                    }
                    
                    // Favorite button
                    IconButton(
                        onClick = { toggleFavorite() },
                        enabled = !isFavoriteLoading
                    ) {
                        if (isFavoriteLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = AppColors.Orange
                            )
                        } else {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorite) "Bỏ yêu thích" else "Yêu thích",
                                tint = if (isFavorite) AppColors.Orange else AppColors.Brown
                            )
                        }
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
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Orange)
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error ?: "Lỗi", color = AppColors.Brown)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onBack) {
                            Text("Quay lại")
                        }
                    }
                }
            }
            dish != null -> {
                DishDetailContent(
                    dish = dish!!,
                    paddingValues = paddingValues
                )
            }
        }
    }
    
    // Save with Edit dialog
    if (showSaveDialog && dish != null) {
        Dialog(
            onDismissRequest = { if (!isSaving) showSaveDialog = false },
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
                            text = "Lưu \"${dish!!.name}\"",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Brown,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { if (!isSaving) showSaveDialog = false }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = AppColors.Brown
                            )
                        }
                    }
                    
                    Text(
                        text = "Tùy chỉnh công thức theo cách của bạn trước khi lưu",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
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
                            text = "Ghi chú của bạn (tùy chọn)",
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
                            onClick = { showSaveDialog = false },
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AppColors.Brown
                            )
                        ) {
                            Text("Hủy")
                        }
                        
                        Button(
                            onClick = { saveDish() },
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Orange
                            )
                        ) {
                            if (isSaving) {
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
                                Text("Lưu món")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * DishDetailScreen - Version nhận Dish object trực tiếp (backward compatible)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishDetailScreen(
    dish: Dish,
    onBack: () -> Unit
) {
    // Use the dishId version for full functionality
    DishDetailScreen(
        dishId = dish.id,
        onBack = onBack
    )
}

@Composable
private fun DishDetailContent(
    dish: Dish,
    paddingValues: PaddingValues
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState)
    ) {
        // Dish Image
        DishImagePlaceholder(
            dishName = dish.name,
            imageUrl = dish.imageUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        
        Spacer(Modifier.height(20.dp))
        
        // Dish Name
        Text(
            text = dish.name,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.Brown,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
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
                value = if (dish.prepTime != null) "${dish.prepTime} phút" else "—",
                modifier = Modifier.weight(1f)
            )
            
            InfoCard(
                icon = Icons.Default.LocalFireDepartment,
                label = "Nấu",
                value = if (dish.cookTime != null) "${dish.cookTime} phút" else "—",
                modifier = Modifier.weight(1f)
            )
            
            InfoCard(
                icon = Icons.Default.People,
                label = "Khẩu phần",
                value = if (dish.serving != null) "${dish.serving} người" else "—",
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Description
        if (!dish.description.isNullOrBlank()) {
            SectionCard(
                title = "Mô tả",
                icon = Icons.Default.Info
            ) {
                Text(
                    text = dish.description,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = AppColors.TextPrimary
                )
            }
            
            Spacer(Modifier.height(16.dp))
        }
        
        // Ingredients
        if (!dish.ingredients.isNullOrBlank()) {
            SectionCard(
                title = "Nguyên liệu",
                icon = Icons.Default.ShoppingCart
            ) {
                IngredientsList(ingredients = dish.ingredients)
            }
            
            Spacer(Modifier.height(16.dp))
        }
        
        // Instructions
        if (!dish.instructions.isNullOrBlank()) {
            SectionCard(
                title = "Hướng dẫn",
                icon = Icons.Default.MenuBook
            ) {
                InstructionsList(instructions = dish.instructions)
            }
            
            Spacer(Modifier.height(16.dp))
        }
        
        // Bottom spacing
        Spacer(Modifier.height(32.dp))
    }
}
