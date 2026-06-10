package com.foodfest.app.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.theme.AppColors

/**
 * Shared source badge for cards that need to show where an item comes from.
 */
@Composable
fun FoodFestSourceBadge(
    text: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (highlighted) AppColors.Orange.copy(alpha = 0.12f) else AppColors.Background,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = if (highlighted) AppColors.Orange else AppColors.TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
