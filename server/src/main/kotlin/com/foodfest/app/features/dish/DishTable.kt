package com.foodfest.app.features.dish

import com.foodfest.app.features.tag.Tag
import com.foodfest.app.features.tag.TagTable
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object DishTable : IntIdTable("dishes", "dish_id") {
	val name = varchar("dish_name", 100)
	val imageUrl = text("image_url").nullable()
	val description = text("description").nullable()
	val ingredients = text("ingredients").nullable()
	val instructions = text("instructions").nullable()
	val prepTime = integer("prep_time").nullable()
	val cookTime = integer("cook_time").nullable()
	val serving = integer("serving").nullable()
}

object DishTagTable : Table("dish_tags") {
	val dishId = reference("dish_id", DishTable, onDelete = ReferenceOption.CASCADE)
	val tagId = reference("tag_id", TagTable, onDelete = ReferenceOption.CASCADE)
	override val primaryKey = PrimaryKey(dishId, tagId)
}

@Serializable
data class Dish(
	val id: Int,
	val name: String,
	val imageUrl: String? = null,
	val description: String? = null,
	val ingredients: String? = null,
	val instructions: String? = null,
	val prepTime: Int? = null,
	val cookTime: Int? = null,
	val serving: Int? = null,
	val tags: List<Tag> = emptyList()
)

class DishRepository {
	suspend fun getDishById(id: Int): Dish? = newSuspendedTransaction(Dispatchers.IO) {
		val dishRow = DishTable.select { DishTable.id eq id }.singleOrNull() ?: return@newSuspendedTransaction null
		val tags = loadTags(listOf(id))[id].orEmpty()
		dishRow.toDish(tags)
	}

	suspend fun searchDishes(keyword: String, page: Int, limit: Int): Pair<List<Dish>, Int> = newSuspendedTransaction(Dispatchers.IO) {
		val offset = ((page - 1).coerceAtLeast(0)) * limit
		val pattern = "%${keyword.trim()}%"

		val baseQuery = DishTable
			.slice(DishTable.id)
			.select { DishTable.name like pattern }
			.orderBy(DishTable.id to SortOrder.ASC)

		val total = baseQuery.count().toInt()
		val pageIds = baseQuery.limit(limit, offset.toLong()).map { it[DishTable.id].value }
		val dishes = fetchDishesWithTags(pageIds)
		dishes to total
	}

	suspend fun getDishes(tagIds: List<Int>, page: Int, limit: Int): Pair<List<Dish>, Int> = newSuspendedTransaction(Dispatchers.IO) {
		val offset = ((page - 1).coerceAtLeast(0)) * limit

		if (tagIds.isEmpty()) {
			val baseQuery = DishTable.slice(DishTable.id).selectAll().orderBy(DishTable.id to SortOrder.ASC)
			val total = baseQuery.count().toInt()
			val pageIds = baseQuery.limit(limit, offset.toLong()).map { it[DishTable.id].value }
			val dishes = fetchDishesWithTags(pageIds)
			return@newSuspendedTransaction dishes to total
		}

		// AND logic: Find dishes that have ALL selected tags
		// Group by dish_id and count matching tags, only keep dishes with count == tagIds.size
		val matchingDishIds = DishTagTable
			.slice(DishTagTable.dishId, DishTagTable.tagId.count())
			.select { DishTagTable.tagId inList tagIds }
			.groupBy(DishTagTable.dishId)
			.having { DishTagTable.tagId.count() eq tagIds.size.toLong() }
			.orderBy(DishTagTable.dishId to SortOrder.ASC)
			.map { it[DishTagTable.dishId].value }

		val total = matchingDishIds.size
		val pageIds = matchingDishIds.drop(offset).take(limit)

		val dishes = fetchDishesWithTags(pageIds)
		dishes to total
	}

	suspend fun getRandomDishes(tagIds: List<Int>, count: Int): List<Dish> = newSuspendedTransaction(Dispatchers.IO) {
		val poolIds = if (tagIds.isEmpty()) {
			DishTable.slice(DishTable.id).selectAll().map { it[DishTable.id].value }
		} else {
			// AND logic: Find dishes that have ALL selected tags
			DishTagTable
				.slice(DishTagTable.dishId, DishTagTable.tagId.count())
				.select { DishTagTable.tagId inList tagIds }
				.groupBy(DishTagTable.dishId)
				.having { DishTagTable.tagId.count() eq tagIds.size.toLong() }
				.map { it[DishTagTable.dishId].value }
		}

		if (poolIds.isEmpty()) return@newSuspendedTransaction emptyList()

		val selectedIds = poolIds.shuffled().take(count)
		fetchDishesWithTags(selectedIds)
	}

	suspend fun updateDishImage(dishId: Int, imageUrl: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
		val updated = DishTable.update({ DishTable.id eq dishId }) {
			it[DishTable.imageUrl] = imageUrl
		}
		updated > 0
	}

	private fun fetchDishesWithTags(dishIds: List<Int>): List<Dish> {
		if (dishIds.isEmpty()) return emptyList()

		val dishesMap = DishTable
			.select { DishTable.id inList dishIds }
			.associateBy({ it[DishTable.id].value }, { it })

		val tagsByDish = loadTags(dishIds)

		return dishIds.mapNotNull { id ->
			val row = dishesMap[id] ?: return@mapNotNull null
			row.toDish(tagsByDish[id].orEmpty())
		}
	}

	private fun loadTags(dishIds: List<Int>): Map<Int, List<Tag>> {
		if (dishIds.isEmpty()) return emptyMap()

		return DishTagTable
			.join(TagTable, JoinType.INNER, additionalConstraint = { DishTagTable.tagId eq TagTable.id })
			.slice(DishTagTable.dishId, TagTable.id, TagTable.name, TagTable.type)
			.select { DishTagTable.dishId inList dishIds }
			.groupBy(
				keySelector = { it[DishTagTable.dishId].value },
				valueTransform = {
					Tag(
						id = it[TagTable.id].value,
						name = it[TagTable.name],
						type = it[TagTable.type]
					)
				}
			)
	}
}

private fun ResultRow.toDish(tags: List<Tag>): Dish = Dish(
	id = this[DishTable.id].value,
	name = this[DishTable.name],
	imageUrl = this[DishTable.imageUrl],
	description = this[DishTable.description],
	ingredients = this[DishTable.ingredients],
	instructions = this[DishTable.instructions],
	prepTime = this[DishTable.prepTime],
	cookTime = this[DishTable.cookTime],
	serving = this[DishTable.serving],
	tags = tags
)
