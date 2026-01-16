package com.foodfest.app.features.personaldish

import com.foodfest.app.features.auth.AuthTable
import com.foodfest.app.features.dish.DishTable
import com.foodfest.app.features.tag.TagTable
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

// =============================================
// TABLE DEFINITIONS
// =============================================
object PersonalDishTable : IntIdTable("personal_dishes", "personal_dish_id") {
    val userId = reference("user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val originalDishId = optReference("original_dish_id", DishTable, onDelete = ReferenceOption.SET_NULL)
    val dishName = varchar("dish_name", 100)
    val imageUrl = text("image_url").nullable()
    val description = text("description").nullable()
    val ingredients = text("ingredients").nullable()
    val instructions = text("instructions").nullable()
    val prepTime = integer("prep_time").nullable()
    val cookTime = integer("cook_time").nullable()
    val serving = integer("serving").nullable()
    val note = text("note").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

object PersonalDishTagTable : Table("personal_dish_tags") {
    val personalDishId = reference("personal_dish_id", PersonalDishTable, onDelete = ReferenceOption.CASCADE)
    val tagId = reference("tag_id", TagTable, onDelete = ReferenceOption.CASCADE)
    
    override val primaryKey = PrimaryKey(personalDishId, tagId)
}

// =============================================
// MODELS
// =============================================
@Serializable
data class PersonalDish(
    val id: Int,
    val userId: Int,
    val originalDishId: Int? = null,
    val dishName: String,
    val imageUrl: String? = null,
    val description: String? = null,
    val ingredients: String? = null,
    val instructions: String? = null,
    val prepTime: Int? = null,
    val cookTime: Int? = null,
    val serving: Int? = null,
    val note: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: String
)

@Serializable
data class CreatePersonalDishRequest(
    val originalDishId: Int? = null,
    val dishName: String,
    val imageUrl: String? = null,
    val description: String? = null,
    val ingredients: String? = null,
    val instructions: String? = null,
    val prepTime: Int? = null,
    val cookTime: Int? = null,
    val serving: Int? = null,
    val note: String? = null,
    val tagIds: List<Int>? = null
)

@Serializable
data class UpdatePersonalDishRequest(
    val dishName: String? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val ingredients: String? = null,
    val instructions: String? = null,
    val prepTime: Int? = null,
    val cookTime: Int? = null,
    val serving: Int? = null,
    val note: String? = null,
    val tagIds: List<Int>? = null
)

// =============================================
// REPOSITORY
// =============================================
class PersonalDishRepository {
    
    private fun rowToPersonalDish(row: ResultRow, tags: List<String> = emptyList()) = PersonalDish(
        id = row[PersonalDishTable.id].value,
        userId = row[PersonalDishTable.userId].value,
        originalDishId = row[PersonalDishTable.originalDishId]?.value,
        dishName = row[PersonalDishTable.dishName],
        imageUrl = row[PersonalDishTable.imageUrl],
        description = row[PersonalDishTable.description],
        ingredients = row[PersonalDishTable.ingredients],
        instructions = row[PersonalDishTable.instructions],
        prepTime = row[PersonalDishTable.prepTime],
        cookTime = row[PersonalDishTable.cookTime],
        serving = row[PersonalDishTable.serving],
        note = row[PersonalDishTable.note],
        tags = tags,
        createdAt = row[PersonalDishTable.createdAt].toString()
    )
    
    private suspend fun getTagsForDish(personalDishId: Int): List<String> = newSuspendedTransaction(Dispatchers.IO) {
        (PersonalDishTagTable innerJoin TagTable)
            .select { PersonalDishTagTable.personalDishId eq personalDishId }
            .map { it[TagTable.name] }
    }
    
    suspend fun create(userId: Int, request: CreatePersonalDishRequest): PersonalDish = 
        newSuspendedTransaction(Dispatchers.IO) {
            val id = PersonalDishTable.insertAndGetId {
                it[PersonalDishTable.userId] = userId
                it[originalDishId] = request.originalDishId
                it[dishName] = request.dishName
                it[imageUrl] = request.imageUrl
                it[description] = request.description
                it[ingredients] = request.ingredients
                it[instructions] = request.instructions
                it[prepTime] = request.prepTime
                it[cookTime] = request.cookTime
                it[serving] = request.serving
                it[note] = request.note
            }.value
            
            // Add tags
            request.tagIds?.forEach { tagId ->
                PersonalDishTagTable.insert {
                    it[personalDishId] = id
                    it[PersonalDishTagTable.tagId] = tagId
                }
            }
            
            val row = PersonalDishTable.select { PersonalDishTable.id eq id }.first()
            val tags = getTagsForDish(id)
            rowToPersonalDish(row, tags)
        }
    
    suspend fun getById(personalDishId: Int, userId: Int): PersonalDish? = 
        newSuspendedTransaction(Dispatchers.IO) {
            PersonalDishTable.select { 
                (PersonalDishTable.id eq personalDishId) and (PersonalDishTable.userId eq userId)
            }.singleOrNull()?.let { row ->
                val tags = getTagsForDish(personalDishId)
                rowToPersonalDish(row, tags)
            }
        }
    
    suspend fun getByUser(userId: Int, page: Int = 1, limit: Int = 20): Pair<List<PersonalDish>, Int> = 
        newSuspendedTransaction(Dispatchers.IO) {
            val offset = ((page - 1).coerceAtLeast(0)) * limit
            
            val total = PersonalDishTable.select { PersonalDishTable.userId eq userId }.count().toInt()
            
            val dishes = PersonalDishTable
                .select { PersonalDishTable.userId eq userId }
                .orderBy(PersonalDishTable.createdAt to SortOrder.DESC)
                .limit(limit, offset.toLong())
                .map { row ->
                    val tags = getTagsForDish(row[PersonalDishTable.id].value)
                    rowToPersonalDish(row, tags)
                }
            
            dishes to total
        }
    
    suspend fun update(personalDishId: Int, userId: Int, request: UpdatePersonalDishRequest): PersonalDish? = 
        newSuspendedTransaction(Dispatchers.IO) {
            val updated = PersonalDishTable.update({
                (PersonalDishTable.id eq personalDishId) and (PersonalDishTable.userId eq userId)
            }) {
                request.dishName?.let { name -> it[dishName] = name }
                request.imageUrl?.let { url -> it[imageUrl] = url }
                request.description?.let { desc -> it[description] = desc }
                request.ingredients?.let { ing -> it[ingredients] = ing }
                request.instructions?.let { inst -> it[instructions] = inst }
                request.prepTime?.let { prep -> it[prepTime] = prep }
                request.cookTime?.let { cook -> it[cookTime] = cook }
                request.serving?.let { serv -> it[serving] = serv }
                request.note?.let { n -> it[note] = n }
            }
            
            if (updated == 0) return@newSuspendedTransaction null
            
            // Update tags if provided
            request.tagIds?.let { tagIds ->
                PersonalDishTagTable.deleteWhere { PersonalDishTagTable.personalDishId eq personalDishId }
                tagIds.forEach { tagId ->
                    PersonalDishTagTable.insert {
                        it[PersonalDishTagTable.personalDishId] = personalDishId
                        it[PersonalDishTagTable.tagId] = tagId
                    }
                }
            }
            
            getById(personalDishId, userId)
        }
    
    suspend fun delete(personalDishId: Int, userId: Int): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val deleted = PersonalDishTable.deleteWhere {
            (PersonalDishTable.id eq personalDishId) and (PersonalDishTable.userId eq userId)
        }
        deleted > 0
    }
    
    suspend fun isFromOriginalDish(userId: Int, originalDishId: Int): PersonalDish? = 
        newSuspendedTransaction(Dispatchers.IO) {
            PersonalDishTable.select { 
                (PersonalDishTable.userId eq userId) and (PersonalDishTable.originalDishId eq originalDishId)
            }.singleOrNull()?.let { row ->
                val tags = getTagsForDish(row[PersonalDishTable.id].value)
                rowToPersonalDish(row, tags)
            }
        }
}
