package com.foodfest.app.features.family.data

import com.foodfest.app.core.network.NetworkClient
import com.foodfest.app.core.storage.TokenManager
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
private data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

/**
 * Repository for family-related API calls.
 */
class FamilyRepository {
    private val client = NetworkClient.httpClient
    private val baseUrl = NetworkClient.BASE_URL

    /**
     * Builds authorization headers if a token exists.
     */
    private fun getAuthHeaders(): Map<String, String> {
        val token = TokenManager.getToken()
        return if (token != null) {
            mapOf("Authorization" to "Bearer $token")
        } else {
            emptyMap()
        }
    }

    private suspend fun apiErrorMessage(response: HttpResponse, fallback: String): String {
        val serverMessage = runCatching { response.body<ApiResponse<Unit>>().message }.getOrNull()
        return serverMessage?.takeIf { it.isNotBlank() } ?: "$fallback (${response.status})"
    }

    /**
     * Fetches all families for the current user.
     */
    suspend fun getMyFamilies(): Result<List<FamilyGroup>> {
        return try {
            val response = client.get("$baseUrl/api/families") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            val apiResponse = response.body<ApiResponse<List<FamilyGroup>>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to load families"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a family for the current user.
     */
    suspend fun createFamily(name: String): Result<FamilyGroup> {
        return try {
            val response = client.post("$baseUrl/api/families") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(CreateFamilyRequest(name = name))
            }

            val apiResponse = response.body<ApiResponse<FamilyGroup>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to create family"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches members of a family by id.
     */
    suspend fun getFamilyMembers(familyId: Int): Result<List<FamilyMember>> {
        return try {
            val response = client.get("$baseUrl/api/families/$familyId/members") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            val apiResponse = response.body<ApiResponse<List<FamilyMember>>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to load members"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds a member to a family.
     */
    suspend fun addFamilyMember(familyId: Int, userId: Int): Result<FamilyMember> {
        return try {
            val response = client.post("$baseUrl/api/families/$familyId/members") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(AddFamilyMemberRequest(userId = userId))
            }

            val apiResponse = response.body<ApiResponse<FamilyMember>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to add member"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes a member from a family.
     */
    suspend fun removeFamilyMember(familyId: Int, userId: Int): Result<Boolean> {
        return try {
            val response = client.delete("$baseUrl/api/families/$familyId/members/$userId") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            val apiResponse = response.body<ApiResponse<FamilyMessageData>>()
            if (apiResponse.success) {
                Result.success(true)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to remove member"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates the current user's nickname inside a family.
     */
    suspend fun updateMyNickname(familyId: Int, nickname: String?): Result<FamilyMember> {
        return try {
            val response = client.put("$baseUrl/api/families/$familyId/members/me/nickname") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(UpdateFamilyMemberNicknameRequest(nickname = nickname))
            }

            val apiResponse = response.body<ApiResponse<FamilyMember>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to update nickname"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Leaves a family for the current user.
     */
    suspend fun leaveFamily(familyId: Int): Result<Boolean> {
        return try {
            val response = client.delete("$baseUrl/api/families/$familyId/leave") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            val apiResponse = response.body<ApiResponse<FamilyMessageData>>()
            if (apiResponse.success) {
                Result.success(true)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to leave family"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches pantry items for a family.
     */
    suspend fun getPantryItems(familyId: Int): Result<List<FamilyPantryItem>> {
        return try {
            val response = client.get("$baseUrl/api/families/$familyId/pantry") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            val apiResponse = response.body<ApiResponse<List<FamilyPantryItem>>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to load pantry items"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a pantry item for a family.
     */
    suspend fun createPantryItem(
        familyId: Int,
        ingredientName: String,
        quantity: Double,
        unit: String? = null,
        expiryDate: String? = null
    ): Result<FamilyPantryItem> {
        return try {
            val response = client.post("$baseUrl/api/families/$familyId/pantry") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(
                    CreateFamilyPantryItemRequest(
                        ingredientName = ingredientName,
                        quantity = quantity,
                        unit = unit,
                        expiryDate = expiryDate
                    )
                )
            }

            val apiResponse = response.body<ApiResponse<FamilyPantryItem>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to create pantry item"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates a pantry item for a family.
     */
    suspend fun updatePantryItem(
        familyId: Int,
        itemId: Int,
        ingredientName: String,
        quantity: Double,
        unit: String? = null,
        expiryDate: String? = null
    ): Result<FamilyPantryItem> {
        return try {
            val response = client.put("$baseUrl/api/families/$familyId/pantry/$itemId") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(
                    UpdateFamilyPantryItemRequest(
                        ingredientName = ingredientName,
                        quantity = quantity,
                        unit = unit,
                        expiryDate = expiryDate
                    )
                )
            }

            val apiResponse = response.body<ApiResponse<FamilyPantryItem>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to update pantry item"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a pantry item by id.
     */
    suspend fun deletePantryItem(familyId: Int, itemId: Int): Result<Boolean> {
        return try {
            val response = client.delete("$baseUrl/api/families/$familyId/pantry/$itemId") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            val apiResponse = response.body<ApiResponse<FamilyMessageData>>()
            if (apiResponse.success) {
                Result.success(true)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to delete pantry item"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes expired pantry items and returns the deleted count.
     */
    suspend fun deleteExpiredPantryItems(familyId: Int): Result<Int> {
        return try {
            val response = client.delete("$baseUrl/api/families/$familyId/pantry/expired") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            val apiResponse = response.body<ApiResponse<FamilyPantryDeleteResult>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data.deletedCount)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to delete expired pantry items"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Phase 4.3: Generates a shopping list from the weekly menu and current Pantry.
     */
    suspend fun generateShoppingList(
        familyId: Int,
        weekStart: String
    ): Result<FamilyShoppingListDetail> {
        return try {
            val response = client.post("$baseUrl/api/families/$familyId/shopping-lists/generate") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(GenerateFamilyShoppingListRequest(weekStart = weekStart))
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<FamilyShoppingListDetail>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to generate shopping list"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Phase 4.4: Loads checklist detail and activity log.
     */
    suspend fun getShoppingListDetail(
        familyId: Int,
        shoppingListId: Int
    ): Result<FamilyShoppingListDetail> {
        return try {
            val response = client.get("$baseUrl/api/families/$familyId/shopping-lists/$shoppingListId") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<FamilyShoppingListDetail>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to load shopping list"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Phase 4.4: Updates one checklist item and returns refreshed detail.
     */
    suspend fun updateShoppingListItem(
        familyId: Int,
        shoppingListId: Int,
        itemId: Int,
        isPurchased: Boolean? = null,
        assignedToUserId: Int? = null,
        usedQty: Double? = null
    ): Result<FamilyShoppingListDetail> {
        return try {
            val response = client.put("$baseUrl/api/families/$familyId/shopping-lists/$shoppingListId/items/$itemId") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(
                    UpdateFamilyShoppingListItemRequest(
                        isPurchased = isPurchased,
                        assignedToUserId = assignedToUserId,
                        usedQty = usedQty
                    )
                )
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<FamilyShoppingListDetail>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to update shopping item"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Phase 4.3: Marks every shopping list item as purchased.
     */
    suspend fun markAllShoppingItemsPurchased(
        familyId: Int,
        shoppingListId: Int
    ): Result<FamilyShoppingListDetail> {
        return try {
            val response = client.post("$baseUrl/api/families/$familyId/shopping-lists/$shoppingListId/mark-all-purchased") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<FamilyShoppingListDetail>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to mark all purchased"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Phase 4.3: Syncs purchased shopping list items into Pantry.
     */
    suspend fun syncShoppingListToPantry(
        familyId: Int,
        shoppingListId: Int
    ): Result<FamilyShoppingListPantrySyncResult> {
        return try {
            val response = client.post("$baseUrl/api/families/$familyId/shopping-lists/$shoppingListId/sync-pantry") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(SyncShoppingListPantryRequest())
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<FamilyShoppingListPantrySyncResult>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to sync pantry"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Phase 5.3: Loads recent family notes for quick coordination/chat.
     */
    suspend fun getFamilyNotes(familyId: Int): Result<List<FamilyNote>> {
        return try {
            val response = client.get("$baseUrl/api/families/$familyId/notes") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<List<FamilyNote>>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to load family notes"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Phase 5.3: Sends one family note message.
     */
    suspend fun createFamilyNote(familyId: Int, message: String): Result<FamilyNote> {
        return try {
            val response = client.post("$baseUrl/api/families/$familyId/notes") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(CreateFamilyNoteRequest(message = message))
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<FamilyNote>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to create family note"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches weekly menus for a family. If weekStart is null, server uses current week.
     */
    suspend fun getWeeklyMenus(familyId: Int, weekStart: String? = null): Result<List<FamilyMenuWithItems>> {
        return try {
            val response = client.get("$baseUrl/api/families/$familyId/menus/week") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                if (weekStart != null) {
                    parameter("weekStart", weekStart)
                }
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<List<FamilyMenuWithItems>>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to load weekly menus"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches recent menu items for the picker "Recent" tab.
     */
    suspend fun getRecentMenuItems(
        familyId: Int,
        limit: Int = 12
    ): Result<List<FamilyRecentMenuItem>> {
        return try {
            val response = client.get("$baseUrl/api/families/$familyId/menus/recent") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                parameter("limit", limit)
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<List<FamilyRecentMenuItem>>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to load recent menu items"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads recent vote events for the selected family.
     */
    suspend fun getRecentVotes(
        familyId: Int,
        limit: Int = 30
    ): Result<List<FamilyRecentVote>> {
        return try {
            val response = client.get("$baseUrl/api/families/$familyId/votes/recent") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                parameter("limit", limit)
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<List<FamilyRecentVote>>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to load recent votes"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a menu slot for a specific date and meal type.
     */
    suspend fun createFamilyMenu(
        familyId: Int,
        menuDate: String,
        mealType: String,
        status: String? = null
    ): Result<FamilyMenu> {
        return try {
            val response = client.post("$baseUrl/api/families/$familyId/menus") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(CreateFamilyMenuRequest(menuDate = menuDate, mealType = mealType, status = status))
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<FamilyMenu>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to create menu"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds an item into a menu slot.
     */
    suspend fun addFamilyMenuItem(
        familyId: Int,
        menuId: Int,
        dishId: Int? = null,
        personalDishId: Int? = null,
        note: String? = null
    ): Result<FamilyMenuItem> {
        return try {
            val response = client.post("$baseUrl/api/families/$familyId/menus/$menuId/items") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(AddFamilyMenuItemRequest(dishId = dishId, personalDishId = personalDishId, note = note))
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<FamilyMenuItem>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to add menu item"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes an item from a menu slot.
     */
    suspend fun removeFamilyMenuItem(familyId: Int, menuId: Int, itemId: Int): Result<Boolean> {
        return try {
            val response = client.delete("$baseUrl/api/families/$familyId/menus/$menuId/items/$itemId") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<FamilyMessageData>>()
            if (apiResponse.success) {
                Result.success(true)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to remove menu item"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets vote summary for a menu slot.
     */
    suspend fun getMenuVoteSummary(familyId: Int, menuId: Int): Result<List<FamilyMenuVoteSummary>> {
        return try {
            val response = client.get("$baseUrl/api/families/$familyId/menus/$menuId/votes") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<List<FamilyMenuVoteSummary>>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to load vote summary"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Votes or unvotes a menu item.
     */
    suspend fun voteMenuItem(
        familyId: Int,
        menuId: Int,
        itemId: Int,
        voteType: String
    ): Result<FamilyVoteActionResult> {
        return try {
            val response = client.post("$baseUrl/api/families/$familyId/menus/$menuId/items/$itemId/vote") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(VoteFamilyMenuItemRequest(voteType = voteType))
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<FamilyVoteActionResult>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to vote"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches saved meal presets for a family.
     */
    suspend fun getSavedMeals(familyId: Int): Result<List<FamilySavedMealSummary>> {
        return try {
            val response = client.get("$baseUrl/api/families/$familyId/saved-meals") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<List<FamilySavedMealSummary>>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to load saved meals"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches detail for a saved meal preset.
     */
    suspend fun getSavedMealDetail(familyId: Int, savedMealId: Int): Result<FamilySavedMealDetail> {
        return try {
            val response = client.get("$baseUrl/api/families/$familyId/saved-meals/$savedMealId") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<FamilySavedMealDetail>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to load saved meal"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a saved meal preset from a menu.
     */
    suspend fun createSavedMealFromMenu(
        familyId: Int,
        menuId: Int,
        presetName: String
    ): Result<FamilySavedMealDetail> {
        return try {
            val response = client.post("$baseUrl/api/families/$familyId/menus/$menuId/saved-meals") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(CreateFamilySavedMealRequest(presetName = presetName))
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception(apiErrorMessage(response, "Không thể lưu bữa ăn mẫu")))
            }

            val apiResponse = response.body<ApiResponse<FamilySavedMealDetail>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Không thể lưu bữa ăn mẫu"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Applies a saved meal preset to an existing menu slot.
     */
    suspend fun applySavedMealToMenu(
        familyId: Int,
        savedMealId: Int,
        menuId: Int
    ): Result<FamilySavedMealApplyResult> {
        return try {
            val response = client.post("$baseUrl/api/families/$familyId/saved-meals/$savedMealId/apply/$menuId") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<FamilySavedMealApplyResult>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to apply saved meal"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a saved meal preset.
     */
    suspend fun deleteSavedMeal(familyId: Int, savedMealId: Int): Result<Boolean> {
        return try {
            val response = client.delete("$baseUrl/api/families/$familyId/saved-meals/$savedMealId") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<FamilyMessageData>>()
            if (apiResponse.success) {
                Result.success(true)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to delete saved meal"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends an invite to a user by username.
     */
    suspend fun inviteByUsername(familyId: Int, username: String): Result<FamilyInviteSummary> {
        return try {
            val response = client.post("$baseUrl/api/families/$familyId/invites") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(CreateFamilyInviteRequest(username = username))
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<FamilyInviteSummary>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to create invite"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches pending invites for the current user.
     */
    suspend fun getMyInvites(): Result<List<FamilyInviteSummary>> {
        return try {
            val response = client.get("$baseUrl/api/families/invites") {
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<List<FamilyInviteSummary>>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to load invites"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Accepts or declines an invite.
     */
    suspend fun respondToInvite(inviteId: Int, accept: Boolean): Result<FamilyInviteSummary> {
        return try {
            val response = client.post("$baseUrl/api/families/invites/$inviteId/respond") {
                contentType(ContentType.Application.Json)
                getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                setBody(RespondFamilyInviteRequest(accept = accept))
            }

            if (response.status.value !in 200..299) {
                return Result.failure(Exception("Lỗi: ${response.status}"))
            }

            val apiResponse = response.body<ApiResponse<FamilyInviteSummary>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to respond to invite"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
