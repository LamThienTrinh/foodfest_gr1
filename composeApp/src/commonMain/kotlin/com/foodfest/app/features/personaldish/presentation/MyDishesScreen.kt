package com.foodfest.app.features.personaldish.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foodfest.app.common.filter.FilterBottomSheet
import com.foodfest.app.features.dish.presentation.components.PaginationControls
import com.foodfest.app.features.personaldish.presentation.components.*
import com.foodfest.app.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDishesScreen(
    viewModel: MyDishesViewModel = remember { MyDishesViewModel() },
    onBack: () -> Unit,
    onExplore: () -> Unit = {}
) {
    val state = viewModel.state
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Hiển thị detail nếu đã chọn dish
    if (state.selectedDish != null) {
        PersonalDishDetailScreen(
            dish = state.selectedDish,
            onBack = { viewModel.refreshAfterDetailClose() },
            onDeleted = { viewModel.onDishDeleted() }
        )
        return
    }
    
    // Load data on first composition
    LaunchedEffect(Unit) {
        viewModel.loadTags()
        viewModel.loadDishes(1)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Món ăn của tôi",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar và Filter
            MyDishesSearchAndFilter(
                searchQuery = state.searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                filterCount = state.dishFilter.selectedCount,
                onFilterClick = { viewModel.showFilterSheet(true) },
                modifier = Modifier.background(Color.White)
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Kết quả count
            DishesCountHeader(count = state.filteredDishes.size)
            
            // Content
            when {
                state.isLoading -> {
                    MyDishesLoadingState(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                state.errorMessage != null -> {
                    MyDishesErrorState(
                        message = state.errorMessage,
                        modifier = Modifier.fillMaxSize(),
                        onRetry = { viewModel.loadDishes(state.currentPage) }
                    )
                }
                state.filteredDishes.isEmpty() -> {
                    MyDishesEmptyState(
                        modifier = Modifier.fillMaxSize(),
                        onExplore = onExplore
                    )
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
                        items(state.filteredDishes) { dish ->
                            PersonalDishCard(
                                dish = dish,
                                onClick = { viewModel.selectDish(dish) }
                            )
                        }
                    }
                    
                    // Pagination
                    if (state.totalPages > 1) {
                        PaginationControls(
                            currentPage = state.currentPage,
                            totalPages = state.totalPages,
                            onPreviousPage = { viewModel.goToPreviousPage() },
                            onNextPage = { viewModel.goToNextPage() }
                        )
                    }
                }
            }
            
            // Filter Bottom Sheet
            if (state.showFilterSheet) {
                FilterBottomSheet(
                    filter = state.dishFilter,
                    onFilterChange = { viewModel.updateFilter(it) },
                    onApplyFilter = { /* Local filtering, no reload needed */ },
                    onDismiss = { viewModel.showFilterSheet(false) }
                )
            }
        }
    }
}
