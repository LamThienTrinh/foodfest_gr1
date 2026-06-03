package com.foodfest.app.features.family.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.components.AppImage
import com.foodfest.app.components.FoodFestTextField
import com.foodfest.app.features.family.presentation.models.FamilyHomeState
import com.foodfest.app.features.family.presentation.models.FamilyInviteUi
import com.foodfest.app.features.family.presentation.models.FamilyMemberSummary
import com.foodfest.app.features.family.presentation.models.FamilyOptionUi
import com.foodfest.app.features.family.presentation.models.FamilyStats
import com.foodfest.app.theme.AppColors
import foodfest.composeapp.generated.resources.Res
import foodfest.composeapp.generated.resources.default_avatar
import org.jetbrains.compose.resources.painterResource

/**
 * Family Home screen shows summary, quick actions, and activity snapshots.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyHomeScreen(
    viewModel: FamilyHomeViewModel = remember { FamilyHomeViewModel() },
    initialFamilyId: Int? = null,
    onBack: () -> Unit,
    onNavigateToMembers: (Int) -> Unit,
    onNavigateToSavedMeals: (Int) -> Unit = {},
    onNavigateToWeeklyMenu: (Int) -> Unit = {},
    onNavigateToPantry: (Int) -> Unit = {},
    onNavigateToNotes: (Int) -> Unit = {},
    onNavigateToVotes: (Int) -> Unit = {}
) {
    val state = viewModel.state

    // Load family data when entering the screen.
    LaunchedEffect(initialFamilyId) {
        viewModel.loadFamilyHome(initialFamilyId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Gia đình",
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Brown
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = AppColors.Brown
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            when {
                state.isLoading -> {
                    FamilyHomeLoadingState()
                }
                state.errorMessage != null -> {
                    FamilyHomeErrorState(
                        message = state.errorMessage,
                        onRetry = { viewModel.loadFamilyHome() }
                    )
                }
                else -> {
                    FamilyHomeContent(
                        state = state,
                        onCreateFamilyNameChange = viewModel::updateCreateFamilyName,
                        onCreateFamily = viewModel::createFamily,
                        onSelectFamily = viewModel::selectFamily,
                        onAcceptInvite = { inviteId ->
                            viewModel.respondToInvite(inviteId, true)
                        },
                        onDeclineInvite = { inviteId ->
                            viewModel.respondToInvite(inviteId, false)
                        },
                        onNavigateToWeeklyMenu = onNavigateToWeeklyMenu,
                        onNavigateToSavedMeals = onNavigateToSavedMeals,
                        onNavigateToMembers = onNavigateToMembers,
                        onNavigateToPantry = onNavigateToPantry,
                        onNavigateToNotes = onNavigateToNotes,
                        onNavigateToVotes = onNavigateToVotes
                    )
                }
            }
        }
    }
}

/**
 * Loading state for Family Home.
 */
@Composable
private fun FamilyHomeLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = AppColors.Orange)
    }
}

/**
 * Error state with retry action.
 */
@Composable
private fun FamilyHomeErrorState(
    message: String?,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message ?: "Có lỗi xảy ra",
            color = AppColors.TextSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange)
        ) {
            Text(text = "Thử lại")
        }
    }
}

/**
 * Main content when data is ready.
 */
