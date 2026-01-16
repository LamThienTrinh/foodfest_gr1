package com.foodfest.app.features.dish

import com.foodfest.app.core.exception.AppException
import com.foodfest.app.core.response.PaginatedResponse
import com.foodfest.app.features.tag.TagService
import com.foodfest.app.services.CloudinaryService
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

class DishService(
	private val dishRepository: DishRepository,
	private val tagService: TagService
) {
	@Serializable
	data class DishFilterParams(
		val tagIds: List<Int> = emptyList(),
		val typeTags: List<String> = emptyList(),
		val tasteTags: List<String> = emptyList(),
		val ingredientTags: List<String> = emptyList(),
		val seasonTags: List<String> = emptyList(),
		val page: Int = 1,
		val limit: Int = 5
	)

	private suspend fun resolveTagIds(params: DishFilterParams): List<Int> {
		val resolved = mutableSetOf<Int>()
		resolved.addAll(params.tagIds)

		resolved.addAll(tagService.getTagIdsByNames("TYPE", params.typeTags))
		resolved.addAll(tagService.getTagIdsByNames("TASTE", params.tasteTags))
		resolved.addAll(tagService.getTagIdsByNames("INGREDIENT", params.ingredientTags))
		resolved.addAll(tagService.getTagIdsByNames("SEASON", params.seasonTags))

		return resolved.toList()
	}

	suspend fun listDishes(params: DishFilterParams): Result<PaginatedResponse<Dish>> {
		return runCatching {
			val page = max(1, params.page)
			val limit = 5 // enforce 5 items per page as requested
			val tagIds = resolveTagIds(params)

			val (dishes, total) = dishRepository.getDishes(tagIds, page, limit)
			PaginatedResponse(
				data = dishes,
				page = page,
				limit = limit,
				total = total
			)
		}
	}

	suspend fun getDish(id: Int): Result<Dish> = runCatching {
		dishRepository.getDishById(id) ?: throw AppException.NotFound("Dish not found")
	}

	suspend fun randomDishes(params: DishFilterParams, count: Int): Result<List<Dish>> {
		val limitedCount = min(max(count, 1), 50)
		return runCatching {
			val tagIds = resolveTagIds(params)
			dishRepository.getRandomDishes(tagIds, limitedCount)
		}
	}

	suspend fun search(keyword: String, page: Int): Result<PaginatedResponse<Dish>> {
		return runCatching {
			val cleaned = keyword.trim()
			if (cleaned.isEmpty()) {
				return@runCatching PaginatedResponse(emptyList(), page = 1, limit = 5, total = 0)
			}
			val pageSafe = max(1, page)
			val limit = 5
			val (dishes, total) = dishRepository.searchDishes(cleaned, pageSafe, limit)
			PaginatedResponse(dishes, page = pageSafe, limit = limit, total = total)
		}
	}

	suspend fun uploadDishImage(dishId: Int, base64Image: String): Result<String> {
		return runCatching {
			dishRepository.getDishById(dishId) ?: throw AppException.NotFound("Dish not found")
			if (base64Image.isBlank()) throw AppException.Validation("Image is required")

			val url = CloudinaryService.uploadAvatar(base64Image, folder = "dishes")
				?: throw AppException.Internal("Upload failed")

			val updated = dishRepository.updateDishImage(dishId, url)
			if (!updated) throw AppException.Internal("Failed to save image url")
			url
		}
	}
}
