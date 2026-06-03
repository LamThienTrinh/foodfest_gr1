package com.foodfest.app.features.family.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.family.data.FamilyGroup
import com.foodfest.app.features.family.data.FamilyMember
import com.foodfest.app.features.family.data.FamilyRepository
import com.foodfest.app.features.family.presentation.models.FamilyHomeState
import com.foodfest.app.features.family.presentation.models.FamilyHomeUiState
import com.foodfest.app.features.family.presentation.models.FamilyInviteUi
import com.foodfest.app.features.family.presentation.models.FamilyMemberSummary
import com.foodfest.app.features.family.presentation.models.FamilyOptionUi
import com.foodfest.app.features.family.presentation.models.FamilyStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for Family Home screen.
 */
class FamilyHomeViewModel(
    private val repository: FamilyRepository = FamilyRepository()
) {
    var state by mutableStateOf(FamilyHomeState())
        private set

    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Loads all families, invites, and the currently selected family dashboard.
     */
    fun loadFamilyHome(preferredFamilyId: Int? = null) {
        if (state.isLoading) return

        val nextSelectedFamilyId = preferredFamilyId ?: state.selectedFamilyId
        state = state.copy(
            isLoading = true,
            errorMessage = null,
            invitesError = null,
            isInvitesLoading = true,
            selectedFamilyId = nextSelectedFamilyId
        )

        scope.launch {
            repository.getMyInvites().fold(
                onSuccess = { invites ->
                    val mapped = invites.map { invite ->
                        val inviterName = invite.invitedByFullName.trim().ifBlank { invite.invitedByUsername }
                        FamilyInviteUi(
                            inviteId = invite.id,
                            familyId = invite.familyId,
                            familyName = invite.familyName,
                            invitedByName = inviterName
                        )
                    }

                    state = state.copy(
                        invites = mapped,
                        isInvitesLoading = false
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        isInvitesLoading = false,
                        invitesError = error.message ?: "Không tải được lời mời"
                    )
                }
            )

            repository.getMyFamilies().fold(
                onSuccess = { families ->
                    if (families.isEmpty()) {
                        state = state.copy(
                            isLoading = false,
                            families = emptyList(),
                            selectedFamilyId = null,
                            uiState = FamilyHomeUiState()
                        )
                        return@fold
                    }

                    val options = families.map {
                        FamilyOptionUi(
                            familyId = it.id,
                            familyName = it.name,
                            memberCount = it.memberCount
                        )
                    }
                    val selectedFamily = families.firstOrNull { it.id == nextSelectedFamilyId }
                        ?: families.first()
                    state = state.copy(
                        families = options,
                        selectedFamilyId = selectedFamily.id
                    )
                    loadSelectedFamily(selectedFamily)
                },
                onFailure = { error ->
                    state = state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Không tải được gia đình"
                    )
                }
            )
        }
    }

    fun selectFamily(familyId: Int) {
        if (familyId <= 0 || familyId == state.selectedFamilyId || state.isLoading) return
        val selected = state.families.firstOrNull { it.familyId == familyId } ?: return
        state = state.copy(
            selectedFamilyId = familyId,
            isLoading = true,
            errorMessage = null,
            uiState = FamilyHomeUiState(
                familyId = selected.familyId,
                familyName = selected.familyName
            )
        )

        scope.launch {
            loadSelectedFamily(
                FamilyGroup(
                    id = selected.familyId,
                    name = selected.familyName,
                    ownerUserId = 0,
                    createdAt = "",
                    memberCount = selected.memberCount
                )
            )
        }
    }

    /**
     * Updates local input for creating a family.
     */
    fun updateCreateFamilyName(value: String) {
        state = state.copy(createFamilyName = value, createFamilyError = null)
    }

    /**
     * Creates a family and refreshes data.
     */
    fun createFamily() {
        if (state.isCreatingFamily) return

        val name = state.createFamilyName.trim()
        if (name.isBlank()) {
            state = state.copy(createFamilyError = "Tên gia đình không được để trống")
            return
        }

        state = state.copy(isCreatingFamily = true, createFamilyError = null)

        scope.launch {
            repository.createFamily(name).fold(
                onSuccess = {
                    state = state.copy(
                        isCreatingFamily = false,
                        createFamilyName = "",
                        selectedFamilyId = it.id
                    )
                    loadFamilyHome()
                },
                onFailure = { error ->
                    state = state.copy(
                        isCreatingFamily = false,
                        createFamilyError = error.message ?: "Không thể tạo gia đình"
                    )
                }
            )
        }
    }

    /**
     * Accepts or declines an invite and refreshes state.
     */
    fun respondToInvite(inviteId: Int, accept: Boolean) {
        if (inviteId <= 0 || state.isInvitesLoading) return

        state = state.copy(isInvitesLoading = true, invitesError = null)

        scope.launch {
            repository.respondToInvite(inviteId, accept).fold(
                onSuccess = {
                    if (accept) {
                        state = state.copy(selectedFamilyId = null)
                        loadFamilyHome()
                    } else {
                        state = state.copy(
                            isInvitesLoading = false,
                            invites = state.invites.filterNot { it.inviteId == inviteId }
                        )
                    }
                },
                onFailure = { error ->
                    state = state.copy(
                        isInvitesLoading = false,
                        invitesError = error.message ?: "Không thể phản hồi lời mời"
                    )
                }
            )
        }
    }

    private suspend fun loadSelectedFamily(selectedFamily: FamilyGroup) {
        repository.getFamilyMembers(selectedFamily.id).fold(
            onSuccess = { members ->
                val summaries = members.map(::mapMemberSummary)
                val savedMealsCount = repository.getSavedMeals(selectedFamily.id)
                    .getOrNull()
                    ?.size
                    ?: 0

                repository.getWeeklyMenus(selectedFamily.id).fold(
                    onSuccess = { menus ->
                        state = state.copy(
                            isLoading = false,
                            uiState = buildUiState(selectedFamily, members, summaries, menus.size, savedMealsCount)
                        )
                    },
                    onFailure = {
                        state = state.copy(
                            isLoading = false,
                            uiState = buildUiState(selectedFamily, members, summaries, 0, savedMealsCount)
                        )
                    }
                )
            },
            onFailure = { error ->
                state = state.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Không tải được thành viên"
                )
            }
        )
    }

    private fun buildUiState(
        family: FamilyGroup,
        members: List<FamilyMember>,
        summaries: List<FamilyMemberSummary>,
        menusThisWeek: Int,
        savedMealsCount: Int
    ): FamilyHomeUiState {
        return FamilyHomeUiState(
            familyId = family.id,
            familyName = family.name,
            members = summaries,
            stats = FamilyStats(
                memberCount = if (members.isNotEmpty()) members.size else family.memberCount,
                menusThisWeek = menusThisWeek,
                savedMealsCount = savedMealsCount
            ),
            recentActivities = emptyList()
        )
    }

    private fun mapMemberSummary(member: FamilyMember): FamilyMemberSummary {
        val displayName = member.nickname?.trim()?.takeIf { it.isNotBlank() }
            ?: member.fullName.trim().ifBlank { member.username }
        return FamilyMemberSummary(
            id = member.userId,
            displayName = displayName,
            avatarUrl = member.avatarUrl
        )
    }
}
