package com.foodfest.app.features.favorite.presentation

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
import com.foodfest.app.features.dish.presentation.DishDetailScreen
import com.foodfest.app.features.dish.presentation.components.PaginationControls
import com.foodfest.app.features.favorite.presentation.components.*
import com.foodfest.app.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteDishesScreen(
    viewModel: FavoriteDishesViewModel = remember { FavoriteDishesViewModel() },
    onBack: () -> Unit,
    onExplore: () -> Unit = {}
) {
    val state = viewModel.state
    
    // Hiển thị detail nếu đã chọn dish
    if (state.selectedDishId != null) {
        DishDetailScreen(
            dishId = state.selectedDishId,
            onBack = { viewModel.selectDish(null) }
        )
        return
    }
    
    // Load data on first composition
    LaunchedEffect(Unit) {
        viewModel.loadTags()
        viewModel.loadFavorites(1)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Món yêu thích",
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
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar và Filter
            FavoritesSearchAndFilter(
                searchQuery = state.searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                filterCount = state.dishFilter.selectedCount,
                onFilterClick = { viewModel.showFilterSheet(true) },
                modifier = Modifier.background(Color.White)
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Kết quả count
            FavoritesCountHeader(count = state.filteredFavorites.size)
            
            // Content
            when {
                state.isLoading -> {
                    FavoritesLoadingState(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                state.errorMessage != null -> {
                    FavoritesErrorState(
                        message = state.errorMessage,
                        modifier = Modifier.fillMaxSize(),
                        onRetry = { viewModel.loadFavorites(state.currentPage) }
                    )
                }
                state.filteredFavorites.isEmpty() -> {
                    FavoritesEmptyState(
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
                        items(state.filteredFavorites) { fav ->
                            FavoriteDishCard(
                                favoriteDish = fav,
                                onClick = { viewModel.selectDish(fav.dishId) }
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
