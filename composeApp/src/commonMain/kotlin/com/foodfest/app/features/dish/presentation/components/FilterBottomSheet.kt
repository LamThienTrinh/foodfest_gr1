package com.foodfest.app.features.dish.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.features.dish.presentation.models.DishFilter
import com.foodfest.app.features.dish.presentation.models.FilterCategory
import com.foodfest.app.features.dish.presentation.models.FilterTag
import com.foodfest.app.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    filter: DishFilter,
    onFilterChange: (DishFilter) -> Unit,
    onApplyFilter: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.75f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bộ lọc",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Brown
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = AppColors.GrayPlaceholder
                    )
                }
            }

            // Filter content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // Loại món
                FilterSection(
                    title = "Loại món",
                    tags = filter.typeTags,
                    onTagClick = { tag ->
                        val updatedTags = filter.typeTags.map {
                            if (it.id == tag.id) it.copy(isSelected = !it.isSelected)
                            else it
                        }
                        onFilterChange(filter.copy(typeTags = updatedTags))
                    }
                )
                
                Spacer(Modifier.height(20.dp))
                
                // Vị
                FilterSection(
                    title = "Vị",
                    tags = filter.tasteTags,
                    onTagClick = { tag ->
                        val updatedTags = filter.tasteTags.map {
                            if (it.id == tag.id) it.copy(isSelected = !it.isSelected)
                            else it
                        }
                        onFilterChange(filter.copy(tasteTags = updatedTags))
                    }
                )
                
                Spacer(Modifier.height(20.dp))
                
                // Nguyên liệu
                FilterSection(
                    title = "Nguyên liệu",
                    tags = filter.ingredientTags,
                    onTagClick = { tag ->
                        val updatedTags = filter.ingredientTags.map {
                            if (it.id == tag.id) it.copy(isSelected = !it.isSelected)
                            else it
                        }
                        onFilterChange(filter.copy(ingredientTags = updatedTags))
                    }
                )
                
                Spacer(Modifier.height(20.dp))
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Clear button
                OutlinedButton(
                    onClick = {
                        val clearedFilter = DishFilter(
                            typeTags = filter.typeTags.map { it.copy(isSelected = false) },
                            tasteTags = filter.tasteTags.map { it.copy(isSelected = false) },
                            ingredientTags = filter.ingredientTags.map { it.copy(isSelected = false) }
                        )
                        onFilterChange(clearedFilter)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.Brown
                    )
                ) {
                    Text("Xóa bộ lọc", fontWeight = FontWeight.Medium)
                }
                
                // Apply button
                Button(
                    onClick = {
                        onApplyFilter()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Orange
                    )
                ) {
                    Text(
                        "Áp dụng (${filter.selectedCount})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    tags: List<FilterTag>,
    onTagClick: (FilterTag) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Custom FlowRow để wrap tags xuống dòng
        CustomFlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalSpacing = 8.dp,
            verticalSpacing = 8.dp
        ) {
            tags.forEach { tag ->
                FilterTagItem(
                    tag = tag,
                    onTagClick = onTagClick
                )
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
