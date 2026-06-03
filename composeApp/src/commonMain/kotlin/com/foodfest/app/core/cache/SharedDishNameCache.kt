package com.foodfest.app.core.cache

import com.foodfest.app.features.dish.data.DishRepository
import com.foodfest.app.features.personaldish.data.PersonalDishRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object SharedDishNameCache {
    private const val cacheTtlMillis = 30 * 60 * 1000L

    private data class CachedName(
        val value: String,
        val cachedAtMillis: Long
    )

    private val dishCache = mutableMapOf<Int, CachedName>()
    private val personalDishCache = mutableMapOf<Int, CachedName>()
    private val mutex = Mutex()

    suspend fun getDishName(dishId: Int, repo: DishRepository): String {
        mutex.withLock {
            dishCache[dishId]?.takeIf { !isExpired(it) }?.let { return it.value }
            dishCache.remove(dishId)
        }
        val name = repo.getDishById(dishId).getOrNull()?.name
        val resolved = name?.takeIf { it.isNotBlank() } ?: "Món #$dishId"
        mutex.withLock { dishCache[dishId] = CachedName(resolved, nowMillis()) }
        return resolved
    }

    suspend fun getPersonalDishName(personalDishId: Int, repo: PersonalDishRepository): String {
        mutex.withLock {
            personalDishCache[personalDishId]?.takeIf { !isExpired(it) }?.let { return it.value }
            personalDishCache.remove(personalDishId)
        }
        val name = repo.getById(personalDishId).getOrNull()?.dishName
        val resolved = name?.takeIf { it.isNotBlank() } ?: "Món cá nhân #$personalDishId"
        mutex.withLock { personalDishCache[personalDishId] = CachedName(resolved, nowMillis()) }
        return resolved
    }

    suspend fun resolve(
        dishId: Int?,
        personalDishId: Int?,
        dishRepo: DishRepository,
        personalRepo: PersonalDishRepository
    ): String {
        return when {
            dishId != null -> getDishName(dishId, dishRepo)
            personalDishId != null -> getPersonalDishName(personalDishId, personalRepo)
            else -> "Món trong menu"
        }
    }

    suspend fun clear() {
        mutex.withLock {
            dishCache.clear()
            personalDishCache.clear()
        }
    }

    private fun isExpired(entry: CachedName): Boolean {
        return nowMillis() - entry.cachedAtMillis >= cacheTtlMillis
    }

    private fun nowMillis(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }
}
