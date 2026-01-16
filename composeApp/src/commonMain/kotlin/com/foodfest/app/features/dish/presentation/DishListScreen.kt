package com.foodfest.app.features.dish.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.common.filter.*
import com.foodfest.app.features.dish.data.Dish
import com.foodfest.app.features.dish.data.DishRepository
import com.foodfest.app.features.dish.presentation.components.*
import com.foodfest.app.features.tag.data.TagRepository
import com.foodfest.app.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun DishListScreen() {
    val dishRepo = remember { DishRepository() }
    val tagRepo = remember { TagRepository() }
    val scope = rememberCoroutineScope()
    
    var dishes by remember { mutableStateOf<List<Dish>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var totalItems by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var dishFilter by remember { mutableStateOf(DishFilter()) }
    var selectedDish by remember { mutableStateOf<Dish?>(null) }
    
    // Hiển thị detail nếu đã chọn dish
    if (selectedDish != null) {
        DishDetailScreen(
            dish = selectedDish!!,
            onBack = { selectedDish = null }
        )
        return
    }
    
    // Load tags khi khởi động
    LaunchedEffect(Unit) {
        tagRepo.getAllTags()
            .onSuccess { tags ->
                val typeTags = tags
                    .filter { it.type.uppercase() == "TYPE" }
                    .map { it.toFilterTag(FilterCategory.TYPE) }
                val tasteTags = tags
                    .filter { it.type.uppercase() == "TASTE" }
                    .map { it.toFilterTag(FilterCategory.TASTE) }
                val ingredientTags = tags
                    .filter { it.type.uppercase() == "INGREDIENT" }
                    .map { it.toFilterTag(FilterCategory.INGREDIENT) }
                
                dishFilter = DishFilter(
                    typeTags = typeTags,
                    tasteTags = tasteTags,
                    ingredientTags = ingredientTags
                )
            }
    }
    
    fun loadDishes(page: Int = 1) {
        scope.launch {
            isLoading = true
            errorMessage = null
            
            val selectedTypeTags = dishFilter.typeTags
                .filter { it.isSelected }
                .map { it.name }
            val selectedTasteTags = dishFilter.tasteTags
                .filter { it.isSelected }
                .map { it.name }
            val selectedIngredientTags = dishFilter.ingredientTags
                .filter { it.isSelected }
                .map { it.name }
            
            if (dishFilter.hasSelectedTags) {
                dishRepo.getDishesWithFilter(
                    page = page,
                    typeTags = selectedTypeTags,
                    tasteTags = selectedTasteTags,
                    ingredientTags = selectedIngredientTags
                )
            } else {
                dishRepo.getDishes(page = page)
            }.onSuccess { response ->
                dishes = response.data
                currentPage = response.page
                totalPages = (response.total + response.limit - 1) / response.limit
                totalItems = response.total
            }.onFailure { error ->
                errorMessage = error.message ?: "Không tải được danh sách món"
            }
            
            isLoading = false
        }
    }
    
    LaunchedEffect(Unit) {
        loadDishes(1)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        // Search Bar và Filter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
              SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Tìm kiếm món ăn..."
            )
            
            Spacer(Modifier.height(16.dp))
            
            FilterButton(
                selectedCount = dishFilter.selectedCount,
                onClick = { showFilterSheet = true }
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Kết quả tìm kiếm
        Text(
            text = "$totalItems kết quả",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.GrayPlaceholder,
            fontSize = 18.sp
        )
        
        // Content
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Orange)
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage ?: "Có lỗi xảy ra",
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { loadDishes(currentPage) }) {
                            Text("Thử lại")
                        }
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(dishes) { dish ->
                        DishCard(
                            dish = dish,
                            onClick = { selectedDish = dish }
                        )
                    }
                }
                
                // Pagination
                if (totalPages > 1) {
                    PaginationControls(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        onPreviousPage = { 
                            if (currentPage > 1) loadDishes(currentPage - 1) 
                        },
                        onNextPage = { 
                            if (currentPage < totalPages) loadDishes(currentPage + 1) 
                        }
                    )
                }
            }
        }
        
        // Filter Bottom Sheet
        if (showFilterSheet) {
            FilterBottomSheet(
                filter = dishFilter,
                onFilterChange = { dishFilter = it },
                onApplyFilter = { loadDishes(1) },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}
