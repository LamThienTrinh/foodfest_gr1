package com.foodfest.app.features.tag

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

// Table definition mirrors `tags` in initial.sql
object TagTable : IntIdTable("tags", "tag_id") {
	val name = varchar("tag_name", 50).uniqueIndex()
	val type = varchar("tag_type", 50)
}

@Serializable
data class Tag(
	val id: Int,
	val name: String,
	val type: String
)

class TagRepository {
	suspend fun getAll(type: String? = null): List<Tag> = newSuspendedTransaction(Dispatchers.IO) {
		val query = if (type.isNullOrBlank()) {
			TagTable.selectAll()
		} else {
			TagTable.select { TagTable.type eq type.uppercase() }
		}
		query.map { it.toTag() }
	}

	suspend fun findIdsByNames(type: String, names: List<String>): List<Int> = newSuspendedTransaction(Dispatchers.IO) {
		if (names.isEmpty()) return@newSuspendedTransaction emptyList()

		TagTable
			.slice(TagTable.id)
			.select { (TagTable.type eq type.uppercase()) and (TagTable.name inList names.map { it.trim() }.filter { it.isNotBlank() }) }
			.map { it[TagTable.id].value }
	}

	suspend fun getByIds(ids: List<Int>): List<Tag> = newSuspendedTransaction(Dispatchers.IO) {
		if (ids.isEmpty()) return@newSuspendedTransaction emptyList()
		TagTable.select { TagTable.id inList ids }.map { it.toTag() }
	}
}

private fun ResultRow.toTag(): Tag = Tag(
	id = this[TagTable.id].value,
	name = this[TagTable.name],
	type = this[TagTable.type]
)