@Composable
private fun FamilyHomeContent(
    state: FamilyHomeState,
    onCreateFamilyNameChange: (String) -> Unit,
    onCreateFamily: () -> Unit,
    onSelectFamily: (Int) -> Unit,
    onAcceptInvite: (Int) -> Unit,
    onDeclineInvite: (Int) -> Unit,
    onNavigateToWeeklyMenu: (Int) -> Unit,
    onNavigateToSavedMeals: (Int) -> Unit,
    onNavigateToMembers: (Int) -> Unit,
    onNavigateToPantry: (Int) -> Unit,
    onNavigateToNotes: (Int) -> Unit,
    onNavigateToVotes: (Int) -> Unit
) {
    val uiState = state.uiState

    FamilyCreateSection(
        name = state.createFamilyName,
        isCreating = state.isCreatingFamily,
        errorMessage = state.createFamilyError,
        onNameChange = onCreateFamilyNameChange,
        onCreate = onCreateFamily
    )

    Spacer(modifier = Modifier.height(16.dp))

    FamilyInviteSection(
        invites = state.invites,
        isLoading = state.isInvitesLoading,
        errorMessage = state.invitesError,
        onAccept = onAcceptInvite,
        onDecline = onDeclineInvite
    )

    if (uiState.familyId == null) {
        FamilyEmptyState()
        Spacer(modifier = Modifier.height(24.dp))
        return
    }

    Spacer(modifier = Modifier.height(16.dp))

    FamilySwitcher(
        families = state.families,
        selectedFamilyId = state.selectedFamilyId,
        onSelectFamily = onSelectFamily
    )

    Spacer(modifier = Modifier.height(16.dp))

    FamilyHeaderCard(
        familyName = uiState.familyName,
        members = uiState.members
    )

    Spacer(modifier = Modifier.height(16.dp))

    FamilyStatsRow(stats = uiState.stats)

    Spacer(modifier = Modifier.height(20.dp))

    FamilyQuickActions(
        onNavigateToWeeklyMenu = {
            uiState.familyId?.let(onNavigateToWeeklyMenu)
        },
        onNavigateToSavedMeals = {
            uiState.familyId?.let(onNavigateToSavedMeals)
        },
        onNavigateToMembers = {
            // Guard against missing family id before navigating.
            uiState.familyId?.let(onNavigateToMembers)
        },
        onNavigateToPantry = {
            uiState.familyId?.let(onNavigateToPantry)
        },
        onNavigateToNotes = {
            uiState.familyId?.let(onNavigateToNotes)
        },
        onNavigateToVotes = {
            uiState.familyId?.let(onNavigateToVotes)
        }
    )

    Spacer(modifier = Modifier.height(20.dp))

    FamilyActivitySection(activities = uiState.recentActivities)

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun FamilySwitcher(
    families: List<FamilyOptionUi>,
    selectedFamilyId: Int?,
    onSelectFamily: (Int) -> Unit
) {
    if (families.size <= 1) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Gia đình của bạn",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            families.forEach { family ->
                val selected = family.familyId == selectedFamilyId
                Button(
                    onClick = { onSelectFamily(family.familyId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) AppColors.Orange else Color.White,
                        contentColor = if (selected) Color.White else AppColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "${family.familyName} (${family.memberCount})")
                }
            }
        }
    }
}

/**
 * Section for creating a new family.
 */
@Composable
private fun FamilyCreateSection(
    name: String,
    isCreating: Boolean,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Tạo gia đình",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tạo nhóm gia đình mới để quản lý thực đơn và thành viên.",
                fontSize = 12.sp,
                color = AppColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            FoodFestTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = "Nhập tên gia đình"
            )
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    fontSize = 12.sp,
                    color = AppColors.Error
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCreate,
                enabled = !isCreating,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = if (isCreating) "Đang tạo..." else "Tạo gia đình")
            }
        }
    }
}

/**
 * Section listing pending invites for the current user.
 */
@Composable
private fun FamilyInviteSection(
    invites: List<FamilyInviteUi>,
    isLoading: Boolean,
    errorMessage: String?,
    onAccept: (Int) -> Unit,
    onDecline: (Int) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AppColors.Orange)
        }
        return
    }

    if (!errorMessage.isNullOrBlank()) {
        Text(
            text = errorMessage.orEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            fontSize = 12.sp,
            color = AppColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (invites.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Lời mời vào gia đình",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        invites.forEach { invite ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = invite.familyName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mời bởi ${invite.invitedByName}",
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onDecline(invite.inviteId) }) {
                            Text(text = "Từ chối", color = AppColors.TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onAccept(invite.inviteId) },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "Đồng ý")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Empty state prompt when the user has no family yet.
 */
@Composable
private fun FamilyEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bạn chưa có gia đình",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Hãy tạo gia đình mới hoặc chấp nhận lời mời để bắt đầu.",
            fontSize = 12.sp,
            color = AppColors.TextSecondary
        )
    }
}

/**
 * Header card with family name and member avatar preview.
 */
