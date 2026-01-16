package com.foodfest.app.features.personaldish

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedPersonalDishes(
    val data: List<PersonalDish>,
    val page: Int,
    val limit: Int,
    val total: Int
)

class PersonalDishService(
    private val repository: PersonalDishRepository = PersonalDishRepository()
) {
    suspend fun create(userId: Int, request: CreatePersonalDishRequest): Result<PersonalDish> = runCatching {
        // Check if user already has a personal dish from this original dish
        request.originalDishId?.let { origId ->
            repository.isFromOriginalDish(userId, origId)?.let {
                throw IllegalStateException("Bạn đã lưu công thức riêng cho món này rồi")
            }
        }
        repository.create(userId, request)
    }
    
    suspend fun getById(personalDishId: Int, userId: Int): Result<PersonalDish> = runCatching {
        repository.getById(personalDishId, userId) 
            ?: throw NoSuchElementException("Không tìm thấy món ăn")
    }
    
    suspend fun getByUser(userId: Int, page: Int): Result<PaginatedPersonalDishes> = runCatching {
        val limit = 20
        val (dishes, total) = repository.getByUser(userId, page, limit)
        PaginatedPersonalDishes(
            data = dishes,
            page = page,
            limit = limit,
            total = total
        )
    }
    
    suspend fun update(personalDishId: Int, userId: Int, request: UpdatePersonalDishRequest): Result<PersonalDish> = 
        runCatching {
            repository.update(personalDishId, userId, request)
                ?: throw NoSuchElementException("Không tìm thấy món ăn hoặc bạn không có quyền chỉnh sửa")
        }
    
    suspend fun delete(personalDishId: Int, userId: Int): Result<Boolean> = runCatching {
        val deleted = repository.delete(personalDishId, userId)
        if (!deleted) throw NoSuchElementException("Không tìm thấy món ăn hoặc bạn không có quyền xóa")
        true
    }
    
    suspend fun checkSaved(userId: Int, originalDishId: Int): Result<PersonalDish?> = runCatching {
        repository.isFromOriginalDish(userId, originalDishId)
    }
}
