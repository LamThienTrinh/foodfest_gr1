package com.foodfest.app.features.family

import com.foodfest.app.core.exception.AppException
import com.foodfest.app.features.auth.AuthTable
import com.foodfest.app.features.dish.DishTable
import com.foodfest.app.features.notification.NotificationRepository
import com.foodfest.app.features.personaldish.PersonalDishTable
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
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
import java.time.DayOfWeek
import java.time.Instant
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
    val nickname = varchar("nickname", 60).nullable()
    val joinedAt = timestamp("joined_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(familyId, userId)
}

object FamilyInviteTable : IntIdTable("family_invites", "family_invite_id") {
    val familyId = reference("family_id", FamilyGroupTable, onDelete = ReferenceOption.CASCADE)
    val invitedUserId = reference("invited_user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val invitedByUserId = reference("invited_by_user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val status = varchar("status", 20)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val respondedAt = timestamp("responded_at").nullable()
}

object FamilyMenuTable : IntIdTable("family_menus", "family_menu_id") {
    val familyId = reference("family_id", FamilyGroupTable, onDelete = ReferenceOption.CASCADE)
    val menuDate = date("menu_date")
    val mealType = varchar("meal_type", 20)
    val status = varchar("status", 20)
    val isSaved = bool("is_saved").default(false)
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

object FamilySavedMealTable : IntIdTable("family_saved_meals", "family_saved_meal_id") {
    val familyId = reference("family_id", FamilyGroupTable, onDelete = ReferenceOption.CASCADE)
    val presetName = varchar("preset_name", 120)
    val createdByUserId = reference("created_by_user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

object FamilySavedMealItemTable : IntIdTable("family_saved_meal_items", "family_saved_meal_item_id") {
    val savedMealId = reference("family_saved_meal_id", FamilySavedMealTable, onDelete = ReferenceOption.CASCADE)
    val dishId = optReference("dish_id", DishTable, onDelete = ReferenceOption.SET_NULL)
    val personalDishId = optReference("personal_dish_id", PersonalDishTable, onDelete = ReferenceOption.SET_NULL)
    val note = text("note").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

object FamilyPantryItemTable : IntIdTable("family_pantry_items", "family_pantry_item_id") {
    val familyId = reference("family_id", FamilyGroupTable, onDelete = ReferenceOption.CASCADE)
    val ingredientName = varchar("ingredient_name", 120)
    val quantity = double("quantity")
    val unit = varchar("unit", 30).nullable()
    val expiryDate = date("expiry_date").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

object FamilyShoppingListTable : IntIdTable("family_shopping_lists", "family_shopping_list_id") {
    val familyId = reference("family_id", FamilyGroupTable, onDelete = ReferenceOption.CASCADE)
    val menuWeek = date("menu_week")
    val status = varchar("status", 20).default("active")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

object FamilyShoppingListItemTable : IntIdTable("family_shopping_list_items", "family_shopping_list_item_id") {
    val shoppingListId = reference("family_shopping_list_id", FamilyShoppingListTable, onDelete = ReferenceOption.CASCADE)
    val ingredientName = varchar("ingredient_name", 120)
    val requiredQty = double("required_qty").default(1.0)
    val unit = varchar("unit", 30).nullable()
    val category = varchar("category", 40).default("Khác")
    val note = text("note").nullable()
    val isPurchased = bool("is_purchased").default(false)
    val assignedToUserId = optReference("assigned_to_user_id", AuthTable, onDelete = ReferenceOption.SET_NULL)
    val usedQty = double("used_qty").nullable()
    val purchasedAt = timestamp("purchased_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

object FamilyShoppingListActivityTable :
    IntIdTable("family_shopping_list_activity", "family_shopping_list_activity_id") {
    val shoppingListId = reference("family_shopping_list_id", FamilyShoppingListTable, onDelete = ReferenceOption.CASCADE)
    val shoppingListItemId = optReference(
        "family_shopping_list_item_id",
        FamilyShoppingListItemTable,
        onDelete = ReferenceOption.SET_NULL
    )
    val actorUserId = reference("actor_user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val action = varchar("action", 40)
    val message = text("message")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

object FamilyNoteTable : IntIdTable("family_notes", "family_note_id") {
    val familyId = reference("family_id", FamilyGroupTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val message = text("message")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
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
    val nickname: String? = null,
    val joinedAt: String
)

@Serializable
data class FamilyInviteSummary(
    val id: Int,
    val familyId: Int,
    val familyName: String,
    val invitedByUserId: Int,
    val invitedByUsername: String,
    val invitedByFullName: String,
    val status: String,
    val createdAt: String
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
data class FamilyRecentMenuItem(
    val dishId: Int? = null,
    val personalDishId: Int? = null,
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
data class FamilyRecentVote(
    val familyId: Int,
    val menuId: Int,
    val menuDate: String,
    val mealType: String,
    val familyMenuItemId: Int,
    val voterUserId: Int,
    val voterName: String,
    val dishId: Int? = null,
    val personalDishId: Int? = null,
    val dishName: String,
    val voteType: String,
    val createdAt: String
)

@Serializable
data class FamilyVoteActionResult(
    val voted: Boolean,
    val voteType: String? = null,
    val upVotes: Int,
    val downVotes: Int
)

@Serializable
data class FamilySavedMealSummary(
    val id: Int,
    val familyId: Int,
    val presetName: String,
    val createdByUserId: Int,
    val createdByName: String,
    val itemsCount: Int,
    val createdAt: String
)

@Serializable
data class FamilySavedMealItem(
    val id: Int,
    val savedMealId: Int,
    val dishId: Int? = null,
    val personalDishId: Int? = null,
    val note: String? = null,
    val createdAt: String
)

@Serializable
data class FamilySavedMealDetail(
    val savedMeal: FamilySavedMealSummary,
    val items: List<FamilySavedMealItem>
)

@Serializable
data class FamilySavedMealApplyResult(
    val addedCount: Int,
    val skippedCount: Int
)

@Serializable
data class FamilyPantryItem(
    val id: Int,
    val familyId: Int,
    val ingredientName: String,
    val quantity: Double,
    val unit: String? = null,
    val expiryDate: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class FamilyPantryDeleteResult(
    val deletedCount: Int
)

@Serializable
data class FamilyShoppingList(
    val id: Int,
    val familyId: Int,
    val menuWeek: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class FamilyShoppingListItem(
    val id: Int,
    val shoppingListId: Int,
    val ingredientName: String,
    val requiredQty: Double,
    val unit: String? = null,
    val category: String,
    val note: String? = null,
    val isPurchased: Boolean,
    val assignedToUserId: Int? = null,
    val assignedToName: String? = null,
    val usedQty: Double? = null,
    val purchasedAt: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class FamilyShoppingListActivity(
    val id: Int,
    val shoppingListId: Int,
    val shoppingListItemId: Int? = null,
    val actorUserId: Int,
    val actorName: String,
    val action: String,
    val message: String,
    val createdAt: String
)

@Serializable
data class FamilyShoppingListDetail(
    val shoppingList: FamilyShoppingList,
    val items: List<FamilyShoppingListItem>,
    val activityLog: List<FamilyShoppingListActivity>
)

@Serializable
data class FamilyShoppingListPantrySyncResult(
    val updatedCount: Int
)

@Serializable
data class FamilyNote(
    val id: Int,
    val familyId: Int,
    val userId: Int,
    val authorName: String,
    val avatarUrl: String? = null,
    val message: String,
    val createdAt: String
)

private data class ParsedShoppingIngredient(
    val name: String,
    val quantity: Double,
    val unit: String?,
    val category: String,
    val note: String?
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

    private fun rowToFamilyRecentMenuItem(row: ResultRow): FamilyRecentMenuItem {
        return FamilyRecentMenuItem(
            dishId = row[FamilyMenuItemTable.dishId]?.value,
            personalDishId = row[FamilyMenuItemTable.personalDishId]?.value,
            createdAt = row[FamilyMenuItemTable.createdAt].toString()
        )
    }

    private fun rowToFamilyInviteSummary(row: ResultRow): FamilyInviteSummary {
        return FamilyInviteSummary(
            id = row[FamilyInviteTable.id].value,
            familyId = row[FamilyInviteTable.familyId].value,
            familyName = row[FamilyGroupTable.name],
            invitedByUserId = row[FamilyInviteTable.invitedByUserId].value,
            invitedByUsername = row[AuthTable.username],
            invitedByFullName = row[AuthTable.fullName],
            status = row[FamilyInviteTable.status],
            createdAt = row[FamilyInviteTable.createdAt].toString()
        )
    }

    private fun rowToFamilySavedMealSummary(
        row: ResultRow,
        itemsCount: Int
    ): FamilySavedMealSummary {
        val creatorName = row[AuthTable.fullName].trim().ifBlank { row[AuthTable.username] }
        return FamilySavedMealSummary(
            id = row[FamilySavedMealTable.id].value,
            familyId = row[FamilySavedMealTable.familyId].value,
            presetName = row[FamilySavedMealTable.presetName],
            createdByUserId = row[FamilySavedMealTable.createdByUserId].value,
            createdByName = creatorName,
            itemsCount = itemsCount,
            createdAt = row[FamilySavedMealTable.createdAt].toString()
        )
    }

    private fun rowToFamilySavedMealItem(row: ResultRow): FamilySavedMealItem {
        return FamilySavedMealItem(
            id = row[FamilySavedMealItemTable.id].value,
            savedMealId = row[FamilySavedMealItemTable.savedMealId].value,
            dishId = row[FamilySavedMealItemTable.dishId]?.value,
            personalDishId = row[FamilySavedMealItemTable.personalDishId]?.value,
            note = row[FamilySavedMealItemTable.note],
            createdAt = row[FamilySavedMealItemTable.createdAt].toString()
        )
    }

    private fun rowToFamilyPantryItem(row: ResultRow): FamilyPantryItem {
        return FamilyPantryItem(
            id = row[FamilyPantryItemTable.id].value,
            familyId = row[FamilyPantryItemTable.familyId].value,
            ingredientName = row[FamilyPantryItemTable.ingredientName],
            quantity = row[FamilyPantryItemTable.quantity],
            unit = row[FamilyPantryItemTable.unit],
            expiryDate = row[FamilyPantryItemTable.expiryDate]?.toString(),
            createdAt = row[FamilyPantryItemTable.createdAt].toString(),
            updatedAt = row[FamilyPantryItemTable.updatedAt].toString()
        )
    }

    private fun rowToFamilyShoppingList(row: ResultRow): FamilyShoppingList {
        return FamilyShoppingList(
            id = row[FamilyShoppingListTable.id].value,
            familyId = row[FamilyShoppingListTable.familyId].value,
            menuWeek = row[FamilyShoppingListTable.menuWeek].toString(),
            status = row[FamilyShoppingListTable.status],
            createdAt = row[FamilyShoppingListTable.createdAt].toString(),
            updatedAt = row[FamilyShoppingListTable.updatedAt].toString()
        )
    }

    private fun displayNameForUser(userId: Int?): String? {
        if (userId == null) return null
        return AuthTable.select { AuthTable.id eq userId }
            .singleOrNull()
            ?.let { row -> row[AuthTable.fullName].trim().ifBlank { row[AuthTable.username] } }
    }

    private fun displayNameForFamilyMember(familyId: Int, userId: Int): String {
        return (FamilyMemberTable innerJoin AuthTable)
            .select {
                (FamilyMemberTable.familyId eq familyId) and
                    (FamilyMemberTable.userId eq userId)
            }
            .singleOrNull()
            ?.let { row ->
                row[FamilyMemberTable.nickname]?.trim()?.takeIf { it.isNotBlank() }
                    ?: row[AuthTable.fullName].trim().ifBlank { row[AuthTable.username] }
            }
            ?: "Thành viên"
    }

    private fun menuItemDisplayName(dishId: Int?, personalDishId: Int?, note: String?): String {
        dishId?.let { id ->
            DishTable.select { DishTable.id eq id }
                .singleOrNull()
                ?.let { return it[DishTable.name] }
        }

        personalDishId?.let { id ->
            PersonalDishTable.select { PersonalDishTable.id eq id }
                .singleOrNull()
                ?.let { return it[PersonalDishTable.dishName] }
        }

        return note?.trim()?.takeIf { it.isNotBlank() } ?: "Món chưa đặt tên"
    }

    private fun rowToFamilyShoppingListItem(row: ResultRow): FamilyShoppingListItem {
        val assignedUserId = row[FamilyShoppingListItemTable.assignedToUserId]?.value
        return FamilyShoppingListItem(
            id = row[FamilyShoppingListItemTable.id].value,
            shoppingListId = row[FamilyShoppingListItemTable.shoppingListId].value,
            ingredientName = row[FamilyShoppingListItemTable.ingredientName],
            requiredQty = row[FamilyShoppingListItemTable.requiredQty],
            unit = row[FamilyShoppingListItemTable.unit],
            category = row[FamilyShoppingListItemTable.category],
            note = row[FamilyShoppingListItemTable.note],
            isPurchased = row[FamilyShoppingListItemTable.isPurchased],
            assignedToUserId = assignedUserId,
            assignedToName = displayNameForUser(assignedUserId),
            usedQty = row[FamilyShoppingListItemTable.usedQty],
            purchasedAt = row[FamilyShoppingListItemTable.purchasedAt]?.toString(),
            createdAt = row[FamilyShoppingListItemTable.createdAt].toString(),
            updatedAt = row[FamilyShoppingListItemTable.updatedAt].toString()
        )
    }

    private fun rowToFamilyShoppingListActivity(row: ResultRow): FamilyShoppingListActivity {
        val actorName = row[AuthTable.fullName].trim().ifBlank { row[AuthTable.username] }
        return FamilyShoppingListActivity(
            id = row[FamilyShoppingListActivityTable.id].value,
            shoppingListId = row[FamilyShoppingListActivityTable.shoppingListId].value,
            shoppingListItemId = row[FamilyShoppingListActivityTable.shoppingListItemId]?.value,
            actorUserId = row[FamilyShoppingListActivityTable.actorUserId].value,
            actorName = actorName,
            action = row[FamilyShoppingListActivityTable.action],
            message = row[FamilyShoppingListActivityTable.message],
            createdAt = row[FamilyShoppingListActivityTable.createdAt].toString()
        )
    }

    private fun rowToFamilyNote(row: ResultRow): FamilyNote {
        val authorName = row[AuthTable.fullName].trim().ifBlank { row[AuthTable.username] }
        return FamilyNote(
            id = row[FamilyNoteTable.id].value,
            familyId = row[FamilyNoteTable.familyId].value,
            userId = row[FamilyNoteTable.userId].value,
            authorName = authorName,
            avatarUrl = row[AuthTable.avatarUrl],
            message = row[FamilyNoteTable.message],
            createdAt = row[FamilyNoteTable.createdAt].toString()
        )
    }

    private fun rowToFamilyMember(row: ResultRow): FamilyMember {
        return FamilyMember(
            familyId = row[FamilyMemberTable.familyId].value,
            userId = row[FamilyMemberTable.userId].value,
            username = row[AuthTable.username],
            fullName = row[AuthTable.fullName],
            avatarUrl = row[AuthTable.avatarUrl],
            role = row[FamilyMemberTable.role],
            nickname = row[FamilyMemberTable.nickname],
            joinedAt = row[FamilyMemberTable.joinedAt].toString()
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

    private fun familyNameExists(name: String, excludeFamilyId: Int? = null): Boolean {
        return if (excludeFamilyId != null) {
            FamilyGroupTable.select {
                (FamilyGroupTable.name eq name) and (FamilyGroupTable.id neq excludeFamilyId)
            }.count() > 0
        } else {
            FamilyGroupTable.select { FamilyGroupTable.name eq name }.count() > 0
        }
    }

    private fun savedMealNameExists(
        familyId: Int,
        presetName: String,
        excludeSavedMealId: Int? = null
    ): Boolean {
        return if (excludeSavedMealId != null) {
            FamilySavedMealTable.select {
                (FamilySavedMealTable.familyId eq familyId) and
                    (FamilySavedMealTable.presetName eq presetName) and
                    (FamilySavedMealTable.id neq excludeSavedMealId)
            }.count() > 0
        } else {
            FamilySavedMealTable.select {
                (FamilySavedMealTable.familyId eq familyId) and
                    (FamilySavedMealTable.presetName eq presetName)
            }.count() > 0
        }
    }

    private fun countSavedMealItems(savedMealId: Int): Int {
        return FamilySavedMealItemTable.select {
            FamilySavedMealItemTable.savedMealId eq savedMealId
        }.count().toInt()
    }

    private fun getSavedMealRowOrThrow(familyId: Int, savedMealId: Int): ResultRow {
        return FamilySavedMealTable.select {
            (FamilySavedMealTable.id eq savedMealId) and
                (FamilySavedMealTable.familyId eq familyId)
        }.singleOrNull() ?: throw AppException.NotFound("Saved meal not found")
    }

    private fun getPantryItemRowOrThrow(familyId: Int, itemId: Int): ResultRow {
        return FamilyPantryItemTable.select {
            (FamilyPantryItemTable.id eq itemId) and
                (FamilyPantryItemTable.familyId eq familyId)
        }.singleOrNull() ?: throw AppException.NotFound("Pantry item not found")
    }

    private fun getUserByUsernameOrThrow(username: String): ResultRow {
        val normalized = username.trim()
        if (normalized.isBlank()) {
            throw AppException.Validation("Username is required")
        }

        return AuthTable.select { AuthTable.username eq normalized }
            .singleOrNull() ?: throw AppException.NotFound("User not found")
    }

    suspend fun createFamily(ownerUserId: Int, name: String): FamilyGroup =
        newSuspendedTransaction(Dispatchers.IO) {
            if (!userExists(ownerUserId)) {
                throw AppException.NotFound("Owner user not found")
            }

            // Keep family names unique across the system.
            if (familyNameExists(name)) {
                throw AppException.Conflict("Family name already exists")
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

            // Prevent renaming to an existing family name.
            if (familyNameExists(name, excludeFamilyId = familyId)) {
                throw AppException.Conflict("Family name already exists")
            }

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
                .map(::rowToFamilyMember)
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

            rowToFamilyMember(row)
        }

    suspend fun updateMyNickname(
        familyId: Int,
        requesterUserId: Int,
        nickname: String?
    ): FamilyMember = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)

        FamilyMemberTable.update({
            (FamilyMemberTable.familyId eq familyId) and
                (FamilyMemberTable.userId eq requesterUserId)
        }) {
            it[FamilyMemberTable.nickname] = nickname
        }

        (FamilyMemberTable innerJoin AuthTable)
            .select {
                (FamilyMemberTable.familyId eq familyId) and
                    (FamilyMemberTable.userId eq requesterUserId)
            }
            .singleOrNull()
            ?.let(::rowToFamilyMember)
            ?: throw AppException.NotFound("Member not found in family")
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

    suspend fun createFamilyInvite(
        familyId: Int,
        requesterUserId: Int,
        targetUsername: String
    ): FamilyInviteSummary = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyOwner(familyId, requesterUserId)

        val invitedRow = getUserByUsernameOrThrow(targetUsername)
        val invitedUserId = invitedRow[AuthTable.id].value

        if (invitedUserId == requesterUserId) {
            throw AppException.Conflict("Cannot invite yourself")
        }

        val alreadyMember = FamilyMemberTable.select {
            (FamilyMemberTable.familyId eq familyId) and
                (FamilyMemberTable.userId eq invitedUserId)
        }.count() > 0

        if (alreadyMember) {
            throw AppException.Conflict("User is already a family member")
        }

        val pendingInviteExists = FamilyInviteTable.select {
            (FamilyInviteTable.familyId eq familyId) and
                (FamilyInviteTable.invitedUserId eq invitedUserId) and
                (FamilyInviteTable.status eq "pending")
        }.count() > 0

        if (pendingInviteExists) {
            throw AppException.Conflict("Pending invite already exists")
        }

        val inviteId = FamilyInviteTable.insertAndGetId {
            it[FamilyInviteTable.familyId] = familyId
            it[FamilyInviteTable.invitedUserId] = invitedUserId
            it[FamilyInviteTable.invitedByUserId] = requesterUserId
            it[FamilyInviteTable.status] = "pending"
        }.value

        val familyName = FamilyGroupTable.select { FamilyGroupTable.id eq familyId }
            .single()[FamilyGroupTable.name]
        val inviterName = AuthTable.select { AuthTable.id eq requesterUserId }
            .single()[AuthTable.fullName]
            .trim()
            .ifBlank { "Một thành viên" }
        NotificationRepository.insertEventNotification(
            userId = invitedUserId,
            type = "family_invite",
            title = "Lời mời vào gia đình",
            message = "$inviterName mời bạn vào gia đình $familyName",
            relatedEntityType = "family_invite",
            relatedEntityId = inviteId,
            actionUrl = "foodfest://families/invites/$inviteId"
        )

        val row = FamilyInviteTable
            .join(FamilyGroupTable, JoinType.INNER, additionalConstraint = {
                FamilyInviteTable.familyId eq FamilyGroupTable.id
            })
            .join(AuthTable, JoinType.INNER, additionalConstraint = {
                FamilyInviteTable.invitedByUserId eq AuthTable.id
            })
            .select { FamilyInviteTable.id eq inviteId }
            .single()

        rowToFamilyInviteSummary(row)
    }

    suspend fun listPendingInvitesForUser(userId: Int): List<FamilyInviteSummary> =
        newSuspendedTransaction(Dispatchers.IO) {
            FamilyInviteTable
                .join(FamilyGroupTable, JoinType.INNER, additionalConstraint = {
                    FamilyInviteTable.familyId eq FamilyGroupTable.id
                })
                .join(AuthTable, JoinType.INNER, additionalConstraint = {
                    FamilyInviteTable.invitedByUserId eq AuthTable.id
                })
                .select {
                    (FamilyInviteTable.invitedUserId eq userId) and
                        (FamilyInviteTable.status eq "pending")
                }
                .orderBy(FamilyInviteTable.createdAt to SortOrder.DESC)
                .map { rowToFamilyInviteSummary(it) }
        }

    suspend fun respondToInvite(
        inviteId: Int,
        userId: Int,
        accept: Boolean
    ): FamilyInviteSummary = newSuspendedTransaction(Dispatchers.IO) {
        val inviteRow = FamilyInviteTable.select {
            (FamilyInviteTable.id eq inviteId) and
                (FamilyInviteTable.invitedUserId eq userId)
        }.singleOrNull() ?: throw AppException.NotFound("Invite not found")

        val currentStatus = inviteRow[FamilyInviteTable.status]
        if (currentStatus != "pending") {
            throw AppException.Conflict("Invite already responded")
        }

        val familyId = inviteRow[FamilyInviteTable.familyId].value
        if (accept) {
            val alreadyMember = FamilyMemberTable.select {
                (FamilyMemberTable.familyId eq familyId) and
                    (FamilyMemberTable.userId eq userId)
            }.count() > 0

            if (!alreadyMember) {
                FamilyMemberTable.insert {
                    it[FamilyMemberTable.familyId] = familyId
                    it[FamilyMemberTable.userId] = userId
                    it[FamilyMemberTable.role] = "member"
                }
            }
        }

        val nextStatus = if (accept) "accepted" else "declined"
        FamilyInviteTable.update({ FamilyInviteTable.id eq inviteId }) {
            it[FamilyInviteTable.status] = nextStatus
            it[FamilyInviteTable.respondedAt] = CurrentTimestamp()
        }

        NotificationRepository.markRelatedEventRead(
            userId = userId,
            type = "family_invite",
            relatedEntityType = "family_invite",
            relatedEntityId = inviteId
        )

        val row = FamilyInviteTable
            .join(FamilyGroupTable, JoinType.INNER, additionalConstraint = {
                FamilyInviteTable.familyId eq FamilyGroupTable.id
            })
            .join(AuthTable, JoinType.INNER, additionalConstraint = {
                FamilyInviteTable.invitedByUserId eq AuthTable.id
            })
            .select { FamilyInviteTable.id eq inviteId }
            .single()

        rowToFamilyInviteSummary(row)
    }

    suspend fun listSavedMeals(
        familyId: Int,
        requesterUserId: Int
    ): List<FamilySavedMealSummary> = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)

        // Use per-record counting to keep logic simple for small lists.
        FamilySavedMealTable
            .join(AuthTable, JoinType.INNER, additionalConstraint = {
                FamilySavedMealTable.createdByUserId eq AuthTable.id
            })
            .select { FamilySavedMealTable.familyId eq familyId }
            .orderBy(FamilySavedMealTable.createdAt to SortOrder.DESC)
            .map { row ->
                val savedMealId = row[FamilySavedMealTable.id].value
                rowToFamilySavedMealSummary(row, countSavedMealItems(savedMealId))
            }
    }

    suspend fun getSavedMealDetail(
        familyId: Int,
        savedMealId: Int,
        requesterUserId: Int
    ): FamilySavedMealDetail = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)

        val savedMealRow = getSavedMealRowOrThrow(familyId, savedMealId)
        val summaryRow = FamilySavedMealTable
            .join(AuthTable, JoinType.INNER, additionalConstraint = {
                FamilySavedMealTable.createdByUserId eq AuthTable.id
            })
            .select { FamilySavedMealTable.id eq savedMealRow[FamilySavedMealTable.id].value }
            .single()

        val items = FamilySavedMealItemTable
            .select { FamilySavedMealItemTable.savedMealId eq savedMealId }
            .orderBy(FamilySavedMealItemTable.createdAt to SortOrder.ASC)
            .map { rowToFamilySavedMealItem(it) }

        val summary = rowToFamilySavedMealSummary(summaryRow, items.size)
        FamilySavedMealDetail(savedMeal = summary, items = items)
    }

    suspend fun createSavedMealFromMenu(
        familyId: Int,
        menuId: Int,
        requesterUserId: Int,
        presetName: String
    ): FamilySavedMealDetail = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)
        getMenuByIdOrThrow(familyId, menuId)

        // Prevent duplicate preset names inside the same family.
        if (savedMealNameExists(familyId, presetName)) {
            throw AppException.Conflict("Preset name already exists")
        }

        val menuItems = FamilyMenuItemTable
            .select { FamilyMenuItemTable.familyMenuId eq menuId }
            .orderBy(FamilyMenuItemTable.createdAt to SortOrder.ASC)
            .map { rowToFamilyMenuItem(it) }

        if (menuItems.isEmpty()) {
            throw AppException.Validation("Menu has no items to save")
        }

        val savedMealId = FamilySavedMealTable.insertAndGetId {
            it[FamilySavedMealTable.familyId] = familyId
            it[FamilySavedMealTable.presetName] = presetName
            it[FamilySavedMealTable.createdByUserId] = requesterUserId
        }.value

        // Mark source menu as saved so weekly cleanup will keep it.
        markMenuAsSaved(menuId)

        menuItems.forEach { item ->
            FamilySavedMealItemTable.insert {
                it[FamilySavedMealItemTable.savedMealId] = savedMealId
                it[FamilySavedMealItemTable.dishId] = item.dishId
                it[FamilySavedMealItemTable.personalDishId] = item.personalDishId
                it[FamilySavedMealItemTable.note] = item.note
            }
        }

        getSavedMealDetail(familyId, savedMealId, requesterUserId)
    }

    /**
     * Flags a menu as saved to prevent weekly cleanup from deleting it.
     */
    private fun markMenuAsSaved(menuId: Int) {
        FamilyMenuTable.update({ FamilyMenuTable.id eq menuId }) {
            it[FamilyMenuTable.isSaved] = true
        }
    }

    suspend fun applySavedMealToMenu(
        familyId: Int,
        savedMealId: Int,
        menuId: Int,
        requesterUserId: Int
    ): FamilySavedMealApplyResult = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)
        getMenuByIdOrThrow(familyId, menuId)
        getSavedMealRowOrThrow(familyId, savedMealId)

        val items = FamilySavedMealItemTable
            .select { FamilySavedMealItemTable.savedMealId eq savedMealId }
            .orderBy(FamilySavedMealItemTable.createdAt to SortOrder.ASC)
            .map { rowToFamilySavedMealItem(it) }

        var addedCount = 0
        var skippedCount = 0

        items.forEach { item ->
            val inserted = insertMenuItemIfMissing(
                menuId = menuId,
                dishId = item.dishId,
                personalDishId = item.personalDishId,
                note = item.note
            )
            if (inserted) {
                addedCount += 1
            } else {
                skippedCount += 1
            }
        }

        FamilySavedMealApplyResult(
            addedCount = addedCount,
            skippedCount = skippedCount
        )
    }

    suspend fun deleteSavedMeal(
        familyId: Int,
        savedMealId: Int,
        requesterUserId: Int
    ): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)

        val savedMealRow = getSavedMealRowOrThrow(familyId, savedMealId)
        val createdBy = savedMealRow[FamilySavedMealTable.createdByUserId].value
        val ownerId = FamilyGroupTable.select { FamilyGroupTable.id eq familyId }
            .single()[FamilyGroupTable.ownerUserId].value

        // Only owner or creator can delete saved meals.
        if (requesterUserId != ownerId && requesterUserId != createdBy) {
            throw AppException.Forbidden("Only owner or creator can delete this preset")
        }

        val deleted = FamilySavedMealTable.deleteWhere { FamilySavedMealTable.id eq savedMealId }
        deleted > 0
    }

    /**
     * Returns pantry items for a family.
     */
    suspend fun listPantryItems(
        familyId: Int,
        requesterUserId: Int
    ): List<FamilyPantryItem> = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)

        FamilyPantryItemTable
            .select { FamilyPantryItemTable.familyId eq familyId }
            .orderBy(FamilyPantryItemTable.expiryDate, SortOrder.ASC)
            .map(::rowToFamilyPantryItem)
    }

    /**
     * Creates a new pantry item for the family.
     */
    suspend fun createPantryItem(
        familyId: Int,
        requesterUserId: Int,
        ingredientName: String,
        quantity: Double,
        unit: String?,
        expiryDate: LocalDate?
    ): FamilyPantryItem = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)

        val itemId = FamilyPantryItemTable.insertAndGetId {
            it[FamilyPantryItemTable.familyId] = familyId
            it[FamilyPantryItemTable.ingredientName] = ingredientName
            it[FamilyPantryItemTable.quantity] = quantity
            it[FamilyPantryItemTable.unit] = unit
            it[FamilyPantryItemTable.expiryDate] = expiryDate
        }.value

        FamilyPantryItemTable.select { FamilyPantryItemTable.id eq itemId }
            .singleOrNull()
            ?.let(::rowToFamilyPantryItem)
            ?: throw AppException.NotFound("Pantry item not found")
    }

    /**
     * Updates a pantry item fields for the family.
     */
    suspend fun updatePantryItem(
        familyId: Int,
        requesterUserId: Int,
        itemId: Int,
        ingredientName: String?,
        quantity: Double?,
        unit: String?,
        expiryDate: LocalDate?,
        setUnit: Boolean,
        setExpiryDate: Boolean
    ): FamilyPantryItem = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)
        getPantryItemRowOrThrow(familyId, itemId)

        FamilyPantryItemTable.update({
            (FamilyPantryItemTable.id eq itemId) and (FamilyPantryItemTable.familyId eq familyId)
        }) {
            if (ingredientName != null) {
                it[FamilyPantryItemTable.ingredientName] = ingredientName
            }
            if (quantity != null) {
                it[FamilyPantryItemTable.quantity] = quantity
            }
            if (setUnit) {
                it[FamilyPantryItemTable.unit] = unit
            }
            if (setExpiryDate) {
                it[FamilyPantryItemTable.expiryDate] = expiryDate
            }
            it[FamilyPantryItemTable.updatedAt] = CurrentTimestamp()
        }

        FamilyPantryItemTable.select { FamilyPantryItemTable.id eq itemId }
            .singleOrNull()
            ?.let(::rowToFamilyPantryItem)
            ?: throw AppException.NotFound("Pantry item not found")
    }

