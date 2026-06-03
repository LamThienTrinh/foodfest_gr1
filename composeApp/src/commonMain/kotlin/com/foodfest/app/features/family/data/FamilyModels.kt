package com.foodfest.app.features.family.data

import kotlinx.serialization.Serializable

/**
 * Family group summary returned by /api/families.
 */
@Serializable
data class FamilyGroup(
    val id: Int,
    val name: String,
    val ownerUserId: Int,
    val createdAt: String,
    val memberCount: Int
)

/**
 * Member item returned by /api/families/{familyId}/members.
 */
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

/**
 * Request body to add a member into a family.
 */
@Serializable
data class AddFamilyMemberRequest(
    val userId: Int
)

/**
 * Request body for updating the current user's family nickname.
 */
@Serializable
data class UpdateFamilyMemberNicknameRequest(
    val nickname: String? = null
)

/**
 * Menu item returned by family menu endpoints.
 */
@Serializable
data class FamilyMenuItem(
    val id: Int,
    val familyMenuId: Int,
    val dishId: Int? = null,
    val personalDishId: Int? = null,
    val note: String? = null,
    val createdAt: String
)

/**
 * Recent menu item summary for picker "Recent" tab.
 */
@Serializable
data class FamilyRecentMenuItem(
    val dishId: Int? = null,
    val personalDishId: Int? = null,
    val createdAt: String
)

/**
 * Menu header returned by family menu endpoints.
 */
@Serializable
data class FamilyMenu(
    val id: Int,
    val familyId: Int,
    val menuDate: String,
    val mealType: String,
    val status: String,
    val createdAt: String
)

/**
 * Menu payload with items for weekly menu view.
 */
@Serializable
data class FamilyMenuWithItems(
    val menu: FamilyMenu,
    val items: List<FamilyMenuItem>
)

/**
 * Request body to create a menu slot.
 */
@Serializable
data class CreateFamilyMenuRequest(
    val menuDate: String,
    val mealType: String,
    val status: String? = null
)

/**
 * Request body to add an item into a menu.
 */
@Serializable
data class AddFamilyMenuItemRequest(
    val dishId: Int? = null,
    val personalDishId: Int? = null,
    val note: String? = null
)

/**
 * Request body to vote on a menu item.
 */
@Serializable
data class VoteFamilyMenuItemRequest(
    val voteType: String
)

/**
 * Vote summary returned by menu vote endpoint.
 */
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

/**
 * Recent vote event used by Family Home vote history.
 */
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

/**
 * Vote action result after voting.
 */
@Serializable
data class FamilyVoteActionResult(
    val voted: Boolean,
    val voteType: String? = null,
    val upVotes: Int,
    val downVotes: Int
)

/**
 * Request body to create a family.
 */
@Serializable
data class CreateFamilyRequest(
    val name: String
)

/**
 * Request body to invite a user by username.
 */
@Serializable
data class CreateFamilyInviteRequest(
    val username: String
)

/**
 * Request body to accept or decline an invite.
 */
@Serializable
data class RespondFamilyInviteRequest(
    val accept: Boolean
)

/**
 * Request body to create a family saved meal from a menu.
 */
@Serializable
data class CreateFamilySavedMealRequest(
    val presetName: String
)

/**
 * Saved meal summary returned by /saved-meals list.
 */
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

/**
 * Saved meal item payload.
 */
@Serializable
data class FamilySavedMealItem(
    val id: Int,
    val savedMealId: Int,
    val dishId: Int? = null,
    val personalDishId: Int? = null,
    val note: String? = null,
    val createdAt: String
)

/**
 * Saved meal details with items.
 */
@Serializable
data class FamilySavedMealDetail(
    val savedMeal: FamilySavedMealSummary,
    val items: List<FamilySavedMealItem>
)

/**
 * Result for applying a saved meal into a menu.
 */
@Serializable
data class FamilySavedMealApplyResult(
    val addedCount: Int,
    val skippedCount: Int
)

/**
 * Pending invite summary for the current user.
 */
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

/**
 * Generic message response payload from family endpoints.
 */
@Serializable
data class FamilyMessageData(
    val message: String
)

/**
 * Pantry item payload for family pantry feature.
 */
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

/**
 * Request body to create a pantry item.
 */
@Serializable
data class CreateFamilyPantryItemRequest(
    val ingredientName: String,
    val quantity: Double,
    val unit: String? = null,
    val expiryDate: String? = null
)

/**
 * Request body to update a pantry item.
 */
@Serializable
data class UpdateFamilyPantryItemRequest(
    val ingredientName: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val expiryDate: String? = null
)

/**
 * Response payload for deleting expired pantry items.
 */
@Serializable
data class FamilyPantryDeleteResult(
    val deletedCount: Int
)

/**
 * Request body to generate a shopping list from a weekly menu.
 */
@Serializable
data class GenerateFamilyShoppingListRequest(
    val weekStart: String? = null
)

/**
 * Shopping list header generated from a weekly menu.
 */
@Serializable
data class FamilyShoppingList(
    val id: Int,
    val familyId: Int,
    val menuWeek: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Checklist item for shopping/cooking prep.
 */
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

/**
 * Shopping checklist activity entry for realtime sync prep.
 */
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

/**
 * Shopping list detail payload with grouped checklist source data.
 */
@Serializable
data class FamilyShoppingListDetail(
    val shoppingList: FamilyShoppingList,
    val items: List<FamilyShoppingListItem>,
    val activityLog: List<FamilyShoppingListActivity>
)

/**
 * Request body to update one checklist item.
 */
@Serializable
data class UpdateFamilyShoppingListItemRequest(
    val isPurchased: Boolean? = null,
    val assignedToUserId: Int? = null,
    val usedQty: Double? = null
)

/**
 * Optional confirmed quantity when syncing purchased items into Pantry.
 */
@Serializable
data class ShoppingListPantryQuantity(
    val itemId: Int,
    val quantity: Double? = null
)

/**
 * Request body for shopping list to Pantry sync.
 */
@Serializable
data class SyncShoppingListPantryRequest(
    val items: List<ShoppingListPantryQuantity>? = null
)

/**
 * Result returned after Pantry sync.
 */
@Serializable
data class FamilyShoppingListPantrySyncResult(
    val updatedCount: Int
)

/**
 * Family note/chat message for Phase 5 quick coordination.
 */
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

/**
 * Request body to create a family note.
 */
@Serializable
data class CreateFamilyNoteRequest(
    val message: String
)
