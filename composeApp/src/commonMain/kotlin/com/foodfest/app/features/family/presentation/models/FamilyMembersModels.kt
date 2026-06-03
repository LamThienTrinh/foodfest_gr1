package com.foodfest.app.features.family.presentation.models

/**
 * UI model for the member list.
 */
data class FamilyMemberUi(
    val id: Int,
    val name: String,
    val username: String,
    val fullName: String,
    val nickname: String? = null,
    val roleLabel: String,
    val avatarUrl: String? = null,
    val isOwner: Boolean = false
)

/**
 * Screen state container for member management.
 */
data class FamilyMembersState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val members: List<FamilyMemberUi> = emptyList(),
    val inviteUsername: String = "",
    val isInviting: Boolean = false,
    val inviteError: String? = null,
    val inviteSuccessMessage: String? = null,
    val showNicknameDialog: Boolean = false,
    val nicknameInput: String = "",
    val nicknameEditingMember: FamilyMemberUi? = null,
    val isSavingNickname: Boolean = false,
    val nicknameError: String? = null
)
