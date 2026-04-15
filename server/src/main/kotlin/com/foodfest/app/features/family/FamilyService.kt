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

    private fun parseDate(dateText: String): LocalDate {
        return runCatching { LocalDate.parse(dateText.trim()) }
            .getOrElse { throw IllegalArgumentException("Date must be ISO format yyyy-MM-dd") }
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

    suspend fun leaveFamily(requesterUserId: Int, familyId: Int): Result<Boolean> = runCatching {
        require(familyId > 0) { "Invalid family id" }
        val left = repository.leaveFamily(familyId, requesterUserId)
        require(left) { "You are not a member of this family" }
        true
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
}
