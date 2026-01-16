package com.foodfest.app.features.favorite

import com.foodfest.app.features.auth.AuthTable
import com.foodfest.app.features.dish.DishTable
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

// =============================================
// TABLE DEFINITIONS
// =============================================
object FavoriteDishTable : Table("favorite_dishes") {
    val userId = reference("user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val dishId = reference("dish_id", DishTable, onDelete = ReferenceOption.CASCADE)
    val savedAt = timestamp("saved_at").defaultExpression(CurrentTimestamp())
    
    override val primaryKey = PrimaryKey(userId, dishId)
}

// =============================================
// MODELS
// =============================================
@Serializable
data class FavoriteDish(
    val dishId: Int,
    val dishName: String,
    val imageUrl: String? = null,
    val savedAt: String
)

// =============================================
// REPOSITORY
// =============================================
class FavoriteRepository {
    
    suspend fun addFavorite(userId: Int, dishId: Int): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        // Check if already favorited
        val exists = FavoriteDishTable.select {
            (FavoriteDishTable.userId eq userId) and (FavoriteDishTable.dishId eq dishId)
        }.count() > 0
        
        if (exists) return@newSuspendedTransaction false
        
        FavoriteDishTable.insert {
            it[FavoriteDishTable.userId] = userId
            it[FavoriteDishTable.dishId] = dishId
        }
        true
    }
    
    suspend fun removeFavorite(userId: Int, dishId: Int): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val deleted = FavoriteDishTable.deleteWhere {
            (FavoriteDishTable.userId eq userId) and (FavoriteDishTable.dishId eq dishId)
        }
        deleted > 0
    }
    
    suspend fun isFavorite(userId: Int, dishId: Int): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        FavoriteDishTable.select {
            (FavoriteDishTable.userId eq userId) and (FavoriteDishTable.dishId eq dishId)
        }.count() > 0
    }
    
    suspend fun getFavorites(userId: Int, page: Int = 1, limit: Int = 20): Pair<List<FavoriteDish>, Int> = 
        newSuspendedTransaction(Dispatchers.IO) {
            val offset = ((page - 1).coerceAtLeast(0)) * limit
            
            val total = FavoriteDishTable.select { FavoriteDishTable.userId eq userId }.count().toInt()
            
            val favorites = (FavoriteDishTable innerJoin DishTable)
                .select { FavoriteDishTable.userId eq userId }
                .orderBy(FavoriteDishTable.savedAt to SortOrder.DESC)
                .limit(limit, offset.toLong())
                .map { row ->
                    FavoriteDish(
                        dishId = row[FavoriteDishTable.dishId].value,
                        dishName = row[DishTable.name],
                        imageUrl = row[DishTable.imageUrl],
                        savedAt = row[FavoriteDishTable.savedAt].toString()
                    )
                }
            
            favorites to total
        }
    
    suspend fun getFavoriteIds(userId: Int): List<Int> = newSuspendedTransaction(Dispatchers.IO) {
        FavoriteDishTable.select { FavoriteDishTable.userId eq userId }
            .map { it[FavoriteDishTable.dishId].value }
    }
}
