package com.foodfest.app.features.family.presentation.models

/**
 * Summary stats for Family Home quick cards.
 */
data class FamilyStats(
    val memberCount: Int = 0,
    val menusThisWeek: Int = 0,
    val savedMealsCount: Int = 0
)

/**
 * Lightweight member info for avatar row.
 */
data class FamilyMemberSummary(
    val id: Int,
    val displayName: String,
    val avatarUrl: String? = null
)

/**
 * UI model for pending family invites.
 */
data class FamilyInviteUi(
    val inviteId: Int,
    val familyId: Int,
    val familyName: String,
    val invitedByName: String
)

/**
 * Family option available to the current user.
 */
data class FamilyOptionUi(
    val familyId: Int,
    val familyName: String,
    val memberCount: Int
)

/**
 * UI-ready data for Family Home.
 */
data class FamilyHomeUiState(
    val familyId: Int? = null,
    val familyName: String = "",
    val members: List<FamilyMemberSummary> = emptyList(),
    val stats: FamilyStats = FamilyStats(),
    val recentActivities: List<String> = emptyList()
)

/**
 * Screen state container for Family Home.
 */
data class FamilyHomeState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val uiState: FamilyHomeUiState = FamilyHomeUiState(),
    val createFamilyName: String = "",
    val isCreatingFamily: Boolean = false,
    val createFamilyError: String? = null,
    val families: List<FamilyOptionUi> = emptyList(),
    val selectedFamilyId: Int? = null,
    val invites: List<FamilyInviteUi> = emptyList(),
    val isInvitesLoading: Boolean = false,
    val invitesError: String? = null
)
