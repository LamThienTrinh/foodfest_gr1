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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.components.AppImage
import com.foodfest.app.components.FoodFestTextField
import com.foodfest.app.features.family.presentation.models.FamilyMemberUi
import com.foodfest.app.features.family.presentation.models.FamilyMembersState
import com.foodfest.app.theme.AppColors
import foodfest.composeapp.generated.resources.Res
import foodfest.composeapp.generated.resources.default_avatar
import org.jetbrains.compose.resources.painterResource

/**
 * Family member management screen with invite and role display.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMembersScreen(
    familyId: Int,
    currentUserId: Int? = null,
    viewModel: FamilyMembersViewModel = remember { FamilyMembersViewModel() },
    onBack: () -> Unit,
    onTransferOwner: (Int) -> Unit = {}
) {
    val state = viewModel.state

    // Load members when familyId changes.
    LaunchedEffect(familyId) {
        viewModel.loadMembers(familyId)
    }

    // Keep owner on top for better scanning.
    val sortedMembers = remember(state.members) {
        state.members.sortedWith(compareByDescending<FamilyMemberUi> { it.isOwner }.thenBy { it.name })
    }

    if (state.showNicknameDialog) {
        NicknameDialog(
            state = state,
            onNicknameChange = viewModel::updateNicknameInput,
            onDismiss = viewModel::hideNicknameDialog,
            onSave = { viewModel.saveMyNickname(familyId) },
            onClear = { viewModel.saveMyNickname(familyId, clear = true) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Thành viên",
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
        ) {
            MemberHeader(
                state = state,
                onUsernameChange = viewModel::updateInviteUsername,
                onInviteMember = { viewModel.inviteMemberByUsername(familyId) }
            )

            when {
                state.isLoading -> {
                    MemberLoadingState()
                }
                state.errorMessage != null -> {
                    MemberErrorState(
                        message = state.errorMessage,
                        onRetry = { viewModel.loadMembers(familyId) }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppColors.Background),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedMembers, key = { it.id }) { member ->
                            FamilyMemberCard(
                                member = member,
                                isCurrentUser = member.id == currentUserId,
                                onRemoveMember = { memberId ->
                                    viewModel.removeMember(familyId, memberId)
                                },
                                onEditNickname = {
                                    viewModel.showNicknameDialog(member)
                                },
                                onTransferOwner = onTransferOwner
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Loading indicator for member list.
 */
@Composable
private fun MemberLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = AppColors.Orange)
    }
}

/**
 * Error state with retry for member list.
 */
@Composable
private fun MemberErrorState(
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
            fontSize = 14.sp,
            color = AppColors.TextSecondary
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
 * Header section with invite-by-username form.
 */
@Composable
private fun MemberHeader(
    state: FamilyMembersState,
    onUsernameChange: (String) -> Unit,
    onInviteMember: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Quản lý thành viên",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        Text(
            text = "Mời thành viên bằng username",
            fontSize = 12.sp,
            color = AppColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        FoodFestTextField(
            value = state.inviteUsername,
            onValueChange = onUsernameChange,
            placeholder = "Nhập username"
        )
        if (!state.inviteError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = state.inviteError.orEmpty(),
                fontSize = 12.sp,
                color = AppColors.Error
            )
        }
        if (!state.inviteSuccessMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = state.inviteSuccessMessage.orEmpty(),
                fontSize = 12.sp,
                color = AppColors.TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onInviteMember,
                enabled = !state.isInviting,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(text = if (state.isInviting) "Đang mời..." else "Mời")
            }
        }
    }
}

/**
 * Member card row with role chip and actions.
 */
@Composable
private fun FamilyMemberCard(
    member: FamilyMemberUi,
    isCurrentUser: Boolean,
    onRemoveMember: (Int) -> Unit,
    onEditNickname: () -> Unit,
    onTransferOwner: (Int) -> Unit
) {
    // Prevent accidental owner transfer for the current owner.
    val transferEnabled = !member.isOwner

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatar(
                avatarUrl = member.avatarUrl,
                contentDescription = member.name
            )

            Spacer(modifier = Modifier.size(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = member.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )

                if (!member.nickname.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = member.fullName.trim().ifBlank { "@${member.username}" },
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                AssistChip(
                    onClick = {
                        if (transferEnabled) {
                            onTransferOwner(member.id)
                        }
                    },
                    label = {
                        Text(text = member.roleLabel)
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (member.isOwner) AppColors.Orange.copy(alpha = 0.15f) else AppColors.Background
                    ),
                    leadingIcon = if (member.isOwner) {
                        {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AppColors.Orange
                            )
                        }
                    } else {
                        null
                    }
                )
            }

            if (member.isOwner) {
                Column(horizontalAlignment = Alignment.End) {
                    if (isCurrentUser) {
                        NicknameActionButton(onClick = onEditNickname)
                    }
                    Text(
                        text = "Chủ nhóm",
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    if (isCurrentUser) {
                        NicknameActionButton(onClick = onEditNickname)
                    }
                    TextButton(
                        onClick = { onRemoveMember(member.id) }
                    ) {
                        Text(
                            text = "Xóa",
                            fontSize = 12.sp,
                            color = AppColors.Orange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NicknameActionButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = AppColors.Orange
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = "Đổi biệt danh",
            fontSize = 12.sp,
            color = AppColors.Orange
        )
    }
}

@Composable
private fun NicknameDialog(
    state: FamilyMembersState,
    onNicknameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Đổi biệt danh")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FoodFestTextField(
                    value = state.nicknameInput,
                    onValueChange = onNicknameChange,
                    placeholder = "Biệt danh trong gia đình"
                )
                if (!state.nicknameError.isNullOrBlank()) {
                    Text(
                        text = state.nicknameError.orEmpty(),
                        fontSize = 12.sp,
                        color = AppColors.Error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !state.isSavingNickname,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange)
            ) {
                Text(text = if (state.isSavingNickname) "Đang lưu..." else "Lưu")
            }
        },
        dismissButton = {
            Row {
                if (!state.nicknameEditingMember?.nickname.isNullOrBlank()) {
                    TextButton(
                        onClick = onClear,
                        enabled = !state.isSavingNickname
                    ) {
                        Text(text = "Xóa biệt danh", color = AppColors.Error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text = "Hủy")
                }
            }
        }
    )
}

/**
 * Member avatar with fallback to default image.
 */
@Composable
private fun MemberAvatar(
    avatarUrl: String?,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(48.dp)
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
