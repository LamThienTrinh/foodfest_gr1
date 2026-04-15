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
