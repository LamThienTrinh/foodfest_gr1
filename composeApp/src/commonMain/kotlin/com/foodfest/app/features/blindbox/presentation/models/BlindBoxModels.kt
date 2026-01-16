package com.foodfest.app.features.blindbox.presentation.models

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.foodfest.app.features.tag.data.Tag

enum class TagCategory { TYPE, TASTE, INGREDIENT }

data class TagUI(
    val id: Int,
    val name: String,
    val category: TagCategory,
    val isSelected: Boolean = false
)

data class DishUI(
    val id: Int,
    val name: String,
    val imageUrl: String? = null
)

data class Confetti(
    val x: Float,
    val y: Float,
    val color: Color,
    val rotation: Float,
    val velocity: Offset,
    val size: Float
)

fun Tag.toTagUI(): TagUI {
    val category = when (type.uppercase()) {
        "TYPE" -> TagCategory.TYPE
        "TASTE" -> TagCategory.TASTE
        "INGREDIENT" -> TagCategory.INGREDIENT
        else -> TagCategory.TYPE
    }
    return TagUI(id, name, category, false)
}