@Composable
private fun FamilyHeaderCard(
    familyName: String,
    members: List<FamilyMemberSummary>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = familyName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Thành viên đang hoạt động",
                fontSize = 13.sp,
                color = AppColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            FamilyAvatarRow(members = members)
        }
    }
}

/**
 * Shows up to 4 avatars and a "+N" overflow badge.
 */
@Composable
private fun FamilyAvatarRow(members: List<FamilyMemberSummary>) {
    // Limit visible avatars to avoid overflow in small screens.
    val visibleMembers = members.take(4)
    val overflowCount = (members.size - visibleMembers.size).coerceAtLeast(0)

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        visibleMembers.forEachIndexed { index, member ->
            FamilyAvatar(
                avatarUrl = member.avatarUrl,
                contentDescription = member.displayName,
                modifier = Modifier
                    .size(44.dp)
                    .offset(x = (-8 * index).dp)
            )
        }

        if (overflowCount > 0) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .offset(x = (-8 * visibleMembers.size).dp)
                    .clip(CircleShape)
                    .background(AppColors.Orange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$overflowCount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Orange
                )
            }
        }
    }
}

/**
 * Avatar with fallback to local default image.
 */
@Composable
private fun FamilyAvatar(
    avatarUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White)
    ) {
        if (avatarUrl.isNullOrBlank()) {
            Image(
                painter = painterResource(Res.drawable.default_avatar),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AppImage(
                url = avatarUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Horizontal row of stat cards.
 */
@Composable
private fun FamilyStatsRow(stats: FamilyStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FamilyStatCard(
            label = "Thành viên",
            value = stats.memberCount,
            modifier = Modifier.weight(1f)
        )
        FamilyStatCard(
            label = "Menu tuần",
            value = stats.menusThisWeek,
            modifier = Modifier.weight(1f)
        )
        FamilyStatCard(
            label = "Preset",
            value = stats.savedMealsCount,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Small stat card used in Family Home.
 */
@Composable
private fun FamilyStatCard(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = AppColors.TextPrimary
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = AppColors.TextSecondary
            )
        }
    }
}

/**
 * Quick action list with consistent card styling.
 */
@Composable
private fun FamilyQuickActions(
    onNavigateToWeeklyMenu: () -> Unit,
    onNavigateToSavedMeals: () -> Unit,
    onNavigateToMembers: () -> Unit,
    onNavigateToPantry: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToVotes: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Lối tắt nhanh",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )

        FamilyActionCard(
            icon = Icons.Default.CalendarToday,
            title = "Lịch tuần",
            subtitle = "Lập kế hoạch theo ngày",
            onClick = onNavigateToWeeklyMenu
        )
        FamilyActionCard(
            icon = Icons.Default.Bookmark,
            title = "Thực đơn đã lưu",
            subtitle = "Family presets",
            onClick = onNavigateToSavedMeals
        )
        FamilyActionCard(
            icon = Icons.Default.People,
            title = "Quản lý thành viên",
            subtitle = "Mời và phân quyền",
            onClick = onNavigateToMembers
        )
        FamilyActionCard(
            icon = Icons.Default.Restaurant,
            title = "Pantry / Tủ lạnh",
            subtitle = "Nguyên liệu và hạn dùng",
            onClick = onNavigateToPantry
        )
        FamilyActionCard(
            icon = Icons.Default.Chat,
            title = "Family Notes",
            subtitle = "Chat nhanh về menu và shopping",
            onClick = onNavigateToNotes
        )
        FamilyActionCard(
            icon = Icons.Default.HowToVote,
            title = "Vote gần đây",
            subtitle = "Xem kết quả mới nhất",
            onClick = onNavigateToVotes
        )
    }
}

/**
 * Single action card aligned with Profile menu style.
 */
@Composable
private fun FamilyActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(bottom = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Orange.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.Orange,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = AppColors.GrayPlaceholder
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AppColors.GrayPlaceholder
            )
        }
    }
}

/**
 * Recent activity list for quick context.
 */
@Composable
private fun FamilyActivitySection(activities: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Hoạt động gần đây",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (activities.isEmpty()) {
            Text(
                text = "Chưa có hoạt động mới",
                fontSize = 13.sp,
                color = AppColors.TextSecondary
            )
            return
        }

        activities.forEach { activity ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Text(
                    text = activity,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}
