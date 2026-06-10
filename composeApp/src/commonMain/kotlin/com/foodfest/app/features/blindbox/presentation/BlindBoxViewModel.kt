package com.foodfest.app.features.blindbox.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.blindbox.data.BlindBoxRepository
import com.foodfest.app.features.blindbox.data.BlindBoxDishResult
import com.foodfest.app.features.blindbox.presentation.models.DishUI
import com.foodfest.app.features.blindbox.presentation.models.DishSourceType
import com.foodfest.app.features.blindbox.presentation.models.TagCategory
import com.foodfest.app.features.blindbox.presentation.models.TagUI
import com.foodfest.app.features.blindbox.presentation.models.toTagUI
import com.foodfest.app.features.dish.data.DishRepository
import com.foodfest.app.features.home.data.CreatePostRequest
import com.foodfest.app.features.home.data.PostRepository
import com.foodfest.app.features.personaldish.data.PersonalDishRepository
import com.foodfest.app.features.tag.data.TagRepository
import kotlinx.coroutines.delay

class BlindBoxViewModel {
    private val dishRepo = DishRepository()
    private val blindBoxRepo = BlindBoxRepository()
    private val personalDishRepo = PersonalDishRepository()
    private val postRepo = PostRepository()
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
    var includeSystemDishes by mutableStateOf(true)
        private set
    var includePersonalDishes by mutableStateOf(false)
        private set
    var isLoadingRandom by mutableStateOf(false)
        private set
    var sourceErrorMessage by mutableStateOf<String?>(null)
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
    var isPostingResult by mutableStateOf(false)
        private set
    var postResultMessage by mutableStateOf<String?>(null)
        private set
    var postedWinningDishKey by mutableStateOf<String?>(null)
        private set

    val hasPersonalDishCandidates: Boolean
        get() = allDishes.any { it.sourceType == DishSourceType.PERSONAL } ||
            wheelItems.any { it.sourceType == DishSourceType.PERSONAL }

    val shouldShowCreatePersonalDishCta: Boolean
        get() = includePersonalDishes && !isLoadingDishes && !hasPersonalDishCandidates

    val hasPostedCurrentWinningDish: Boolean
        get() = winningDish?.wheelKey != null && winningDish?.wheelKey == postedWinningDishKey

    // Helper function
    private fun getSelectedTagNames(tagList: List<TagUI>, category: TagCategory): List<String> {
        return tagList.filter { it.isSelected && it.category == category }.map { it.name }
    }

    suspend fun loadInitialData(preferPersonalSource: Boolean = false) {
        if (preferPersonalSource) {
            includePersonalDishes = true
        }
        loadDishPageWithFilter(1, emptyList(), "")
        tagRepo.getAllTags().onSuccess { tagList ->
            tags = tagList.map { it.toTagUI() }
        }.onFailure {
            println("Failed to load tags: ${it.message}")
        }
    }

    suspend fun loadDishPageWithFilter(page: Int, currentTags: List<TagUI>, query: String) {
        isLoadingDishes = true
        sourceErrorMessage = null

        val typeTags = getSelectedTagNames(currentTags, TagCategory.TYPE)
        val tasteTags = getSelectedTagNames(currentTags, TagCategory.TASTE)
        val ingredientTags = getSelectedTagNames(currentTags, TagCategory.INGREDIENT)

        val hasFilter = typeTags.isNotEmpty() || tasteTags.isNotEmpty() || ingredientTags.isNotEmpty()

        val loadedDishes = mutableListOf<DishUI>()
        var systemTotal = 0
        var systemLimit = 5
        var personalTotal = 0
        var personalLimit = 20
        var lastError: String? = null

        if (includeSystemDishes) {
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
                loadedDishes += resp.data.map { dish ->
                    DishUI(
                        id = dish.id,
                        sourceType = DishSourceType.SYSTEM,
                        name = dish.name,
                        imageUrl = dish.imageUrl
                    )
                }
                systemTotal = resp.total
                systemLimit = resp.limit
            }.onFailure { error ->
                lastError = error.message
                if (page == 1 && !includePersonalDishes) {
                    loadedDishes += fallbackSystemDishes()
                    systemTotal = loadedDishes.size
                }
            }
        }

        if (includePersonalDishes) {
            personalDishRepo.getMyDishes(page).onSuccess { resp ->
                val selectedTags = typeTags + tasteTags + ingredientTags
                val filteredPersonalDishes = resp.data.filter { dish ->
                    selectedTags.isEmpty() || selectedTags.all { selected -> selected in dish.tags }
                }
                loadedDishes += filteredPersonalDishes.map { dish ->
                    DishUI(
                        id = dish.id,
                        sourceType = DishSourceType.PERSONAL,
                        name = dish.dishName,
                        imageUrl = dish.imageUrl
                    )
                }
                personalTotal = resp.total
                personalLimit = resp.limit
            }.onFailure { error ->
                lastError = error.message
            }
        }