    /**
     * Deletes a pantry item from a family.
     */
    suspend fun deletePantryItem(
        familyId: Int,
        requesterUserId: Int,
        itemId: Int
    ): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)

        FamilyPantryItemTable.deleteWhere {
            (FamilyPantryItemTable.familyId eq familyId) and
                (FamilyPantryItemTable.id eq itemId)
        } > 0
    }

    /**
     * Deletes all expired pantry items for the family.
     */
    suspend fun deleteExpiredPantryItems(
        familyId: Int,
        requesterUserId: Int,
        today: LocalDate
    ): Int = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)

        FamilyPantryItemTable.deleteWhere {
            (FamilyPantryItemTable.familyId eq familyId) and
                (FamilyPantryItemTable.expiryDate less today)
        }
    }

    /**
     * Phase 5.3: Lists recent family notes for lightweight chat/coordination.
     */
    suspend fun listFamilyNotes(
        familyId: Int,
        requesterUserId: Int,
        limit: Int
    ): List<FamilyNote> = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)

        (FamilyNoteTable innerJoin AuthTable)
            .select { FamilyNoteTable.familyId eq familyId }
            .orderBy(FamilyNoteTable.createdAt to SortOrder.DESC)
            .limit(limit.coerceIn(1, 100))
            .map(::rowToFamilyNote)
            .reversed()
    }

    /**
     * Phase 5.3: Creates a family note message visible only to members of that family.
     */
    suspend fun createFamilyNote(
        familyId: Int,
        requesterUserId: Int,
        message: String
    ): FamilyNote = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)

        val noteId = FamilyNoteTable.insertAndGetId {
            it[FamilyNoteTable.familyId] = familyId
            it[FamilyNoteTable.userId] = requesterUserId
            it[FamilyNoteTable.message] = message
        }.value

        (FamilyNoteTable innerJoin AuthTable)
            .select { FamilyNoteTable.id eq noteId }
            .singleOrNull()
            ?.let(::rowToFamilyNote)
            ?: throw AppException.NotFound("Family note not found")
    }

    private fun getShoppingListRowOrThrow(familyId: Int, shoppingListId: Int): ResultRow {
        return FamilyShoppingListTable.select {
            (FamilyShoppingListTable.id eq shoppingListId) and
                (FamilyShoppingListTable.familyId eq familyId)
        }.singleOrNull() ?: throw AppException.NotFound("Shopping list not found")
    }

    private fun getShoppingItemRowOrThrow(shoppingListId: Int, itemId: Int): ResultRow {
        return FamilyShoppingListItemTable.select {
            (FamilyShoppingListItemTable.id eq itemId) and
                (FamilyShoppingListItemTable.shoppingListId eq shoppingListId)
        }.singleOrNull() ?: throw AppException.NotFound("Shopping list item not found")
    }

    private fun appendShoppingActivity(
        shoppingListId: Int,
        itemId: Int?,
        actorUserId: Int,
        action: String,
        message: String
    ) {
        FamilyShoppingListActivityTable.insert {
            it[FamilyShoppingListActivityTable.shoppingListId] = shoppingListId
            it[FamilyShoppingListActivityTable.shoppingListItemId] = itemId
            it[FamilyShoppingListActivityTable.actorUserId] = actorUserId
            it[FamilyShoppingListActivityTable.action] = action
            it[FamilyShoppingListActivityTable.message] = message
        }
    }

    private fun getShoppingListDetailInCurrentTx(shoppingListId: Int): FamilyShoppingListDetail {
        val listRow = FamilyShoppingListTable.select { FamilyShoppingListTable.id eq shoppingListId }
            .singleOrNull() ?: throw AppException.NotFound("Shopping list not found")
        val items = FamilyShoppingListItemTable
            .select { FamilyShoppingListItemTable.shoppingListId eq shoppingListId }
            .orderBy(
                FamilyShoppingListItemTable.category to SortOrder.ASC,
                FamilyShoppingListItemTable.ingredientName to SortOrder.ASC
            )
            .map(::rowToFamilyShoppingListItem)
        val activity = (FamilyShoppingListActivityTable innerJoin AuthTable)
            .select { FamilyShoppingListActivityTable.shoppingListId eq shoppingListId }
            .orderBy(FamilyShoppingListActivityTable.createdAt to SortOrder.DESC)
            .limit(30)
            .map(::rowToFamilyShoppingListActivity)

        return FamilyShoppingListDetail(
            shoppingList = rowToFamilyShoppingList(listRow),
            items = items,
            activityLog = activity
        )
    }

    private fun normalizeIngredientKey(value: String): String {
        return value
            .lowercase()
            .replace(Regex("\\(.*?\\)"), " ")
            .replace(Regex("\\d+[\\d.,/]*"), " ")
            .replace(Regex("[^\\p{L}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun parseQuantityAndUnit(value: String): Pair<Double, String?> {
        val normalized = value.trim()
        val match = Regex("^([0-9]+(?:[\\.,][0-9]+)?)(?:\\s*)([\\p{L}]+)?").find(normalized)
            ?: return 1.0 to null
        val quantity = match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 1.0
        val unit = match.groupValues.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
        return quantity to unit
    }

    private fun parseIngredientLine(rawLine: String): ParsedShoppingIngredient? {
        val cleaned = rawLine.trim()
            .removePrefix("-")
            .removePrefix("*")
            .trim()
        if (cleaned.isBlank()) return null

        val (name, quantity, unit) = if (cleaned.contains(":")) {
            val candidateName = cleaned.substringBefore(":").trim()
            val (parsedQty, parsedUnit) = parseQuantityAndUnit(cleaned.substringAfter(":"))
            Triple(candidateName, parsedQty, parsedUnit)
        } else {
            val match = Regex("^([0-9]+(?:[\\.,][0-9]+)?)(?:\\s*)([\\p{L}]+)?\\s+(.+)$").find(cleaned)
            if (match != null) {
                Triple(
                    match.groupValues[3].substringBefore(",").trim(),
                    match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 1.0,
                    match.groupValues[2].trim().takeIf { it.isNotBlank() }
                )
            } else {
                Triple(cleaned.substringBefore(",").trim(), 1.0, null)
            }
        }

        val normalizedName = name.trim().takeIf { it.isNotBlank() } ?: return null
        return ParsedShoppingIngredient(
            name = normalizedName,
            quantity = quantity.coerceAtLeast(0.1),
            unit = unit?.take(30),
            category = inferShoppingCategory(normalizedName),
            note = "Tự động tạo từ menu tuần"
        )
    }

    private fun inferShoppingCategory(ingredientName: String): String {
        val value = ingredientName.lowercase()
        return when {
            listOf("rau", "cải", "hành", "ngò", "ớt", "cà", "dưa", "bí", "nấm").any { it in value } -> "Rau"
            listOf("thịt", "heo", "bò", "gà", "vịt", "sườn", "xương").any { it in value } -> "Thịt"
            listOf("cá", "tôm", "mực", "nghêu", "sò", "hải sản").any { it in value } -> "Hải sản"
            listOf("muối", "đường", "mắm", "nước tương", "tiêu", "dầu", "bột", "gia vị").any { it in value } -> "Gia vị"
            listOf("trứng", "sữa", "phô mai", "bơ").any { it in value } -> "Trứng/Sữa"
            else -> "Khác"
        }
    }

    private fun extractShoppingIngredientsFromMenu(familyId: Int, weekStart: LocalDate): List<ParsedShoppingIngredient> {
        val weekEnd = weekStart.plusDays(6)
        val menuIds = FamilyMenuTable.select {
            (FamilyMenuTable.familyId eq familyId) and
                (FamilyMenuTable.menuDate greaterEq weekStart) and
                (FamilyMenuTable.menuDate lessEq weekEnd)
        }.map { row -> row[FamilyMenuTable.id].value }

        if (menuIds.isEmpty()) return emptyList()

        val menuItems = FamilyMenuItemTable.select {
            FamilyMenuItemTable.familyMenuId inList menuIds
        }.map(::rowToFamilyMenuItem)

        return menuItems.flatMap { item ->
            val ingredients = item.dishId?.let { dishId ->
                DishTable.select { DishTable.id eq dishId }.singleOrNull()?.get(DishTable.ingredients)
            } ?: item.personalDishId?.let { personalDishId ->
                PersonalDishTable.select { PersonalDishTable.id eq personalDishId }
                    .singleOrNull()
                    ?.get(PersonalDishTable.ingredients)
            }
            ingredients.orEmpty().lines().mapNotNull(::parseIngredientLine)
        }
    }

    private fun subtractPantryFromRequired(
        familyId: Int,
        requiredIngredients: List<ParsedShoppingIngredient>
    ): List<ParsedShoppingIngredient> {
        val pantry = FamilyPantryItemTable.select { FamilyPantryItemTable.familyId eq familyId }
            .map(::rowToFamilyPantryItem)

        return requiredIngredients.mapNotNull { required ->
            val requiredKey = normalizeIngredientKey(required.name)
            val pantryQty = pantry
                .filter { pantryItem ->
                    val pantryKey = normalizeIngredientKey(pantryItem.ingredientName)
                    val sameIngredient = pantryKey == requiredKey ||
                        pantryKey.contains(requiredKey) ||
                        requiredKey.contains(pantryKey)
                    val compatibleUnit = required.unit.isNullOrBlank() ||
                        pantryItem.unit.isNullOrBlank() ||
                        pantryItem.unit.equals(required.unit, ignoreCase = true)
                    sameIngredient && compatibleUnit
                }
                .sumOf { it.quantity }
            val missingQty = required.quantity - pantryQty
            if (missingQty <= 0.0) {
                null
            } else {
                required.copy(quantity = missingQty)
            }
        }
    }

    private fun mergeShoppingIngredients(
        ingredients: List<ParsedShoppingIngredient>
    ): List<ParsedShoppingIngredient> {
        return ingredients
            .groupBy { normalizeIngredientKey(it.name) to it.unit.orEmpty().lowercase() }
            .map { (_, grouped) ->
                val first = grouped.first()
                first.copy(quantity = grouped.sumOf { it.quantity })
            }
            .sortedWith(compareBy<ParsedShoppingIngredient> { it.category }.thenBy { it.name })
    }

    /**
     * Phase 4.3: Generates a shopping list from weekly menu ingredients minus current pantry stock.
     */
    suspend fun generateShoppingList(
        familyId: Int,
        requesterUserId: Int,
        weekStart: LocalDate
    ): FamilyShoppingListDetail = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)

        val sourceIngredients = extractShoppingIngredientsFromMenu(familyId, weekStart)
        val missingIngredients = mergeShoppingIngredients(
            subtractPantryFromRequired(
                familyId = familyId,
                requiredIngredients = mergeShoppingIngredients(sourceIngredients)
            )
        )

        val existingListId = FamilyShoppingListTable.select {
            (FamilyShoppingListTable.familyId eq familyId) and
                (FamilyShoppingListTable.menuWeek eq weekStart)
        }.singleOrNull()?.get(FamilyShoppingListTable.id)?.value

        val shoppingListId = existingListId ?: FamilyShoppingListTable.insertAndGetId {
            it[FamilyShoppingListTable.familyId] = familyId
            it[FamilyShoppingListTable.menuWeek] = weekStart
            it[FamilyShoppingListTable.status] = "active"
        }.value

        // Regeneration intentionally replaces item rows so the list reflects the current weekly menu.
        FamilyShoppingListActivityTable.deleteWhere {
            FamilyShoppingListActivityTable.shoppingListId eq shoppingListId
        }
        FamilyShoppingListItemTable.deleteWhere {
            FamilyShoppingListItemTable.shoppingListId eq shoppingListId
        }
        FamilyShoppingListTable.update({ FamilyShoppingListTable.id eq shoppingListId }) {
            it[FamilyShoppingListTable.status] = "active"
            it[FamilyShoppingListTable.updatedAt] = CurrentTimestamp()
        }

        missingIngredients.forEach { ingredient ->
            FamilyShoppingListItemTable.insert {
                it[FamilyShoppingListItemTable.shoppingListId] = shoppingListId
                it[FamilyShoppingListItemTable.ingredientName] = ingredient.name
                it[FamilyShoppingListItemTable.requiredQty] = ingredient.quantity
                it[FamilyShoppingListItemTable.unit] = ingredient.unit
                it[FamilyShoppingListItemTable.category] = ingredient.category
                it[FamilyShoppingListItemTable.note] = ingredient.note
            }
        }

        appendShoppingActivity(
            shoppingListId = shoppingListId,
            itemId = null,
            actorUserId = requesterUserId,
            action = "generated",
            message = "Đã tạo shopping list từ menu tuần, sau khi trừ tồn kho Pantry"
        )

        getShoppingListDetailInCurrentTx(shoppingListId)
    }

    /**
     * Phase 4.4: Returns checklist detail including items and recent activity log.
     */
    suspend fun getShoppingListDetail(
        familyId: Int,
        requesterUserId: Int,
        shoppingListId: Int
    ): FamilyShoppingListDetail = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)
        getShoppingListRowOrThrow(familyId, shoppingListId)
        getShoppingListDetailInCurrentTx(shoppingListId)
    }

    /**
     * Phase 4.4: Updates checklist item purchased/assigned/used state and writes activity log.
     */
    suspend fun updateShoppingListItem(
        familyId: Int,
        requesterUserId: Int,
        shoppingListId: Int,
        itemId: Int,
        isPurchased: Boolean?,
        assignedToUserId: Int?,
        setAssignedTo: Boolean,
        usedQty: Double?,
        setUsedQty: Boolean
    ): FamilyShoppingListDetail = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)
        getShoppingListRowOrThrow(familyId, shoppingListId)
        val itemRow = getShoppingItemRowOrThrow(shoppingListId, itemId)

        if (assignedToUserId != null) {
            requireFamilyMember(familyId, assignedToUserId)
        }

        val itemName = itemRow[FamilyShoppingListItemTable.ingredientName]
        FamilyShoppingListItemTable.update({ FamilyShoppingListItemTable.id eq itemId }) {
            if (isPurchased != null) {
                it[FamilyShoppingListItemTable.isPurchased] = isPurchased
                it[FamilyShoppingListItemTable.purchasedAt] = if (isPurchased) Instant.now() else null
                if (isPurchased && itemRow[FamilyShoppingListItemTable.assignedToUserId] == null) {
                    it[FamilyShoppingListItemTable.assignedToUserId] = requesterUserId
                }
            }
            if (setAssignedTo) {
                it[FamilyShoppingListItemTable.assignedToUserId] = assignedToUserId
            }
            if (setUsedQty) {
                it[FamilyShoppingListItemTable.usedQty] = usedQty
            }
            it[FamilyShoppingListItemTable.updatedAt] = CurrentTimestamp()
        }

        val message = when {
            isPurchased == true -> "Đã mua/chuẩn bị $itemName"
            isPurchased == false -> "Bỏ tick $itemName"
            setAssignedTo -> "Cập nhật người phụ trách $itemName"
            setUsedQty -> "Cập nhật số lượng đã dùng cho $itemName"
            else -> "Cập nhật $itemName"
        }
        appendShoppingActivity(shoppingListId, itemId, requesterUserId, "item_updated", message)

        getShoppingListDetailInCurrentTx(shoppingListId)
    }

    /**
     * Phase 4.3: Marks every checklist item as purchased.
     */
    suspend fun markAllShoppingItemsPurchased(
        familyId: Int,
        requesterUserId: Int,
        shoppingListId: Int
    ): FamilyShoppingListDetail = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)
        getShoppingListRowOrThrow(familyId, shoppingListId)

        FamilyShoppingListItemTable.select {
            FamilyShoppingListItemTable.shoppingListId eq shoppingListId
        }.forEach { row ->
            FamilyShoppingListItemTable.update({ FamilyShoppingListItemTable.id eq row[FamilyShoppingListItemTable.id] }) {
                it[FamilyShoppingListItemTable.isPurchased] = true
                it[FamilyShoppingListItemTable.purchasedAt] = Instant.now()
                if (row[FamilyShoppingListItemTable.assignedToUserId] == null) {
                    it[FamilyShoppingListItemTable.assignedToUserId] = requesterUserId
                }
                it[FamilyShoppingListItemTable.updatedAt] = CurrentTimestamp()
            }
        }

        appendShoppingActivity(
            shoppingListId = shoppingListId,
            itemId = null,
            actorUserId = requesterUserId,
            action = "mark_all",
            message = "Đã đánh dấu mua hết danh sách"
        )
        getShoppingListDetailInCurrentTx(shoppingListId)
    }

    private fun addPurchasedItemToPantry(
        familyId: Int,
        ingredientName: String,
        quantity: Double,
        unit: String?
    ) {
        val unitFilter = if (unit == null) {
            FamilyPantryItemTable.unit.isNull()
        } else {
            FamilyPantryItemTable.unit eq unit
        }
        val existing = FamilyPantryItemTable.select {
            (FamilyPantryItemTable.familyId eq familyId) and
                (FamilyPantryItemTable.ingredientName eq ingredientName) and
                unitFilter
        }.singleOrNull()

        if (existing == null) {
            FamilyPantryItemTable.insert {
                it[FamilyPantryItemTable.familyId] = familyId
                it[FamilyPantryItemTable.ingredientName] = ingredientName
                it[FamilyPantryItemTable.quantity] = quantity
                it[FamilyPantryItemTable.unit] = unit
            }
        } else {
            val existingId = existing[FamilyPantryItemTable.id]
            val newQuantity = existing[FamilyPantryItemTable.quantity] + quantity
            FamilyPantryItemTable.update({ FamilyPantryItemTable.id eq existingId }) {
                it[FamilyPantryItemTable.quantity] = newQuantity
                it[FamilyPantryItemTable.updatedAt] = CurrentTimestamp()
            }
        }
    }

    /**
     * Phase 4.3: Adds purchased shopping items back into Pantry using confirmed quantities.
     */
    suspend fun syncShoppingListToPantry(
        familyId: Int,
        requesterUserId: Int,
        shoppingListId: Int,
        confirmedQuantities: Map<Int, Double>
    ): FamilyShoppingListPantrySyncResult = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)
        getShoppingListRowOrThrow(familyId, shoppingListId)

        val purchasedItems = FamilyShoppingListItemTable.select {
            (FamilyShoppingListItemTable.shoppingListId eq shoppingListId) and
                (FamilyShoppingListItemTable.isPurchased eq true)
        }.toList()

        purchasedItems.forEach { row ->
            val itemId = row[FamilyShoppingListItemTable.id].value
            val quantity = confirmedQuantities[itemId] ?: row[FamilyShoppingListItemTable.requiredQty]
            addPurchasedItemToPantry(
                familyId = familyId,
                ingredientName = row[FamilyShoppingListItemTable.ingredientName],
                quantity = quantity,
                unit = row[FamilyShoppingListItemTable.unit]
            )
        }

        if (purchasedItems.isNotEmpty()) {
            FamilyShoppingListTable.update({ FamilyShoppingListTable.id eq shoppingListId }) {
                it[FamilyShoppingListTable.status] = "completed"
                it[FamilyShoppingListTable.updatedAt] = CurrentTimestamp()
            }
            appendShoppingActivity(
                shoppingListId = shoppingListId,
                itemId = null,
                actorUserId = requesterUserId,
                action = "sync_pantry",
                message = "Đã cập nhật Pantry từ các item đã mua"
            )
        }

        FamilyShoppingListPantrySyncResult(updatedCount = purchasedItems.size)
    }

    private fun getMenuByIdOrThrow(familyId: Int, menuId: Int): ResultRow {
        return FamilyMenuTable.select {
            (FamilyMenuTable.id eq menuId) and (FamilyMenuTable.familyId eq familyId)
        }.singleOrNull() ?: throw AppException.NotFound("Family menu not found")
    }

    private fun insertMenuItemIfMissing(
        menuId: Int,
        dishId: Int?,
        personalDishId: Int?,
        note: String?
    ): Boolean {
        // Skip invalid items that no longer have a source.
        if ((dishId == null && personalDishId == null) || (dishId != null && personalDishId != null)) {
            return false
        }

        if (dishId != null) {
            val dishExists = DishTable.select { DishTable.id eq dishId }.count() > 0
            if (!dishExists) {
                return false
            }
        }

        if (personalDishId != null) {
            val personalExists = PersonalDishTable.select { PersonalDishTable.id eq personalDishId }.count() > 0
            if (!personalExists) {
                return false
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
            return false
        }

        FamilyMenuItemTable.insert {
            it[FamilyMenuItemTable.familyMenuId] = menuId
            it[FamilyMenuItemTable.dishId] = dishId
            it[FamilyMenuItemTable.personalDishId] = personalDishId
            it[FamilyMenuItemTable.note] = note?.trim()?.takeIf { text -> text.isNotBlank() }
        }

        return true
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

        // Remove old weekly menus that were not saved as presets.
        deleteExpiredMenus(familyId, LocalDate.now().with(DayOfWeek.MONDAY))

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

    /**
     * Lists recently added menu items for a family to power picker "Recent".
     */
    suspend fun listRecentMenuItems(
        familyId: Int,
        requesterUserId: Int,
        limit: Int
    ): List<FamilyRecentMenuItem> = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)

        val safeLimit = limit.coerceIn(1, 50)

        (FamilyMenuItemTable innerJoin FamilyMenuTable)
            .select { FamilyMenuTable.familyId eq familyId }
            .orderBy(FamilyMenuItemTable.createdAt to SortOrder.DESC)
            .limit(safeLimit)
            .map { rowToFamilyRecentMenuItem(it) }
    }

    /**
     * Lists the newest vote records for the selected family so Family Home can deep-link to the voted meal.
     */
    suspend fun listRecentVotes(
        familyId: Int,
        requesterUserId: Int,
        limit: Int
    ): List<FamilyRecentVote> = newSuspendedTransaction(Dispatchers.IO) {
        requireFamilyExists(familyId)
        requireFamilyMember(familyId, requesterUserId)

        val safeLimit = limit.coerceIn(1, 50)

        (FamilyMenuVoteTable innerJoin FamilyMenuItemTable innerJoin FamilyMenuTable)
            .select { FamilyMenuTable.familyId eq familyId }
            .orderBy(FamilyMenuVoteTable.createdAt to SortOrder.DESC)
            .limit(safeLimit)
            .map { row ->
                val dishId = row[FamilyMenuItemTable.dishId]?.value
                val personalDishId = row[FamilyMenuItemTable.personalDishId]?.value
                val voterUserId = row[FamilyMenuVoteTable.userId].value

                FamilyRecentVote(
                    familyId = row[FamilyMenuTable.familyId].value,
                    menuId = row[FamilyMenuTable.id].value,
                    menuDate = row[FamilyMenuTable.menuDate].toString(),
                    mealType = row[FamilyMenuTable.mealType],
                    familyMenuItemId = row[FamilyMenuItemTable.id].value,
                    voterUserId = voterUserId,
                    voterName = displayNameForFamilyMember(familyId, voterUserId),
                    dishId = dishId,
                    personalDishId = personalDishId,
                    dishName = menuItemDisplayName(dishId, personalDishId, row[FamilyMenuItemTable.note]),
                    voteType = row[FamilyMenuVoteTable.voteType],
                    createdAt = row[FamilyMenuVoteTable.createdAt].toString()
                )
            }
    }

    /**
     * Deletes menus older than the current week unless they were saved as presets.
     */
    private fun deleteExpiredMenus(familyId: Int, currentWeekStart: LocalDate) {
        FamilyMenuTable.deleteWhere {
            (FamilyMenuTable.familyId eq familyId) and
                (FamilyMenuTable.menuDate less currentWeekStart) and
                (FamilyMenuTable.isSaved eq false)
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
