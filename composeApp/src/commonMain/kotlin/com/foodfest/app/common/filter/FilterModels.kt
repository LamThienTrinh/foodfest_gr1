package com.foodfest.app.common.filter

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
    
    fun getSelectedTypeTags(): List<String> = typeTags.filter { it.isSelected }.map { it.name }
    fun getSelectedTasteTags(): List<String> = tasteTags.filter { it.isSelected }.map { it.name }
    fun getSelectedIngredientTags(): List<String> = ingredientTags.filter { it.isSelected }.map { it.name }
    
    fun clearAll(): DishFilter = copy(
        typeTags = typeTags.map { it.copy(isSelected = false) },
        tasteTags = tasteTags.map { it.copy(isSelected = false) },
        ingredientTags = ingredientTags.map { it.copy(isSelected = false) }
    )
    
    fun toggleTag(tagId: Int): DishFilter {
        return copy(
            typeTags = typeTags.map { if (it.id == tagId) it.copy(isSelected = !it.isSelected) else it },
            tasteTags = tasteTags.map { if (it.id == tagId) it.copy(isSelected = !it.isSelected) else it },
            ingredientTags = ingredientTags.map { if (it.id == tagId) it.copy(isSelected = !it.isSelected) else it }
        )
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