        allDishes = loadedDishes
        currentDishPage = page
        totalDishes = systemTotal + personalTotal
        totalDishPages = kotlin.math.max(
            1,
            kotlin.math.max(
                pagesOf(systemTotal, systemLimit),
                pagesOf(personalTotal, personalLimit)
            )
        )
        sourceErrorMessage = lastError?.takeIf { loadedDishes.isEmpty() }
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

    suspend fun toggleIncludeSystemDishes() {
        includeSystemDishes = !includeSystemDishes
        if (!includeSystemDishes && !includePersonalDishes) {
            sourceErrorMessage = "Chọn ít nhất một nguồn món"
            allDishes = emptyList()
            return
        }
        loadDishPageWithFilter(1, tags, searchQuery)
    }

    suspend fun toggleIncludePersonalDishes() {
        includePersonalDishes = !includePersonalDishes
        if (!includeSystemDishes && !includePersonalDishes) {
            sourceErrorMessage = "Chọn ít nhất một nguồn món"
            allDishes = emptyList()
            return
        }
        loadDishPageWithFilter(1, tags, searchQuery)
    }

    fun addDishToWheel(dish: DishUI) {
        if (wheelItems.none { it.wheelKey == dish.wheelKey }) {
            wheelItems = wheelItems + dish
        }
    }

    fun removeDishFromWheel(dish: DishUI) {
        wheelItems = wheelItems.filter { it.wheelKey != dish.wheelKey }
    }

    suspend fun randomDish() {
        if (isOpeningBox || isLoadingRandom) return

        if (!includeSystemDishes && !includePersonalDishes) {
            sourceErrorMessage = "Chọn ít nhất một nguồn món"
            return
        }

        isOpeningBox = true
        isLoadingRandom = true
        showResult = false
        randomError = null
        sourceErrorMessage = null
        postResultMessage = null
        winningDish = null

        if (wheelItems.isNotEmpty()) {
            // Local wheel keeps hand-picked dishes from both sources, so identity must include sourceType.
            delay(1500)
            winningDish = wheelItems.random()
            showResult = true
            delay(3000)
            isOpeningBox = false
            isLoadingRandom = false
        } else {
            val typeTags = getSelectedTagNames(tags, TagCategory.TYPE)
            val tasteTags = getSelectedTagNames(tags, TagCategory.TASTE)
            val ingredientTags = getSelectedTagNames(tags, TagCategory.INGREDIENT)

            // API random centralizes source merging so system and personal dishes are handled consistently.
            blindBoxRepo.randomDish(
                includeSystem = includeSystemDishes,
                includePersonal = includePersonalDishes,
                typeTags = typeTags,
                tasteTags = tasteTags,
                ingredientTags = ingredientTags
            ).onSuccess { dish ->
                    delay(1500)
                    winningDish = dish.toDishUi()
                    showResult = true
                    delay(3000)
                    isOpeningBox = false
                    isLoadingRandom = false
                }
                .onFailure { error ->
                    randomError = error.message ?: "Không tìm thấy món phù hợp. Hãy thử lọc lại."
                    isOpeningBox = false
                    isLoadingRandom = false
                }
        }
    }

    suspend fun shareWinningDishToFollowers() {
        val dish = winningDish ?: run {
            postResultMessage = "Chưa có món để đăng"
            return
        }
        if (isPostingResult || hasPostedCurrentWinningDish) return

        isPostingResult = true
        postResultMessage = null

        // MVP only creates a feed post; follower push notifications stay out of this phase.
        val request = CreatePostRequest(
            postType = "blind_box_result",
            title = "Hôm nay ăn gì?",
            content = "Blind Box chọn cho mình: ${dish.name}",
            imageUrl = dish.imageUrl
        )

        postRepo.createPost(request).fold(
            onSuccess = {
                postedWinningDishKey = dish.wheelKey
                postResultMessage = "Đã đăng cho follower"
                isPostingResult = false
            },
            onFailure = { error ->
                postResultMessage = error.message ?: "Không đăng được kết quả"
                isPostingResult = false
            }
        )
    }

    private fun pagesOf(total: Int, limit: Int): Int {
        if (total <= 0) return 1
        return kotlin.math.max(1, (total + limit - 1) / limit)
    }

    private fun fallbackSystemDishes(): List<DishUI> {
        return listOf(
            DishUI(id = 1, sourceType = DishSourceType.SYSTEM, name = "Phở Bò"),
            DishUI(id = 2, sourceType = DishSourceType.SYSTEM, name = "Bún Đậu"),
            DishUI(id = 3, sourceType = DishSourceType.SYSTEM, name = "Cơm Tấm"),
            DishUI(id = 4, sourceType = DishSourceType.SYSTEM, name = "Lẩu Thái"),
            DishUI(id = 5, sourceType = DishSourceType.SYSTEM, name = "Gà Rán")
        )
    }

    private fun BlindBoxDishResult.toDishUi(): DishUI {
        return DishUI(
            id = id,
            sourceType = if (sourceType == "personal") DishSourceType.PERSONAL else DishSourceType.SYSTEM,
            name = name,
            imageUrl = imageUrl
        )
    }
}
