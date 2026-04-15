package com.foodfest.app.features.family

import com.foodfest.app.core.exception.AppException
import com.foodfest.app.features.auth.AuthTable
import com.foodfest.app.features.dish.DishTable
import com.foodfest.app.features.personaldish.PersonalDishTable
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.LocalDate

object FamilyGroupTable : IntIdTable("family_groups", "family_id") {
    val name = varchar("name", 120)
    val ownerUserId = reference("owner_user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

object FamilyMemberTable : Table("family_members") {
    val familyId = reference("family_id", FamilyGroupTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val role = varchar("role", 20)
    val joinedAt = timestamp("joined_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(familyId, userId)
}

object FamilyMenuTable : IntIdTable("family_menus", "family_menu_id") {
    val familyId = reference("family_id", FamilyGroupTable, onDelete = ReferenceOption.CASCADE)
    val menuDate = date("menu_date")
    val mealType = varchar("meal_type", 20)
    val status = varchar("status", 20)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

object FamilyMenuItemTable : IntIdTable("family_menu_items", "family_menu_item_id") {
    val familyMenuId = reference("family_menu_id", FamilyMenuTable, onDelete = ReferenceOption.CASCADE)
    val dishId = optReference("dish_id", DishTable, onDelete = ReferenceOption.SET_NULL)
    val personalDishId = optReference("personal_dish_id", PersonalDishTable, onDelete = ReferenceOption.SET_NULL)
    val note = text("note").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

object FamilyMenuVoteTable : Table("family_menu_votes") {
    val familyMenuItemId = reference("family_menu_item_id", FamilyMenuItemTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val voteType = varchar("vote_type", 10)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(familyMenuItemId, userId)
}

@Serializable
data class FamilyGroup(
    val id: Int,
    val name: String,
    val ownerUserId: Int,
    val createdAt: String,
    val memberCount: Int
)

@Serializable
data class FamilyMember(
    val familyId: Int,
    val userId: Int,
    val username: String,
    val fullName: String,
    val avatarUrl: String? = null,
    val role: String,
    val joinedAt: String
)

@Serializable
data class FamilyMenu(
    val id: Int,
    val familyId: Int,
    val menuDate: String,
    val mealType: String,
    val status: String,
    val createdAt: String
)

@Serializable
data class FamilyMenuItem(
    val id: Int,
    val familyMenuId: Int,
    val dishId: Int? = null,
    val personalDishId: Int? = null,
    val note: String? = null,
    val createdAt: String
)

@Serializable
data class FamilyMenuWithItems(
    val menu: FamilyMenu,
    val items: List<FamilyMenuItem>
)

@Serializable
data class FamilyMenuVoteSummary(
    val familyMenuItemId: Int,
    val dishId: Int? = null,
    val personalDishId: Int? = null,
    val note: String? = null,
    val upVotes: Int,
    val downVotes: Int,
    val userVoteType: String? = null
)

@Serializable
data class FamilyVoteActionResult(
    val voted: Boolean,
    val voteType: String? = null,
    val upVotes: Int,
    val downVotes: Int
)

class FamilyRepository {

    private fun rowToFamilyGroup(row: ResultRow, memberCount: Int): FamilyGroup {
        return FamilyGroup(
            id = row[FamilyGroupTable.id].value,
            name = row[FamilyGroupTable.name],
            ownerUserId = row[FamilyGroupTable.ownerUserId].value,
            createdAt = row[FamilyGroupTable.createdAt].toString(),
            memberCount = memberCount
        )
    }

    private fun rowToFamilyMenu(row: ResultRow): FamilyMenu {
        return FamilyMenu(
            id = row[FamilyMenuTable.id].value,
            familyId = row[FamilyMenuTable.familyId].value,
            menuDate = row[FamilyMenuTable.menuDate].toString(),
            mealType = row[FamilyMenuTable.mealType],
            status = row[FamilyMenuTable.status],
            createdAt = row[FamilyMenuTable.createdAt].toString()
        )
    }

    private fun rowToFamilyMenuItem(row: ResultRow): FamilyMenuItem {
        return FamilyMenuItem(
            id = row[FamilyMenuItemTable.id].value,
            familyMenuId = row[FamilyMenuItemTable.familyMenuId].value,
            dishId = row[FamilyMenuItemTable.dishId]?.value,
            personalDishId = row[FamilyMenuItemTable.personalDishId]?.value,
            note = row[FamilyMenuItemTable.note],
            createdAt = row[FamilyMenuItemTable.createdAt].toString()
        )
    }

    private fun requireFamilyExists(familyId: Int) {
        val exists = FamilyGroupTable.select { FamilyGroupTable.id eq familyId }.count() > 0
        if (!exists) {
            throw AppException.NotFound("Family group not found")
        }
    }

    private fun requireFamilyMember(familyId: Int, userId: Int) {
        val isMember = FamilyMemberTable.select {
            (FamilyMemberTable.familyId eq familyId) and (FamilyMemberTable.userId eq userId)
        }.count() > 0

        if (!isMember) {
            throw AppException.Forbidden("You are not a member of this family")
        }
    }

    private fun requireFamilyOwner(familyId: Int, userId: Int) {
        val isOwner = FamilyGroupTable.select {
            (FamilyGroupTable.id eq familyId) and (FamilyGroupTable.ownerUserId eq userId)
        }.count() > 0

        if (!isOwner) {
            throw AppException.Forbidden("Only family owner can perform this action")
        }
    }

    private fun memberCountOfFamily(familyId: Int): Int {
        return FamilyMemberTable.select { FamilyMemberTable.familyId eq familyId }.count().toInt()
    }

    private fun userExists(userId: Int): Boolean {
        return AuthTable.select { AuthTable.id eq userId }.count() > 0
    }

    suspend fun createFamily(ownerUserId: Int, name: String): FamilyGroup =
        newSuspendedTransaction(Dispatchers.IO) {
            if (!userExists(ownerUserId)) {
                throw AppException.NotFound("Owner user not found")
            }

            val familyId = FamilyGroupTable.insertAndGetId {
                it[FamilyGroupTable.name] = name
                it[FamilyGroupTable.ownerUserId] = ownerUserId
            }.value

            val row = FamilyGroupTable.select { FamilyGroupTable.id eq familyId }.single()
            rowToFamilyGroup(row, memberCountOfFamily(familyId))
        }

    suspend fun listFamiliesByUser(userId: Int): List<FamilyGroup> =
        newSuspendedTransaction(Dispatchers.IO) {
            val familyRows = (FamilyGroupTable innerJoin FamilyMemberTable)
                .select { FamilyMemberTable.userId eq userId }
                .orderBy(FamilyGroupTable.createdAt to SortOrder.DESC)
                .map { row ->
                    val familyId = row[FamilyGroupTable.id].value
                    rowToFamilyGroup(row, memberCountOfFamily(familyId))
                }

            familyRows.distinctBy { it.id }
        }

    suspend fun renameFamily(familyId: Int, requesterUserId: Int, name: String): FamilyGroup =
        newSuspendedTransaction(Dispatchers.IO) {
            requireFamilyExists(familyId)
            requireFamilyOwner(familyId, requesterUserId)

            val updated = FamilyGroupTable.update({ FamilyGroupTable.id eq familyId }) {
                it[FamilyGroupTable.name] = name
            }

            if (updated == 0) {
                throw AppException.Internal("Failed to rename family")
            }

            val row = FamilyGroupTable.select { FamilyGroupTable.id eq familyId }.single()
            rowToFamilyGroup(row, memberCountOfFamily(familyId))
        }

    suspend fun getFamilyMembers(familyId: Int, requesterUserId: Int): List<FamilyMember> =
        newSuspendedTransaction(Dispatchers.IO) {
            requireFamilyExists(familyId)
            requireFamilyMember(familyId, requesterUserId)

            (FamilyMemberTable innerJoin AuthTable)
                .select { FamilyMemberTable.familyId eq familyId }
                .orderBy(FamilyMemberTable.joinedAt to SortOrder.ASC)
                .map { row ->
                    FamilyMember(
                        familyId = row[FamilyMemberTable.familyId].value,
                        userId = row[FamilyMemberTable.userId].value,
                        username = row[AuthTable.username],
                        fullName = row[AuthTable.fullName],
                        avatarUrl = row[AuthTable.avatarUrl],
                        role = row[FamilyMemberTable.role],
                        joinedAt = row[FamilyMemberTable.joinedAt].toString()
                    )
                }
        }

    suspend fun addMember(familyId: Int, requesterUserId: Int, targetUserId: Int): FamilyMember =
        newSuspendedTransaction(Dispatchers.IO) {
            requireFamilyExists(familyId)
            requireFamilyOwner(familyId, requesterUserId)

            if (!userExists(targetUserId)) {
                throw AppException.NotFound("Target user not found")
            }

            val alreadyMember = FamilyMemberTable.select {
                (FamilyMemberTable.familyId eq familyId) and (FamilyMemberTable.userId eq targetUserId)
            }.count() > 0

            if (alreadyMember) {
                throw AppException.Conflict("User is already a family member")
            }

            FamilyMemberTable.insert {
                it[FamilyMemberTable.familyId] = familyId
                it[FamilyMemberTable.userId] = targetUserId
                it[FamilyMemberTable.role] = "member"
            }

            val row = (FamilyMemberTable innerJoin AuthTable)
                .select {
                    (FamilyMemberTable.familyId eq familyId) and
                        (FamilyMemberTable.userId eq targetUserId)
                }
                .single()

            FamilyMember(
                familyId = row[FamilyMemberTable.familyId].value,
                userId = row[FamilyMemberTable.userId].value,
                username = row[AuthTable.username],
                fullName = row[AuthTable.fullName],
                avatarUrl = row[AuthTable.avatarUrl],
                role = row[FamilyMemberTable.role],
                joinedAt = row[FamilyMemberTable.joinedAt].toString()
            )
        }

    suspend fun removeMember(familyId: Int, requesterUserId: Int, targetUserId: Int): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            requireFamilyExists(familyId)
            requireFamilyOwner(familyId, requesterUserId)

            val ownerId = FamilyGroupTable.select { FamilyGroupTable.id eq familyId }
                .single()[FamilyGroupTable.ownerUserId].value

            if (targetUserId == ownerId) {
                throw AppException.Conflict("Cannot remove owner from family")
            }

            val deleted = FamilyMemberTable.deleteWhere {
                (FamilyMemberTable.familyId eq familyId) and
                    (FamilyMemberTable.userId eq targetUserId)
            }

            deleted > 0
        }

    suspend fun leaveFamily(familyId: Int, userId: Int): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            requireFamilyExists(familyId)
            requireFamilyMember(familyId, userId)

            val ownerId = FamilyGroupTable.select { FamilyGroupTable.id eq familyId }
                .single()[FamilyGroupTable.ownerUserId].value

            if (ownerId == userId) {
                throw AppException.Conflict("Owner cannot leave family. Transfer ownership first")
            }

            val deleted = FamilyMemberTable.deleteWhere {
                (FamilyMemberTable.familyId eq familyId) and
                    (FamilyMemberTable.userId eq userId)
            }

            deleted > 0
        }

    private fun getMenuByIdOrThrow(familyId: Int, menuId: Int): ResultRow {
        return FamilyMenuTable.select {
            (FamilyMenuTable.id eq menuId) and (FamilyMenuTable.familyId eq familyId)
        }.singleOrNull() ?: throw AppException.NotFound("Family menu not found")
    }

    private fun getMenuItemByIdOrThrow(familyId: Int, menuId: Int, itemId: Int): ResultRow {
        return (FamilyMenuItemTable innerJoin FamilyMenuTable)
            .select {
                (FamilyMenuItemTable.id eq itemId) and
                    (FamilyMenuItemTable.familyMenuId eq menuId) and
                    (FamilyMenuTable.familyId eq familyId)
            }
            .singleOrNull() ?: throw AppException.NotFound("Family menu item not found")
    }

    suspend fun createMenu(
        familyId: Int,
        requesterUserId: Int,
        menuDate: LocalDate,
        mealType: String,
        status: String
    ): FamilyMenu = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)

        val slotExists = FamilyMenuTable.select {
            (FamilyMenuTable.familyId eq familyId) and
                (FamilyMenuTable.menuDate eq menuDate) and
                (FamilyMenuTable.mealType eq mealType)
        }.count() > 0

        if (slotExists) {
            throw AppException.Conflict("Menu slot already exists for this date and meal type")
        }

        val menuId = FamilyMenuTable.insertAndGetId {
            it[FamilyMenuTable.familyId] = familyId
            it[FamilyMenuTable.menuDate] = menuDate
            it[FamilyMenuTable.mealType] = mealType
            it[FamilyMenuTable.status] = status
        }.value

        rowToFamilyMenu(FamilyMenuTable.select { FamilyMenuTable.id eq menuId }.single())
    }

    suspend fun addMenuItem(
        familyId: Int,
        menuId: Int,
        requesterUserId: Int,
        dishId: Int?,
        personalDishId: Int?,
        note: String?
    ): FamilyMenuItem = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)
        getMenuByIdOrThrow(familyId, menuId)

        if (dishId != null) {
            val dishExists = DishTable.select { DishTable.id eq dishId }.count() > 0
            if (!dishExists) {
                throw AppException.NotFound("Dish not found")
            }
        }

        if (personalDishId != null) {
            val personalDishExists = PersonalDishTable.select { PersonalDishTable.id eq personalDishId }.count() > 0
            if (!personalDishExists) {
                throw AppException.NotFound("Personal dish not found")
            }
        }

        val duplicate = FamilyMenuItemTable.select {
            (FamilyMenuItemTable.familyMenuId eq menuId) and
                (
                    (if (dishId != null) {
                        FamilyMenuItemTable.dishId eq dishId
                    } else {
                        FamilyMenuItemTable.dishId.isNull()
                    }) and
                    (if (personalDishId != null) {
                        FamilyMenuItemTable.personalDishId eq personalDishId
                    } else {
                        FamilyMenuItemTable.personalDishId.isNull()
                    })
                )
        }.count() > 0

        if (duplicate) {
            throw AppException.Conflict("Menu item already exists")
        }

        val itemId = FamilyMenuItemTable.insertAndGetId {
            it[FamilyMenuItemTable.familyMenuId] = menuId
            it[FamilyMenuItemTable.dishId] = dishId
            it[FamilyMenuItemTable.personalDishId] = personalDishId
            it[FamilyMenuItemTable.note] = note?.trim()?.takeIf { text -> text.isNotBlank() }
        }.value

        rowToFamilyMenuItem(FamilyMenuItemTable.select { FamilyMenuItemTable.id eq itemId }.single())
    }

    suspend fun removeMenuItem(
        familyId: Int,
        menuId: Int,
        itemId: Int,
        requesterUserId: Int
    ): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)
        getMenuItemByIdOrThrow(familyId, menuId, itemId)

