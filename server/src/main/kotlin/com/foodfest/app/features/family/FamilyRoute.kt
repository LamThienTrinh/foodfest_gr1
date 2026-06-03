package com.foodfest.app.features.family

import com.foodfest.app.core.exception.toAppStatus
import com.foodfest.app.core.response.ApiResponse
import com.foodfest.app.plugins.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class FamilyMessageData(
    val message: String
)

fun Route.familyRoutes(familyService: FamilyService) {
    route("/api/families") {
        authenticate("auth-jwt") {
            get {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                familyService.listMyFamilies(principal.userId)
                    .onSuccess { families ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(families))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to list families"))
                    }
            }

            post {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val request = runCatching { call.receive<CreateFamilyRequest>() }.getOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                familyService.createFamily(principal.userId, request)
                    .onSuccess { family ->
                        call.respond(HttpStatusCode.Created, ApiResponse.success(family, "Family created"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to create family"))
                    }
            }

            put("/{familyId}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val request = runCatching { call.receive<RenameFamilyRequest>() }.getOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                familyService.renameFamily(principal.userId, familyId, request)
                    .onSuccess { family ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(family, "Family renamed"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to rename family"))
                    }
            }

            get("/{familyId}/members") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                familyService.getFamilyMembers(principal.userId, familyId)
                    .onSuccess { members ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(members))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get family members"))
                    }
            }

            post("/{familyId}/members") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val request = runCatching { call.receive<AddFamilyMemberRequest>() }.getOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                familyService.addFamilyMember(principal.userId, familyId, request)
                    .onSuccess { member ->
                        call.respond(HttpStatusCode.Created, ApiResponse.success(member, "Member added"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to add member"))
                    }
            }

            delete("/{familyId}/members/{userId}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val targetUserId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid target user id"))

                familyService.removeFamilyMember(principal.userId, familyId, targetUserId)
                    .onSuccess {
                        call.respond(HttpStatusCode.OK, ApiResponse.success(FamilyMessageData("Member removed")))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to remove member"))
                    }
            }

            put("/{familyId}/members/me/nickname") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val request = runCatching { call.receive<UpdateFamilyMemberNicknameRequest>() }.getOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                familyService.updateMyFamilyNickname(principal.userId, familyId, request)
                    .onSuccess { member ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(member, "Nickname updated"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to update nickname"))
                    }
            }

            delete("/{familyId}/leave") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                familyService.leaveFamily(principal.userId, familyId)
                    .onSuccess {
                        call.respond(HttpStatusCode.OK, ApiResponse.success(FamilyMessageData("Left family")))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to leave family"))
                    }
            }

            post("/{familyId}/invites") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val request = runCatching { call.receive<CreateFamilyInviteRequest>() }.getOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                familyService.createFamilyInvite(principal.userId, familyId, request)
                    .onSuccess { invite ->
                        call.respond(HttpStatusCode.Created, ApiResponse.success(invite, "Invite created"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to create invite"))
                    }
            }

            get("/invites") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                familyService.listMyInvites(principal.userId)
                    .onSuccess { invites ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(invites))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to list invites"))
                    }
            }

            post("/invites/{inviteId}/respond") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val inviteId = call.parameters["inviteId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid invite id"))

                val request = runCatching { call.receive<RespondFamilyInviteRequest>() }.getOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                familyService.respondToInvite(principal.userId, inviteId, request)
                    .onSuccess { invite ->
                        val message = if (request.accept) "Invite accepted" else "Invite declined"
                        call.respond(HttpStatusCode.OK, ApiResponse.success(invite, message))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to respond to invite"))
                    }
            }

            get("/{familyId}/saved-meals") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                familyService.listFamilySavedMeals(principal.userId, familyId)
                    .onSuccess { savedMeals ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(savedMeals))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to list saved meals"))
                    }
            }

            post("/{familyId}/saved-meals/from-menu/{menuId}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val menuId = call.parameters["menuId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid menu id"))

                val request = runCatching { call.receive<CreateFamilySavedMealRequest>() }.getOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                familyService.createSavedMealFromMenu(principal.userId, familyId, menuId, request)
                    .onSuccess { savedMeal ->
                        call.respond(HttpStatusCode.Created, ApiResponse.success(savedMeal, "Saved meal created"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to create saved meal"))
                    }
            }

            get("/{familyId}/saved-meals/{savedMealId}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val savedMealId = call.parameters["savedMealId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid saved meal id"))

                familyService.getFamilySavedMealDetail(principal.userId, familyId, savedMealId)
                    .onSuccess { savedMeal ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(savedMeal))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get saved meal"))
                    }
            }

            post("/{familyId}/saved-meals/{savedMealId}/apply/{menuId}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val savedMealId = call.parameters["savedMealId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid saved meal id"))

                val menuId = call.parameters["menuId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid menu id"))

                familyService.applySavedMealToMenu(principal.userId, familyId, savedMealId, menuId)
                    .onSuccess { result ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result, "Saved meal applied"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to apply saved meal"))
                    }
            }

            delete("/{familyId}/saved-meals/{savedMealId}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val savedMealId = call.parameters["savedMealId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid saved meal id"))

                familyService.deleteSavedMeal(principal.userId, familyId, savedMealId)
                    .onSuccess {
                        call.respond(HttpStatusCode.OK, ApiResponse.success(FamilyMessageData("Saved meal deleted")))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to delete saved meal"))
                    }
            }

            get("/{familyId}/pantry") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                familyService.listFamilyPantryItems(principal.userId, familyId)
                    .onSuccess { items ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(items))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to list pantry items"))
                    }
            }

            post("/{familyId}/pantry") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val request = runCatching { call.receive<CreateFamilyPantryItemRequest>() }.getOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                familyService.createFamilyPantryItem(principal.userId, familyId, request)
                    .onSuccess { item ->
                        call.respond(HttpStatusCode.Created, ApiResponse.success(item, "Pantry item created"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to create pantry item"))
                    }
            }

            put("/{familyId}/pantry/{itemId}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val itemId = call.parameters["itemId"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid pantry item id"))

                val request = runCatching { call.receive<UpdateFamilyPantryItemRequest>() }.getOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                familyService.updateFamilyPantryItem(principal.userId, familyId, itemId, request)
                    .onSuccess { item ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(item, "Pantry item updated"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to update pantry item"))
                    }
            }

            delete("/{familyId}/pantry/{itemId}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val itemId = call.parameters["itemId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid pantry item id"))

                familyService.deleteFamilyPantryItem(principal.userId, familyId, itemId)
                    .onSuccess {
                        call.respond(HttpStatusCode.OK, ApiResponse.success(FamilyMessageData("Pantry item deleted")))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to delete pantry item"))
                    }
            }

            delete("/{familyId}/pantry/expired") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                familyService.deleteExpiredFamilyPantryItems(principal.userId, familyId)
                    .onSuccess { result ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result, "Expired pantry items deleted"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to delete expired pantry items"))
                    }
            }

            get("/{familyId}/notes") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                familyService.listFamilyNotes(principal.userId, familyId, limit = null)
                    .onSuccess { notes ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(notes))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to list family notes"))
                    }
            }

            post("/{familyId}/notes") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val request = runCatching { call.receive<CreateFamilyNoteRequest>() }.getOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                familyService.createFamilyNote(principal.userId, familyId, request)
                    .onSuccess { note ->
                        call.respond(HttpStatusCode.Created, ApiResponse.success(note, "Family note created"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to create family note"))
                    }
            }

            post("/{familyId}/shopping-lists/generate") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val request = runCatching { call.receive<GenerateFamilyShoppingListRequest>() }
                    .getOrElse { GenerateFamilyShoppingListRequest(call.request.queryParameters["weekStart"]) }

                familyService.generateFamilyShoppingList(principal.userId, familyId, request)
                    .onSuccess { detail ->
                        call.respond(HttpStatusCode.Created, ApiResponse.success(detail, "Shopping list generated"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to generate shopping list"))
                    }
            }

            get("/{familyId}/shopping-lists/{shoppingListId}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val shoppingListId = call.parameters["shoppingListId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid shopping list id"))

                familyService.getFamilyShoppingListDetail(principal.userId, familyId, shoppingListId)
                    .onSuccess { detail ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(detail))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get shopping list"))
                    }
            }

            put("/{familyId}/shopping-lists/{shoppingListId}/items/{itemId}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val shoppingListId = call.parameters["shoppingListId"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid shopping list id"))

                val itemId = call.parameters["itemId"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid shopping list item id"))

                val request = runCatching { call.receive<UpdateFamilyShoppingListItemRequest>() }.getOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                familyService.updateFamilyShoppingListItem(principal.userId, familyId, shoppingListId, itemId, request)
                    .onSuccess { detail ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(detail, "Shopping item updated"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to update shopping item"))
                    }
            }

            post("/{familyId}/shopping-lists/{shoppingListId}/mark-all-purchased") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val shoppingListId = call.parameters["shoppingListId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid shopping list id"))

                familyService.markAllFamilyShoppingItemsPurchased(principal.userId, familyId, shoppingListId)
                    .onSuccess { detail ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(detail, "All items purchased"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to mark all purchased"))
                    }
            }

            post("/{familyId}/shopping-lists/{shoppingListId}/sync-pantry") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val shoppingListId = call.parameters["shoppingListId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid shopping list id"))

                val request = runCatching { call.receive<SyncShoppingListPantryRequest>() }
                    .getOrElse { SyncShoppingListPantryRequest() }

                familyService.syncFamilyShoppingListToPantry(principal.userId, familyId, shoppingListId, request)
                    .onSuccess { result ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(result, "Pantry updated"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to sync pantry"))
                    }
            }

            post("/{familyId}/menus") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val request = runCatching { call.receive<CreateFamilyMenuRequest>() }.getOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                familyService.createFamilyMenu(principal.userId, familyId, request)
                    .onSuccess { menu ->
                        call.respond(HttpStatusCode.Created, ApiResponse.success(menu, "Menu created"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to create menu"))
                    }
            }

            get("/{familyId}/menus/week") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val weekStart = call.request.queryParameters["weekStart"]

                familyService.getWeeklyMenus(principal.userId, familyId, weekStart)
                    .onSuccess { menus ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(menus))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get weekly menus"))
                    }
            }

            get("/{familyId}/menus/recent") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val limit = call.request.queryParameters["limit"]?.toIntOrNull()

                familyService.listRecentMenuItems(principal.userId, familyId, limit)
                    .onSuccess { items ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(items))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get recent menu items"))
                    }
            }

            get("/{familyId}/votes/recent") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val limit = call.request.queryParameters["limit"]?.toIntOrNull()

                familyService.listRecentVotes(principal.userId, familyId, limit)
                    .onSuccess { votes ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(votes))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get recent votes"))
                    }
            }

            post("/{familyId}/menus/{menuId}/items") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val menuId = call.parameters["menuId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid menu id"))

                val request = runCatching { call.receive<AddFamilyMenuItemRequest>() }.getOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                familyService.addFamilyMenuItem(principal.userId, familyId, menuId, request)
                    .onSuccess { item ->
                        call.respond(HttpStatusCode.Created, ApiResponse.success(item, "Menu item added"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to add menu item"))
                    }
            }

            delete("/{familyId}/menus/{menuId}/items/{itemId}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val menuId = call.parameters["menuId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid menu id"))

                val itemId = call.parameters["itemId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid menu item id"))

                familyService.removeFamilyMenuItem(principal.userId, familyId, menuId, itemId)
                    .onSuccess {
                        call.respond(HttpStatusCode.OK, ApiResponse.success(FamilyMessageData("Menu item removed")))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to remove menu item"))
                    }
            }

            post("/{familyId}/menus/{menuId}/items/{itemId}/vote") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val menuId = call.parameters["menuId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid menu id"))

                val itemId = call.parameters["itemId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid menu item id"))

                val request = runCatching { call.receive<VoteFamilyMenuItemRequest>() }.getOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))

                familyService.voteFamilyMenuItem(principal.userId, familyId, menuId, itemId, request)
                    .onSuccess { voteResult ->
                        val message = if (voteResult.voted) "Vote updated" else "Vote removed"
                        call.respond(HttpStatusCode.OK, ApiResponse.success(voteResult, message))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to vote"))
                    }
            }

            delete("/{familyId}/menus/{menuId}/items/{itemId}/vote") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val menuId = call.parameters["menuId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid menu id"))

                val itemId = call.parameters["itemId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid menu item id"))

                familyService.unvoteFamilyMenuItem(principal.userId, familyId, menuId, itemId)
                    .onSuccess { voteResult ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(voteResult, "Vote removed"))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to unvote"))
                    }
            }

            get("/{familyId}/menus/{menuId}/votes") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Unauthorized"))

                val familyId = call.parameters["familyId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid family id"))

                val menuId = call.parameters["menuId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid menu id"))

                familyService.getMenuVoteSummary(principal.userId, familyId, menuId)
                    .onSuccess { summary ->
                        call.respond(HttpStatusCode.OK, ApiResponse.success(summary))
                    }
                    .onFailure { error ->
                        call.respond(error.toAppStatus(), ApiResponse.error<Unit>(error.message ?: "Failed to get vote summary"))
                    }
            }
        }
    }
}
