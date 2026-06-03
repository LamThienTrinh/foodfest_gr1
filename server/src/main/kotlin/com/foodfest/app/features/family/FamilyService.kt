package com.foodfest.app.features.family

import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class CreateFamilyRequest(
    val name: String
)

@Serializable
data class RenameFamilyRequest(
    val name: String
)

@Serializable
data class AddFamilyMemberRequest(
    val userId: Int
)

@Serializable
data class UpdateFamilyMemberNicknameRequest(
    val nickname: String? = null
)

@Serializable
data class CreateFamilyInviteRequest(
    val username: String
)

@Serializable
data class RespondFamilyInviteRequest(
    val accept: Boolean
)

@Serializable
data class CreateFamilyMenuRequest(
    val menuDate: String,
    val mealType: String,
    val status: String? = null
)

@Serializable
data class AddFamilyMenuItemRequest(
    val dishId: Int? = null,
    val personalDishId: Int? = null,
    val note: String? = null
)

@Serializable
data class VoteFamilyMenuItemRequest(
    val voteType: String
)

@Serializable
data class CreateFamilySavedMealRequest(
    val presetName: String
)

@Serializable
data class CreateFamilyPantryItemRequest(
    val ingredientName: String,
    val quantity: Double,
    val unit: String? = null,
    val expiryDate: String? = null
)

@Serializable
data class UpdateFamilyPantryItemRequest(
    val ingredientName: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val expiryDate: String? = null
)

@Serializable
data class GenerateFamilyShoppingListRequest(
    val weekStart: String? = null
)

@Serializable
data class UpdateFamilyShoppingListItemRequest(
    val isPurchased: Boolean? = null,
    val assignedToUserId: Int? = null,
    val usedQty: Double? = null
)

@Serializable
data class ShoppingListPantryQuantity(
    val itemId: Int,
    val quantity: Double? = null
)

@Serializable
data class SyncShoppingListPantryRequest(
    val items: List<ShoppingListPantryQuantity>? = null
)

@Serializable
data class CreateFamilyNoteRequest(
    val message: String
)