        val deleted = FamilyMenuItemTable.deleteWhere { FamilyMenuItemTable.id eq itemId }
        deleted > 0
    }

    suspend fun getWeeklyMenus(
        familyId: Int,
        requesterUserId: Int,
        weekStart: LocalDate
    ): List<FamilyMenuWithItems> = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)

        val weekEnd = weekStart.plusDays(6)
        val menus = FamilyMenuTable
            .select {
                (FamilyMenuTable.familyId eq familyId) and
                    (FamilyMenuTable.menuDate greaterEq weekStart) and
                    (FamilyMenuTable.menuDate lessEq weekEnd)
            }
            .orderBy(
                FamilyMenuTable.menuDate to SortOrder.ASC,
                FamilyMenuTable.mealType to SortOrder.ASC
            )
            .map { rowToFamilyMenu(it) }

        if (menus.isEmpty()) {
            return@newSuspendedTransaction emptyList()
        }

        val menuIds = menus.map { it.id }
        val menuItems = FamilyMenuItemTable
            .select { FamilyMenuItemTable.familyMenuId inList menuIds }
            .orderBy(FamilyMenuItemTable.createdAt to SortOrder.ASC)
            .map { rowToFamilyMenuItem(it) }
            .groupBy { it.familyMenuId }

        menus.map { menu ->
            FamilyMenuWithItems(
                menu = menu,
                items = menuItems[menu.id].orEmpty()
            )
        }
    }

    private fun aggregateVotesByItem(
        itemId: Int,
        requesterUserId: Int
    ): FamilyVoteActionResult {
        val votes = FamilyMenuVoteTable.select {
            FamilyMenuVoteTable.familyMenuItemId eq itemId
        }.map { row ->
            row[FamilyMenuVoteTable.userId].value to row[FamilyMenuVoteTable.voteType]
        }

        val upVotes = votes.count { it.second == "up" }
        val downVotes = votes.count { it.second == "down" }
        val userVoteType = votes.firstOrNull { it.first == requesterUserId }?.second

        return FamilyVoteActionResult(
            voted = userVoteType != null,
            voteType = userVoteType,
            upVotes = upVotes,
            downVotes = downVotes
        )
    }

    suspend fun voteMenuItem(
        familyId: Int,
        menuId: Int,
        itemId: Int,
        requesterUserId: Int,
        voteType: String
    ): FamilyVoteActionResult = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)
        getMenuItemByIdOrThrow(familyId, menuId, itemId)

        val existing = FamilyMenuVoteTable.select {
            (FamilyMenuVoteTable.familyMenuItemId eq itemId) and
                (FamilyMenuVoteTable.userId eq requesterUserId)
        }.singleOrNull()

        if (existing == null) {
            FamilyMenuVoteTable.insert {
                it[FamilyMenuVoteTable.familyMenuItemId] = itemId
                it[FamilyMenuVoteTable.userId] = requesterUserId
                it[FamilyMenuVoteTable.voteType] = voteType
            }
        } else if (existing[FamilyMenuVoteTable.voteType] == voteType) {
            FamilyMenuVoteTable.deleteWhere {
                (FamilyMenuVoteTable.familyMenuItemId eq itemId) and
                    (FamilyMenuVoteTable.userId eq requesterUserId)
            }
        } else {
            FamilyMenuVoteTable.update({
                (FamilyMenuVoteTable.familyMenuItemId eq itemId) and
                    (FamilyMenuVoteTable.userId eq requesterUserId)
            }) {
                it[FamilyMenuVoteTable.voteType] = voteType
            }
        }

        aggregateVotesByItem(itemId, requesterUserId)
    }

    suspend fun unvoteMenuItem(
        familyId: Int,
        menuId: Int,
        itemId: Int,
        requesterUserId: Int
    ): FamilyVoteActionResult = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)
        getMenuItemByIdOrThrow(familyId, menuId, itemId)

        FamilyMenuVoteTable.deleteWhere {
            (FamilyMenuVoteTable.familyMenuItemId eq itemId) and
                (FamilyMenuVoteTable.userId eq requesterUserId)
        }

        aggregateVotesByItem(itemId, requesterUserId)
    }

    suspend fun getMenuVoteSummary(
        familyId: Int,
        menuId: Int,
        requesterUserId: Int
    ): List<FamilyMenuVoteSummary> = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)
        getMenuByIdOrThrow(familyId, menuId)

        val items = FamilyMenuItemTable.select {
            FamilyMenuItemTable.familyMenuId eq menuId
        }.map { rowToFamilyMenuItem(it) }

        if (items.isEmpty()) {
            return@newSuspendedTransaction emptyList()
        }

        val itemIds = items.map { it.id }
        val voteRows = FamilyMenuVoteTable.select {
            FamilyMenuVoteTable.familyMenuItemId inList itemIds
        }.map { row ->
            Triple(
                row[FamilyMenuVoteTable.familyMenuItemId].value,
                row[FamilyMenuVoteTable.userId].value,
                row[FamilyMenuVoteTable.voteType]
            )
        }

        val grouped = voteRows.groupBy { it.first }

        items.map { item ->
            val itemVotes = grouped[item.id].orEmpty()
            val upVotes = itemVotes.count { it.third == "up" }
            val downVotes = itemVotes.count { it.third == "down" }
            val userVoteType = itemVotes.firstOrNull { it.second == requesterUserId }?.third

            FamilyMenuVoteSummary(
                familyMenuItemId = item.id,
                dishId = item.dishId,
                personalDishId = item.personalDishId,
                note = item.note,
                upVotes = upVotes,
                downVotes = downVotes,
                userVoteType = userVoteType
            )
        }
    }
}
