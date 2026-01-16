package com.foodfest.app.features.home.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.theme.AppColors

data class PostTypeOption(
    val value: String?,  // null = "Tất cả"
    val label: String,
    val emoji: String
)

val postTypeOptions = listOf(
    PostTypeOption(null, "Tất cả", "📋"),
    PostTypeOption("share", "Chia sẻ", "📝"),
    PostTypeOption("recipe", "Công thức", "🍳"),
    PostTypeOption("review", "Review", "⭐")
)

@Composable
fun PostTypeFilter(
    selectedType: String?,
    onTypeSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        postTypeOptions.forEach { option ->
            FilterChipItem(
                selected = selectedType == option.value,
                label = "${option.emoji} ${option.label}",
                onClick = { onTypeSelected(option.value) }
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) AppColors.Orange else AppColors.Orange.copy(alpha = 0.1f)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else AppColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
