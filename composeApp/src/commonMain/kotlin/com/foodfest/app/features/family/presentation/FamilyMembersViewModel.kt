package com.foodfest.app.features.family.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.family.data.FamilyMember
import com.foodfest.app.features.family.data.FamilyRepository
import com.foodfest.app.features.family.presentation.models.FamilyMemberUi
import com.foodfest.app.features.family.presentation.models.FamilyMembersState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for Family Members management screen.
 */
class FamilyMembersViewModel(
    private val repository: FamilyRepository = FamilyRepository()
) {
    var state by mutableStateOf(FamilyMembersState())
        private set

    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Loads members by familyId and maps them to UI models.
     */
    fun loadMembers(familyId: Int) {
        if (familyId <= 0) {
            state = state.copy(errorMessage = "FamilyId không hợp lệ")
            return
        }

        if (state.isLoading) return

        state = state.copy(isLoading = true, errorMessage = null)

        scope.launch {
            repository.getFamilyMembers(familyId).fold(
                onSuccess = { members ->
                    val mapped = members.map(::mapMember)

                    state = state.copy(
                        isLoading = false,
                        members = mapped,
                        errorMessage = null
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
    }

    /**
     * Updates the username input for inviting a member.
     */
    fun updateInviteUsername(value: String) {
        state = state.copy(
            inviteUsername = value,
            inviteError = null,
            inviteSuccessMessage = null
        )
    }

    /**
     * Sends an invite by username.
     */
    fun inviteMemberByUsername(familyId: Int) {
        if (familyId <= 0) {
            state = state.copy(inviteError = "FamilyId không hợp lệ")
            return
        }

        if (state.isInviting) return

        val username = state.inviteUsername.trim()
        if (username.isBlank()) {
            state = state.copy(inviteError = "Vui lòng nhập username")
            return
        }

        state = state.copy(isInviting = true, inviteError = null, inviteSuccessMessage = null)

        scope.launch {
            repository.inviteByUsername(familyId, username).fold(
                onSuccess = {
                    state = state.copy(
                        isInviting = false,
                        inviteUsername = "",
                        inviteSuccessMessage = "Đã gửi lời mời"
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        isInviting = false,
                        inviteError = error.message ?: "Không thể gửi lời mời"
                    )
                }
            )
        }
    }

    /**
     * Removes a member from the family and updates local state.
     */
    fun removeMember(familyId: Int, userId: Int) {
        if (familyId <= 0 || userId <= 0) return

        scope.launch {
            repository.removeFamilyMember(familyId, userId).fold(
                onSuccess = {
                    state = state.copy(
                        members = state.members.filterNot { it.id == userId }
                    )
                },
                onFailure = { error ->
                    state = state.copy(errorMessage = error.message ?: "Không thể xóa thành viên")
                }
            )
        }
    }

    /**
     * Opens the nickname dialog for the current user.
     */
    fun showNicknameDialog(member: FamilyMemberUi) {
        state = state.copy(
            showNicknameDialog = true,
            nicknameEditingMember = member,
            nicknameInput = member.nickname.orEmpty(),
            nicknameError = null
        )
    }

    /**
     * Closes the nickname dialog and clears local form state.
     */
    fun hideNicknameDialog() {
        state = state.copy(
            showNicknameDialog = false,
            nicknameEditingMember = null,
            nicknameInput = "",
            nicknameError = null,
            isSavingNickname = false
        )
    }

    fun updateNicknameInput(value: String) {
        state = state.copy(nicknameInput = value, nicknameError = null)
    }

    /**
     * Saves or clears the current user's nickname in this family.
     */
    fun saveMyNickname(familyId: Int, clear: Boolean = false) {
        if (familyId <= 0 || state.isSavingNickname) return

        val nickname = if (clear) null else state.nicknameInput.trim().ifBlank { null }
        if (nickname != null && nickname.length > 60) {
            state = state.copy(nicknameError = "Biệt danh tối đa 60 ký tự")
            return
        }

        state = state.copy(isSavingNickname = true, nicknameError = null)

        scope.launch {
            repository.updateMyNickname(familyId, nickname).fold(
                onSuccess = { member ->
                    val updated = mapMember(member)
                    state = state.copy(
                        isSavingNickname = false,
                        showNicknameDialog = false,
                        nicknameEditingMember = null,
                        nicknameInput = "",
                        members = state.members.map { if (it.id == updated.id) updated else it }
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        isSavingNickname = false,
                        nicknameError = error.message ?: "Không thể cập nhật biệt danh"
                    )
                }
            )
        }
    }

    private fun mapMember(member: FamilyMember): FamilyMemberUi {
        val displayName = member.nickname?.trim()?.takeIf { it.isNotBlank() }
            ?: member.fullName.trim().ifBlank { member.username }
        val isOwner = member.role.trim().equals("owner", ignoreCase = true)
        return FamilyMemberUi(
            id = member.userId,
            name = displayName,
            username = member.username,
            fullName = member.fullName,
            nickname = member.nickname,
            roleLabel = if (isOwner) "Owner" else "Member",
            avatarUrl = member.avatarUrl,
            isOwner = isOwner
        )
    }
}
