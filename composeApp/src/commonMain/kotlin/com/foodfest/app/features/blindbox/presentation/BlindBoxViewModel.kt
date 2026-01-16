package com.foodfest.app.features.blindbox.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.blindbox.presentation.models.DishUI
import com.foodfest.app.features.blindbox.presentation.models.TagCategory
import com.foodfest.app.features.blindbox.presentation.models.TagUI
import com.foodfest.app.features.blindbox.presentation.models.toTagUI
import com.foodfest.app.features.dish.data.DishRepository
import com.foodfest.app.features.tag.data.TagRepository
import kotlinx.coroutines.delay

class BlindBoxViewModel {
    private val dishRepo = DishRepository()
    private val tagRepo = TagRepository()

    // State
    var wheelItems by mutableStateOf<List<DishUI>>(emptyList())
        private set
    var allDishes by mutableStateOf<List<DishUI>>(emptyList())
        private set
    var currentDishPage by mutableStateOf(1)
        private set
    var totalDishPages by mutableStateOf(1)
        private set
    var totalDishes by mutableStateOf(0)
        private set
    var isLoadingDishes by mutableStateOf(false)
        private set
    var tags by mutableStateOf<List<TagUI>>(emptyList())
        private set
    var searchQuery by mutableStateOf("")
        private set
    var winningDish by mutableStateOf<DishUI?>(null)
        private set
    var randomError by mutableStateOf<String?>(null)
        private set
    var isOpeningBox by mutableStateOf(false)
        private set
    var showResult by mutableStateOf(false)
        private set

    // Helper function
    private fun getSelectedTagNames(tagList: List<TagUI>, category: TagCategory): List<String> {
        return tagList.filter { it.isSelected && it.category == category }.map { it.name }
    }

    suspend fun loadInitialData() {
        loadDishPageWithFilter(1, emptyList(), "")
        tagRepo.getAllTags().onSuccess { tagList ->
            tags = tagList.map { it.toTagUI() }
        }.onFailure {
            println("Failed to load tags: ${it.message}")
        }
    }

    suspend fun loadDishPageWithFilter(page: Int, currentTags: List<TagUI>, query: String) {
        isLoadingDishes = true

        val typeTags = getSelectedTagNames(currentTags, TagCategory.TYPE)
        val tasteTags = getSelectedTagNames(currentTags, TagCategory.TASTE)
        val ingredientTags = getSelectedTagNames(currentTags, TagCategory.INGREDIENT)

        val hasFilter = typeTags.isNotEmpty() || tasteTags.isNotEmpty() || ingredientTags.isNotEmpty()

        val result = if (hasFilter) {
            dishRepo.getDishesWithFilter(
                page = page,
                typeTags = typeTags,
                tasteTags = tasteTags,
                ingredientTags = ingredientTags
            )
        } else {
            dishRepo.getDishes(page = page)
        }

        result.onSuccess { resp ->
            allDishes = resp.data.map { DishUI(it.id, it.name, it.imageUrl) }
            currentDishPage = resp.page
            totalDishes = resp.total
            totalDishPages = kotlin.math.max(1, (resp.total + resp.limit - 1) / resp.limit)
        }.onFailure {
            if (page == 1) {
                val fallback = listOf(
                    DishUI(1, "Phở Bò"),
                    DishUI(2, "Bún Đậu"),
                    DishUI(3, "Cơm Tấm"),
                    DishUI(4, "Lẩu Thái"),
                    DishUI(5, "Gà Rán")
                )
                allDishes = fallback
                totalDishes = fallback.size
                totalDishPages = 1
            }
        }
        isLoadingDishes = false
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun toggleTag(tag: TagUI) {
        tags = tags.map {
            if (it.id == tag.id) it.copy(isSelected = !it.isSelected)
            else it
        }
    }

    fun addDishToWheel(dish: DishUI) {
        if (wheelItems.none { it.id == dish.id }) {
            wheelItems = wheelItems + dish
        }
    }

    fun removeDishFromWheel(dish: DishUI) {
        wheelItems = wheelItems.filter { it.id != dish.id }
    }

    suspend fun randomDish() {
        if (isOpeningBox) return

        isOpeningBox = true
        showResult = false
        randomError = null
        winningDish = null

        if (wheelItems.isNotEmpty()) {
            // Random từ wheelItems (đã chọn)
            delay(1500) // Animation mở hộp
            winningDish = wheelItems.random()
            showResult = true
            delay(3000) // Hiển thị kết quả
            isOpeningBox = false
        } else {
            // Random từ server với filter
            val typeTags = getSelectedTagNames(tags, TagCategory.TYPE)
            val tasteTags = getSelectedTagNames(tags, TagCategory.TASTE)
            val ingredientTags = getSelectedTagNames(tags, TagCategory.INGREDIENT)

            dishRepo.getRandomDish(typeTags, tasteTags, ingredientTags)
                .onSuccess { dish ->
                    delay(1500) // Animation mở hộp
                    winningDish = DishUI(dish.id, dish.name, dish.imageUrl)
                    showResult = true
                    delay(3000) // Hiển thị kết quả
                    isOpeningBox = false
                }
                .onFailure {
                    randomError = "Không tìm thấy món phù hợp. Hãy thử lọc lại."
                    isOpeningBox = false
                }
        }
    }
}
