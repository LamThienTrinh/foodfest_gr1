package com.foodfest.app.features.blindbox

import com.foodfest.app.core.exception.AppException
import com.foodfest.app.features.dish.DishRepository
import com.foodfest.app.features.personaldish.PersonalDishRepository
import com.foodfest.app.features.tag.TagService
import kotlinx.serialization.Serializable

@Serializable
data class BlindBoxDishResult(
    val id: Int,
    val sourceType: String,
    val name: String,
    val imageUrl: String? = null,
    val tags: List<String> = emptyList()
)

data class BlindBoxRandomRequest(
    val includeSystem: Boolean = true,
    val includePersonal: Boolean = false,
    val typeTags: List<String> = emptyList(),
    val tasteTags: List<String> = emptyList(),
    val ingredientTags: List<String> = emptyList()
)

class BlindBoxService(
    private val dishRepository: DishRepository,
    private val personalDishRepository: PersonalDishRepository,
    private val tagService: TagService
) {
    suspend fun randomDish(requesterUserId: Int?, request: BlindBoxRandomRequest): Result<BlindBoxDishResult> =
        runCatching {
            if (!request.includeSystem && !request.includePersonal) {
                throw AppException.Validation("Chọn ít nhất một nguồn món")
            }
            if (request.includePersonal && requesterUserId == null) {
                throw AppException.Unauthorized("Bạn cần đăng nhập để dùng Món của tôi")
            }

            val candidates = mutableListOf<BlindBoxDishResult>()
            val selectedTagNames = request.typeTags + request.tasteTags + request.ingredientTags

            if (request.includeSystem) {
                val tagIds = resolveTagIds(request)
                val systemDishes = dishRepository.getRandomDishes(tagIds, count = 50)
                candidates += systemDishes.map { dish ->
                    BlindBoxDishResult(
                        id = dish.id,
                        sourceType = SOURCE_SYSTEM,
                        name = dish.name,
                        imageUrl = dish.imageUrl,
                        tags = dish.tags.map { it.name }
                    )
                }
            }

            if (request.includePersonal && requesterUserId != null) {
                val personalDishes = personalDishRepository.getAllByUser(requesterUserId)
                    .filter { dish ->
                        selectedTagNames.isEmpty() || selectedTagNames.all { selected -> selected in dish.tags }
                    }
                candidates += personalDishes.map { dish ->
                    BlindBoxDishResult(
                        id = dish.id,
                        sourceType = SOURCE_PERSONAL,
                        name = dish.dishName,
                        imageUrl = dish.imageUrl,
                        tags = dish.tags
                    )
                }
            }

            // System dish ids and personal dish ids live in different tables, so sourceType is part of the identity.
            candidates.randomOrNull() ?: throw AppException.NotFound("Không tìm thấy món phù hợp")
        }

    private suspend fun resolveTagIds(request: BlindBoxRandomRequest): List<Int> {
        val resolved = mutableSetOf<Int>()
        resolved += tagService.getTagIdsByNames("TYPE", request.typeTags)
        resolved += tagService.getTagIdsByNames("TASTE", request.tasteTags)
        resolved += tagService.getTagIdsByNames("INGREDIENT", request.ingredientTags)
        return resolved.toList()
    }

    private companion object {
        const val SOURCE_SYSTEM = "system"
        const val SOURCE_PERSONAL = "personal"
    }
}
