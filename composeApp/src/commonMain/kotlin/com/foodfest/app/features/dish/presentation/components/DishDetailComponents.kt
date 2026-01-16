package com.foodfest.app.features.dish.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.theme.AppColors

/**
 * Danh sách nguyên liệu với bullet point
 */
@Composable
fun IngredientsList(
    ingredients: String,
    modifier: Modifier = Modifier
) {
    val ingredientsList = ingredients.split("\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ingredientsList.forEach { ingredient ->
            IngredientItem(ingredient = ingredient)
        }
    }
}

@Composable
private fun IngredientItem(
    ingredient: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(AppColors.Orange, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = ingredient,
            fontSize = 16.sp,
            color = AppColors.TextPrimary
        )
    }
}

/**
 * Danh sách các bước hướng dẫn với số thứ tự
 */
@Composable
fun InstructionsList(
    instructions: String,
    modifier: Modifier = Modifier
) {
    val steps = instructions.split("\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        steps.forEachIndexed { index, step ->
            InstructionStep(
                stepNumber = index + 1,
                stepText = step
            )
        }
    }
}

@Composable
private fun InstructionStep(
    stepNumber: Int,
    stepText: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        // Step number
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(AppColors.Orange, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$stepNumber",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = stepText,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
