package com.foodfest.app.features.family.presentation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HowToVote
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.features.family.data.FamilyRecentVote
import com.foodfest.app.theme.AppColors

/**
 * Family vote history screen; each row deep-links back to the exact meal slot that was voted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyRecentVotesScreen(
    familyId: Int,
    viewModel: FamilyRecentVotesViewModel = remember { FamilyRecentVotesViewModel() },
    onBack: () -> Unit,
    onOpenVoteMeal: (menuId: Int, menuDate: String, mealType: String) -> Unit
) {
    val state = viewModel.state

    LaunchedEffect(familyId) {
        viewModel.loadVotes(familyId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Vote gần đây",
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Brown
                        )
                        Text(
                            text = "Bấm một vote để mở đúng bữa",
                            color = AppColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppColors.Background)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Orange)
                    }
                }
                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.errorMessage,
                            color = AppColors.TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.loadVotes(familyId) },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange)
                        ) {
                            Text(text = "Thử lại")
                        }
                    }
                }
                state.votes.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Chưa có vote nào trong gia đình này",
                            color = AppColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.votes, key = { "${it.familyMenuItemId}-${it.voterUserId}" }) { vote ->
                            RecentVoteCard(
                                vote = vote,
                                onClick = {
                                    onOpenVoteMeal(vote.menuId, vote.menuDate, vote.mealType)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentVoteCard(
    vote: FamilyRecentVote,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(AppColors.Orange.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HowToVote,
                    contentDescription = null,
                    tint = AppColors.Orange
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vote.dishName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${vote.voterName} vote ${voteLabel(vote.voteType)}",
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary
                )
                Text(
                    text = "${mealTypeLabel(vote.mealType)} • ${vote.menuDate}",
                    fontSize = 12.sp,
                    color = AppColors.GrayPlaceholder
                )
                Text(
                    text = vote.createdAt,
                    fontSize = 11.sp,
                    color = AppColors.GrayPlaceholder
                )
            }
        }
    }
}

private fun voteLabel(voteType: String): String {
    return when (voteType.lowercase()) {
        "up" -> "thích"
        "down" -> "không thích"
        else -> voteType
    }
}

private fun mealTypeLabel(mealType: String): String {
    return when (mealType.lowercase()) {
        "breakfast" -> "Bữa sáng"
        "lunch" -> "Bữa trưa"
        "dinner" -> "Bữa tối"
        "snack" -> "Bữa phụ"
        else -> mealType
    }
}
