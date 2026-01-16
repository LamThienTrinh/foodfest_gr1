package com.foodfest.app.features.dish.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.features.dish.presentation.models.FilterTag
import com.foodfest.app.theme.AppColors

@Composable
fun FilterTagItem(
    tag: FilterTag,
    onTagClick: (FilterTag) -> Unit
) {
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (tag.isSelected) AppColors.Orange 
                else AppColors.White
            )
            .border(
                width = 1.dp,
                color = if (tag.isSelected) AppColors.Orange 
                        else AppColors.GrayPlaceholder.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onTagClick(tag) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tag.name,
            fontSize = 14.sp,
            fontWeight = if (tag.isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (tag.isSelected) AppColors.White else AppColors.TextPrimary
        )
    }
}
