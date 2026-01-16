package com.foodfest.app.features.blindbox.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.features.blindbox.presentation.BlindBoxViewModel
import com.foodfest.app.features.blindbox.presentation.models.DishUI
import com.foodfest.app.features.blindbox.presentation.models.TagCategory
import com.foodfest.app.features.blindbox.presentation.models.TagUI
import com.foodfest.app.theme.AppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishSelectionSheet(
    viewModel: BlindBoxViewModel,
    wheelItems: List<DishUI>,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filterScrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Chọn món ăn",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Brown,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Filter tags section - scrollable với height giới hạn
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(filterScrollState)
            ) {
                Text(
                    "Lọc theo tag:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Type tags
                TagSection(
                    title = "Loại món:",
                    tags = viewModel.tags.filter { it.category == TagCategory.TYPE },
                    onTagClick = { tag ->
                        viewModel.toggleTag(tag)
                        scope.launch {
                            viewModel.loadDishPageWithFilter(1, viewModel.tags, "")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Taste tags
                TagSection(
                    title = "Vị:",
                    tags = viewModel.tags.filter { it.category == TagCategory.TASTE },
                    onTagClick = { tag ->
                        viewModel.toggleTag(tag)
                        scope.launch {
                            viewModel.loadDishPageWithFilter(1, viewModel.tags, "")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Ingredient tags
                TagSection(
                    title = "Nguyên liệu:",
                    tags = viewModel.tags.filter { it.category == TagCategory.INGREDIENT },
                    onTagClick = { tag ->
                        viewModel.toggleTag(tag)
                        scope.launch {
                            viewModel.loadDishPageWithFilter(1, viewModel.tags, "")
                        }
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = AppColors.GrayPlaceholder.copy(alpha = 0.3f)
            )

            // Selected dishes count
            if (wheelItems.isNotEmpty()) {
                Text(
                    "Đã chọn: ${wheelItems.size} món",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Orange,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Dishes list
            Box(modifier = Modifier.weight(1f)) {
                if (viewModel.isLoadingDishes) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AppColors.Orange
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(viewModel.allDishes) { dish ->
                            DishSelectionItem(
                                dish = dish,
                                isOnWheel = wheelItems.any { it.id == dish.id },
                                onToggle = {
                                    if (wheelItems.any { it.id == dish.id }) {
                                        viewModel.removeDishFromWheel(dish)
                                    } else {
                                        viewModel.addDishToWheel(dish)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Pagination
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (viewModel.currentDishPage > 1) {
                            scope.launch {
                                viewModel.loadDishPageWithFilter(
                                    viewModel.currentDishPage - 1,
                                    viewModel.tags,
                                    ""
                                )
                            }
                        }
                    },
                    enabled = viewModel.currentDishPage > 1
                ) {
                    Icon(Icons.Default.KeyboardArrowLeft, "Previous")
                }

                Text(
                    "Trang ${viewModel.currentDishPage} / ${viewModel.totalDishPages}",
                    fontSize = 16.sp
                )

                IconButton(
                    onClick = {
                        if (viewModel.currentDishPage < viewModel.totalDishPages) {
                            scope.launch {
                                viewModel.loadDishPageWithFilter(
                                    viewModel.currentDishPage + 1,
                                    viewModel.tags,
                                    ""
                                )
                            }
                        }
                    },
                    enabled = viewModel.currentDishPage < viewModel.totalDishPages
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, "Next")
                }
            }
        }
    }
}

@Composable
private fun TagSection(
    title: String,
    tags: List<TagUI>,
    onTagClick: (TagUI) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Custom FlowRow để wrap tags xuống dòng
        CustomFlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalSpacing = 8.dp,
            verticalSpacing = 8.dp
        ) {
            tags.forEach { tag ->
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .background(
                            color = if (tag.isSelected) AppColors.Orange else AppColors.White,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onTagClick(tag) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        tag.name,
                        color = if (tag.isSelected) AppColors.White else AppColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = if (tag.isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomFlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    verticalSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val horizontalSpacingPx = horizontalSpacing.roundToPx()
        val verticalSpacingPx = verticalSpacing.roundToPx()
        
        // Quan trọng: set minWidth = 0 để children wrap content thay vì fill
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        
        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0
        
        measurables.forEach { measurable ->
            val placeable = measurable.measure(childConstraints)
            
            if (currentRowWidth + placeable.width + 
                (if (currentRow.isNotEmpty()) horizontalSpacingPx else 0) > constraints.maxWidth) {
                // Start new row
                if (currentRow.isNotEmpty()) {
                    rows.add(currentRow)
                    currentRow = mutableListOf()
                    currentRowWidth = 0
                }
            }
            
            currentRow.add(placeable)
            currentRowWidth += placeable.width + 
                (if (currentRow.size > 1) horizontalSpacingPx else 0)
        }
        
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }
        
        val totalHeight = rows.sumOf { row ->
            row.maxOfOrNull { it.height } ?: 0
        } + (rows.size - 1) * verticalSpacingPx
        
        layout(constraints.maxWidth, totalHeight) {
            var yPosition = 0
            
            rows.forEach { row ->
                var xPosition = 0
                val rowHeight = row.maxOfOrNull { it.height } ?: 0
                
                row.forEach { placeable ->
                    placeable.placeRelative(xPosition, yPosition)
                    xPosition += placeable.width + horizontalSpacingPx
                }
                
                yPosition += rowHeight + verticalSpacingPx
            }
        }
    }
}
