package com.foodfest.app.features.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.features.home.presentation.models.HomeFeedMode
import com.foodfest.app.theme.AppColors

@Composable
fun FeedModeSelector(
    selectedMode: HomeFeedMode,
    onModeSelected: (HomeFeedMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeedModeChip(
            label = "👥 Following",
            selected = selectedMode == HomeFeedMode.FOLLOWING,
            onClick = { onModeSelected(HomeFeedMode.FOLLOWING) }
        )
        FeedModeChip(
            label = "🌍 All posts",
            selected = selectedMode == HomeFeedMode.ALL,
            onClick = { onModeSelected(HomeFeedMode.ALL) }
        )
    }
}

@Composable
private fun FeedModeChip(
    label: String,
    selected: Boolean,
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
