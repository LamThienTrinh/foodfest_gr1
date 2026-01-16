package com.foodfest.app.features.personaldish.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.theme.AppColors

@Composable
fun MyDishesSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Tìm trong món của tôi..."
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                color = AppColors.GrayPlaceholder,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Tìm kiếm",
                tint = AppColors.GrayPlaceholder
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Orange,
            unfocusedBorderColor = AppColors.GrayPlaceholder.copy(alpha = 0.3f),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

@Composable
fun MyDishesFilterButton(
    selectedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selectedCount > 0) AppColors.Orange else AppColors.Background
        ),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Bộ lọc",
                tint = if (selectedCount > 0) Color.White else AppColors.Brown,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = if (selectedCount > 0) "Lọc ($selectedCount)" else "Bộ lọc",
                color = if (selectedCount > 0) Color.White else AppColors.Brown,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun MyDishesSearchAndFilter(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filterCount: Int,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        MyDishesSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange
        )
        
        Spacer(Modifier.height(16.dp))
        
        MyDishesFilterButton(
            selectedCount = filterCount,
            onClick = onFilterClick
        )
    }
}
