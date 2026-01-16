package com.foodfest.app.features.dish.presentation.models

import com.foodfest.app.features.tag.data.Tag

enum class FilterCategory { TYPE, TASTE, INGREDIENT }

data class FilterTag(
    val id: Int,
    val name: String,
    val category: FilterCategory,
    val isSelected: Boolean = false
)

data class DishFilter(
    val typeTags: List<FilterTag> = emptyList(),
    val tasteTags: List<FilterTag> = emptyList(),
    val ingredientTags: List<FilterTag> = emptyList()
) {
    val selectedCount: Int
        get() = typeTags.count { it.isSelected } +
                tasteTags.count { it.isSelected } +
                ingredientTags.count { it.isSelected }
    
    val hasSelectedTags: Boolean
        get() = selectedCount > 0
    
    fun getSelectedTagIds(): List<Int> {
        return (typeTags + tasteTags + ingredientTags)
            .filter { it.isSelected }
            .map { it.id }
    }
}

// Extension function để convert Tag từ API sang FilterTag
fun Tag.toFilterTag(category: FilterCategory, isSelected: Boolean = false): FilterTag {
    return FilterTag(
        id = id,
        name = name,
        category = category,
        isSelected = isSelected
    )
}

// Extension function để convert Tag type string sang FilterCategory
fun String.toFilterCategory(): FilterCategory? {
    return when (this.uppercase()) {
        "TYPE" -> FilterCategory.TYPE
        "TASTE" -> FilterCategory.TASTE
        "INGREDIENT" -> FilterCategory.INGREDIENT
        else -> null
    }
}
