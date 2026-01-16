package com.foodfest.app.common.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.theme.AppColors

/**
 * Search bar với placeholder - reusable
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Tìm kiếm...",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFFF0F0F0))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = AppColors.GrayPlaceholder,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = AppColors.TextPrimary
            ),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.GrayPlaceholder
                    )
                }
                innerTextField()
            }
        )
    }
}

/**
 * Nút bộ lọc - reusable
 */
@Composable
fun FilterButton(
    selectedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(24.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = AppColors.TextPrimary
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = "Bộ lọc",
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (selectedCount > 0) 
                "Bộ lọc • $selectedCount"
            else "Bộ lọc",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Filter Tag Item - reusable (chip style nhỏ gọn)
 */
@Composable
fun FilterTagItem(
    tag: FilterTag,
    onTagClick: (FilterTag) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (tag.isSelected) AppColors.Orange else Color.White
    val textColor = if (tag.isSelected) Color.White else AppColors.TextPrimary
    
    Box(
        modifier = modifier
            .wrapContentWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onTagClick(tag) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = tag.name,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (tag.isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Filter Bottom Sheet - reusable
 */
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
                if (filter.typeTags.isNotEmpty()) {
                    FilterSection(
                        title = "Loại món",
                        tags = filter.typeTags,
                        onTagClick = { tag -> onFilterChange(filter.toggleTag(tag.id)) }
                    )
                    Spacer(Modifier.height(20.dp))
                }
                
                // Vị
                if (filter.tasteTags.isNotEmpty()) {
                    FilterSection(
                        title = "Vị",
                        tags = filter.tasteTags,
                        onTagClick = { tag -> onFilterChange(filter.toggleTag(tag.id)) }
                    )
                    Spacer(Modifier.height(20.dp))
                }
                
                // Nguyên liệu
                if (filter.ingredientTags.isNotEmpty()) {
                    FilterSection(
                        title = "Nguyên liệu",
                        tags = filter.ingredientTags,
                        onTagClick = { tag -> onFilterChange(filter.toggleTag(tag.id)) }
                    )
                    Spacer(Modifier.height(20.dp))
                }
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
                    onClick = { onFilterChange(filter.clearAll()) },
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

/**
 * Custom FlowRow để wrap tags xuống dòng - giống BlindBox style
 */
@Composable
fun CustomFlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 0.dp,
    verticalSpacing: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
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
            
            if (currentRowWidth + placeable.width > constraints.maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow.toList())
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            
            currentRow.add(placeable)
            currentRowWidth += placeable.width + horizontalSpacingPx
        }
        
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow.toList())
        }
        
        val rowHeights = rows.map { row -> row.maxOfOrNull { it.height } ?: 0 }
        val totalHeight = rowHeights.sum() + (rows.size - 1).coerceAtLeast(0) * verticalSpacingPx
        
        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                var x = 0
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + horizontalSpacingPx
                }
                y += rowHeights[rowIndex] + verticalSpacingPx
            }
        }
    }
}