class FamilyService(
    private val repository: FamilyRepository = FamilyRepository()
) {

    private fun normalizeFamilyName(name: String): String {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Family name is required" }
        require(trimmed.length <= 120) { "Family name must be <= 120 characters" }
        return trimmed
    }

    private fun normalizeMealType(mealType: String): String {
        val normalized = mealType.trim().lowercase()
        val allowed = setOf("breakfast", "lunch", "dinner", "snack", "other")
        require(normalized in allowed) { "mealType must be one of: breakfast, lunch, dinner, snack, other" }
        return normalized
    }

    private fun normalizeMenuStatus(status: String?): String {
        val normalized = status?.trim()?.lowercase() ?: "draft"
        val allowed = setOf("draft", "voting", "finalized", "archived")
        require(normalized in allowed) { "status must be one of: draft, voting, finalized, archived" }
        return normalized
    }

    private fun normalizePresetName(name: String): String {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Preset name is required" }
        require(trimmed.length <= 120) { "Preset name must be <= 120 characters" }
        return trimmed
    }

    private fun normalizeIngredientName(name: String): String {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Ingredient name is required" }
        require(trimmed.length <= 120) { "Ingredient name must be <= 120 characters" }
        return trimmed
    }

    private fun normalizeMemberNickname(nickname: String?): String? {
        val trimmed = nickname?.trim()?.takeIf { it.isNotBlank() } ?: return null
        require(trimmed.length <= 60) { "Nickname must be <= 60 characters" }
        return trimmed
    }

    private fun normalizeUnit(unit: String?): String? {
        val trimmed = unit?.trim()?.takeIf { it.isNotBlank() } ?: return null
        require(trimmed.length <= 30) { "Unit must be <= 30 characters" }
        return trimmed
    }

    private fun normalizeFamilyNote(message: String): String {
        val trimmed = message.trim()
        require(trimmed.isNotBlank()) { "Message is required" }
        require(trimmed.length <= 500) { "Message must be <= 500 characters" }
        return trimmed
    }

    private fun normalizeQuantity(quantity: Double): Double {
        require(quantity > 0) { "Quantity must be greater than 0" }
        return quantity
    }

    private fun normalizeUsedQuantity(quantity: Double?): Double? {
        if (quantity == null) return null
        require(quantity >= 0) { "Used quantity must be >= 0" }
        return quantity
    }

    private fun parseDate(dateText: String): LocalDate {
        return runCatching { LocalDate.parse(dateText.trim()) }
            .getOrElse { throw IllegalArgumentException("Date must be ISO format yyyy-MM-dd") }
    }

    private fun parseOptionalDate(dateText: String?): LocalDate? {
        if (dateText.isNullOrBlank()) return null
        return parseDate(dateText)
    }

    private fun parseWeekStart(weekStart: String?): LocalDate {
        return if (weekStart.isNullOrBlank()) {
            LocalDate.now().with(DayOfWeek.MONDAY)
        } else {
            parseDate(weekStart)
        }
    }

    suspend fun createFamily(requesterUserId: Int, request: CreateFamilyRequest): Result<FamilyGroup> = runCatching {
        repository.createFamily(
            ownerUserId = requesterUserId,
            name = normalizeFamilyName(request.name)
        )
    }

    suspend fun listMyFamilies(requesterUserId: Int): Result<List<FamilyGroup>> = runCatching {
        repository.listFamiliesByUser(requesterUserId)
    }

    suspend fun renameFamily(
        requesterUserId: Int,
        familyId: Int,
        request: RenameFamilyRequest
    ): Result<FamilyGroup> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        repository.renameFamily(
            familyId = familyId,
            requesterUserId = requesterUserId,
            name = normalizeFamilyName(request.name)
        )
    }

    suspend fun getFamilyMembers(requesterUserId: Int, familyId: Int): Result<List<FamilyMember>> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        repository.getFamilyMembers(familyId, requesterUserId)
    }

    suspend fun addFamilyMember(
        requesterUserId: Int,
        familyId: Int,
        request: AddFamilyMemberRequest
    ): Result<FamilyMember> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(request.userId > 0) { "Invalid target user id" }
        repository.addMember(
            familyId = familyId,
            requesterUserId = requesterUserId,
            targetUserId = request.userId
        )
    }

    suspend fun removeFamilyMember(
        requesterUserId: Int,
        familyId: Int,
        targetUserId: Int
    ): Result<Boolean> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(targetUserId > 0) { "Invalid target user id" }
        val removed = repository.removeMember(
            familyId = familyId,
            requesterUserId = requesterUserId,
            targetUserId = targetUserId
        )
        require(removed) { "Member not found in family" }
        true
    }

    suspend fun updateMyFamilyNickname(
        requesterUserId: Int,
        familyId: Int,
        request: UpdateFamilyMemberNicknameRequest
    ): Result<FamilyMember> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        repository.updateMyNickname(
            familyId = familyId,
            requesterUserId = requesterUserId,
            nickname = normalizeMemberNickname(request.nickname)
        )
    }

    suspend fun leaveFamily(requesterUserId: Int, familyId: Int): Result<Boolean> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        val left = repository.leaveFamily(familyId, requesterUserId)
        require(left) { "You are not a member of this family" }
        true
    }

    suspend fun createFamilyInvite(
        requesterUserId: Int,
        familyId: Int,
        request: CreateFamilyInviteRequest
    ): Result<FamilyInviteSummary> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        repository.createFamilyInvite(
            familyId = familyId,
            requesterUserId = requesterUserId,
            targetUsername = request.username
        )
    }

    suspend fun listMyInvites(requesterUserId: Int): Result<List<FamilyInviteSummary>> = runCatching {
        repository.listPendingInvitesForUser(requesterUserId)
    }

    suspend fun respondToInvite(
        requesterUserId: Int,
        inviteId: Int,
        request: RespondFamilyInviteRequest
    ): Result<FamilyInviteSummary> = runCatching {
        require(inviteId > 0) { "Invalid invite id" }
        repository.respondToInvite(inviteId, requesterUserId, request.accept)
    }

    suspend fun createFamilyMenu(
        requesterUserId: Int,
        familyId: Int,
        request: CreateFamilyMenuRequest
    ): Result<FamilyMenu> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        repository.createMenu(
            familyId = familyId,
            requesterUserId = requesterUserId,
            menuDate = parseDate(request.menuDate),
            mealType = normalizeMealType(request.mealType),
            status = normalizeMenuStatus(request.status)
        )
    }

    suspend fun addFamilyMenuItem(
        requesterUserId: Int,
        familyId: Int,
        menuId: Int,
        request: AddFamilyMenuItemRequest
    ): Result<FamilyMenuItem> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(menuId > 0) { "Invalid menu id" }

        val sourceCount = listOf(request.dishId, request.personalDishId).count { it != null }
        require(sourceCount == 1) { "Provide exactly one of dishId or personalDishId" }

        request.dishId?.let { require(it > 0) { "Invalid dish id" } }
        request.personalDishId?.let { require(it > 0) { "Invalid personal dish id" } }

        repository.addMenuItem(
            familyId = familyId,
            menuId = menuId,
            requesterUserId = requesterUserId,
            dishId = request.dishId,
            personalDishId = request.personalDishId,
            note = request.note
        )
    }

    suspend fun removeFamilyMenuItem(
        requesterUserId: Int,
        familyId: Int,
        menuId: Int,
        itemId: Int
    ): Result<Boolean> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(menuId > 0) { "Invalid menu id" }
        require(itemId > 0) { "Invalid menu item id" }

        val removed = repository.removeMenuItem(
            familyId = familyId,
            menuId = menuId,
            itemId = itemId,
            requesterUserId = requesterUserId
        )
        require(removed) { "Menu item not found" }
        true
    }

    suspend fun getWeeklyMenus(
        requesterUserId: Int,
        familyId: Int,
        weekStart: String?
    ): Result<List<FamilyMenuWithItems>> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        repository.getWeeklyMenus(
            familyId = familyId,
            requesterUserId = requesterUserId,
            weekStart = parseWeekStart(weekStart)
        )
    }

    /**
     * Returns recently added menu items to populate the picker "Recent" tab.
     */
    suspend fun listRecentMenuItems(
        requesterUserId: Int,
        familyId: Int,
        limit: Int?
    ): Result<List<FamilyRecentMenuItem>> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        val safeLimit = limit?.coerceIn(1, 50) ?: 12
        repository.listRecentMenuItems(
            familyId = familyId,
            requesterUserId = requesterUserId,
            limit = safeLimit
        )
    }

    /**
     * Returns recent family vote events for the vote history screen.
     */
    suspend fun listRecentVotes(
        requesterUserId: Int,
        familyId: Int,
        limit: Int?
    ): Result<List<FamilyRecentVote>> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        val safeLimit = limit?.coerceIn(1, 50) ?: 30
        repository.listRecentVotes(
            familyId = familyId,
            requesterUserId = requesterUserId,
            limit = safeLimit
        )
    }

    suspend fun voteFamilyMenuItem(
        requesterUserId: Int,
        familyId: Int,
        menuId: Int,
        itemId: Int,
        request: VoteFamilyMenuItemRequest
    ): Result<FamilyVoteActionResult> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(menuId > 0) { "Invalid menu id" }
        require(itemId > 0) { "Invalid menu item id" }

        val voteType = request.voteType.trim().lowercase()
        require(voteType == "up" || voteType == "down") { "voteType must be up or down" }

        repository.voteMenuItem(
            familyId = familyId,
            menuId = menuId,
            itemId = itemId,
            requesterUserId = requesterUserId,
            voteType = voteType
        )
    }

    suspend fun unvoteFamilyMenuItem(
        requesterUserId: Int,
        familyId: Int,
        menuId: Int,
        itemId: Int
    ): Result<FamilyVoteActionResult> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(menuId > 0) { "Invalid menu id" }
        require(itemId > 0) { "Invalid menu item id" }

        repository.unvoteMenuItem(
            familyId = familyId,
            menuId = menuId,
            itemId = itemId,
            requesterUserId = requesterUserId
        )
    }

    suspend fun getMenuVoteSummary(
        requesterUserId: Int,
        familyId: Int,
        menuId: Int
    ): Result<List<FamilyMenuVoteSummary>> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(menuId > 0) { "Invalid menu id" }

        repository.getMenuVoteSummary(
            familyId = familyId,
            menuId = menuId,
            requesterUserId = requesterUserId
        )
    }

    suspend fun listFamilySavedMeals(
        requesterUserId: Int,
        familyId: Int
    ): Result<List<FamilySavedMealSummary>> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        repository.listSavedMeals(familyId, requesterUserId)
    }

    suspend fun getFamilySavedMealDetail(
        requesterUserId: Int,
        familyId: Int,
        savedMealId: Int
    ): Result<FamilySavedMealDetail> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(savedMealId > 0) { "Invalid saved meal id" }
        repository.getSavedMealDetail(familyId, savedMealId, requesterUserId)
    }

    suspend fun createSavedMealFromMenu(
        requesterUserId: Int,
        familyId: Int,
        menuId: Int,
        request: CreateFamilySavedMealRequest
    ): Result<FamilySavedMealDetail> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(menuId > 0) { "Invalid menu id" }
        repository.createSavedMealFromMenu(
            familyId = familyId,
            menuId = menuId,
            requesterUserId = requesterUserId,
            presetName = normalizePresetName(request.presetName)
        )
    }

    suspend fun applySavedMealToMenu(
        requesterUserId: Int,
        familyId: Int,
        savedMealId: Int,
        menuId: Int
    ): Result<FamilySavedMealApplyResult> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(savedMealId > 0) { "Invalid saved meal id" }
        require(menuId > 0) { "Invalid menu id" }
        repository.applySavedMealToMenu(
            familyId = familyId,
            savedMealId = savedMealId,
            menuId = menuId,
            requesterUserId = requesterUserId
        )
    }

    suspend fun deleteSavedMeal(
        requesterUserId: Int,
        familyId: Int,
        savedMealId: Int
    ): Result<Boolean> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(savedMealId > 0) { "Invalid saved meal id" }
        val deleted = repository.deleteSavedMeal(familyId, savedMealId, requesterUserId)
        require(deleted) { "Saved meal not found" }
        true
    }

    /**
     * Returns pantry items for a family.
     */
    suspend fun listFamilyPantryItems(
        requesterUserId: Int,
        familyId: Int
    ): Result<List<FamilyPantryItem>> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        repository.listPantryItems(familyId, requesterUserId)
    }

    /**
     * Creates a pantry item for the family.
     */
    suspend fun createFamilyPantryItem(
        requesterUserId: Int,
        familyId: Int,
        request: CreateFamilyPantryItemRequest
    ): Result<FamilyPantryItem> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        repository.createPantryItem(
            familyId = familyId,
            requesterUserId = requesterUserId,
            ingredientName = normalizeIngredientName(request.ingredientName),
            quantity = normalizeQuantity(request.quantity),
            unit = normalizeUnit(request.unit),
            expiryDate = parseOptionalDate(request.expiryDate)
        )
    }

    /**
     * Updates a pantry item for the family.
     */
    suspend fun updateFamilyPantryItem(
        requesterUserId: Int,
        familyId: Int,
        itemId: Int,
        request: UpdateFamilyPantryItemRequest
    ): Result<FamilyPantryItem> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(itemId > 0) { "Invalid pantry item id" }

        val hasAnyField = listOf(request.ingredientName, request.quantity, request.unit, request.expiryDate)
            .any { it != null }
        require(hasAnyField) { "No fields to update" }

        repository.updatePantryItem(
            familyId = familyId,
            requesterUserId = requesterUserId,
            itemId = itemId,
            ingredientName = request.ingredientName?.let(::normalizeIngredientName),
            quantity = request.quantity?.let(::normalizeQuantity),
            unit = if (request.unit != null) normalizeUnit(request.unit) else null,
            expiryDate = if (request.expiryDate != null) parseOptionalDate(request.expiryDate) else null,
            setUnit = request.unit != null,
            setExpiryDate = request.expiryDate != null
        )
    }

    /**
     * Deletes a pantry item from the family.
     */
    suspend fun deleteFamilyPantryItem(
        requesterUserId: Int,
        familyId: Int,
        itemId: Int
    ): Result<Boolean> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(itemId > 0) { "Invalid pantry item id" }
        val deleted = repository.deletePantryItem(familyId, requesterUserId, itemId)
        require(deleted) { "Pantry item not found" }
        true
    }

    /**
     * Deletes all expired pantry items for the family.
     */
    suspend fun deleteExpiredFamilyPantryItems(
        requesterUserId: Int,
        familyId: Int
    ): Result<FamilyPantryDeleteResult> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        val deleted = repository.deleteExpiredPantryItems(
            familyId = familyId,
            requesterUserId = requesterUserId,
            today = LocalDate.now()
        )
        FamilyPantryDeleteResult(deletedCount = deleted)
    }

    /**
     * Phase 4.3: Generates a shopping list from the selected weekly menu.
     */
    suspend fun generateFamilyShoppingList(
        requesterUserId: Int,
        familyId: Int,
        request: GenerateFamilyShoppingListRequest
    ): Result<FamilyShoppingListDetail> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        repository.generateShoppingList(
            familyId = familyId,
            requesterUserId = requesterUserId,
            weekStart = parseWeekStart(request.weekStart)
        )
    }

    /**
     * Phase 4.4: Loads shopping checklist detail and recent activity.
     */
    suspend fun getFamilyShoppingListDetail(
        requesterUserId: Int,
        familyId: Int,
        shoppingListId: Int
    ): Result<FamilyShoppingListDetail> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(shoppingListId > 0) { "Invalid shopping list id" }
        repository.getShoppingListDetail(familyId, requesterUserId, shoppingListId)
    }

    /**
     * Phase 4.4: Updates purchased/assigned/used state for one checklist item.
     */
    suspend fun updateFamilyShoppingListItem(
        requesterUserId: Int,
        familyId: Int,
        shoppingListId: Int,
        itemId: Int,
        request: UpdateFamilyShoppingListItemRequest
    ): Result<FamilyShoppingListDetail> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(shoppingListId > 0) { "Invalid shopping list id" }
        require(itemId > 0) { "Invalid shopping list item id" }
        val hasField = request.isPurchased != null || request.assignedToUserId != null || request.usedQty != null
        require(hasField) { "No fields to update" }
        request.assignedToUserId?.let { require(it > 0) { "Invalid assigned user id" } }
        repository.updateShoppingListItem(
            familyId = familyId,
            requesterUserId = requesterUserId,
            shoppingListId = shoppingListId,
            itemId = itemId,
            isPurchased = request.isPurchased,
            assignedToUserId = request.assignedToUserId,
            setAssignedTo = request.assignedToUserId != null,
            usedQty = normalizeUsedQuantity(request.usedQty),
            setUsedQty = request.usedQty != null
        )
    }

    /**
     * Phase 4.3: Marks all shopping list items as purchased.
     */
    suspend fun markAllFamilyShoppingItemsPurchased(
        requesterUserId: Int,
        familyId: Int,
        shoppingListId: Int
    ): Result<FamilyShoppingListDetail> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(shoppingListId > 0) { "Invalid shopping list id" }
        repository.markAllShoppingItemsPurchased(familyId, requesterUserId, shoppingListId)
    }

    /**
     * Phase 4.3: Syncs purchased items into Pantry after the shopping trip.
     */
    suspend fun syncFamilyShoppingListToPantry(
        requesterUserId: Int,
        familyId: Int,
        shoppingListId: Int,
        request: SyncShoppingListPantryRequest
    ): Result<FamilyShoppingListPantrySyncResult> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        require(shoppingListId > 0) { "Invalid shopping list id" }
        val confirmed = request.items.orEmpty().associate { item ->
            require(item.itemId > 0) { "Invalid shopping list item id" }
            item.itemId to item.quantity?.let(::normalizeQuantity)
        }.filterValues { it != null }.mapValues { it.value!! }
        repository.syncShoppingListToPantry(familyId, requesterUserId, shoppingListId, confirmed)
    }

    /**
     * Phase 5.3: Lists family notes for the selected family.
     */
    suspend fun listFamilyNotes(
        requesterUserId: Int,
        familyId: Int,
        limit: Int?
    ): Result<List<FamilyNote>> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        repository.listFamilyNotes(
            familyId = familyId,
            requesterUserId = requesterUserId,
            limit = limit?.coerceIn(1, 100) ?: 50
        )
    }

    /**
     * Phase 5.3: Creates a short family note/chat message.
     */
    suspend fun createFamilyNote(
        requesterUserId: Int,
        familyId: Int,
        request: CreateFamilyNoteRequest
    ): Result<FamilyNote> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        repository.createFamilyNote(
            familyId = familyId,
            requesterUserId = requesterUserId,
            message = normalizeFamilyNote(request.message)
        )
    }
}
