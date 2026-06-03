package com.foodfest.app.features.family.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.foodfest.app.components.FoodFestTextField
import com.foodfest.app.features.family.data.FamilyNote
import com.foodfest.app.features.family.presentation.models.FamilyNotesState
import com.foodfest.app.theme.AppColors
import kotlinx.coroutines.delay

/**
 * Phase 5.3 Family Notes screen with polling-based sync prep.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyNotesScreen(
    familyId: Int,
    viewModel: FamilyNotesViewModel = remember { FamilyNotesViewModel() },
    onBack: () -> Unit
) {
    val state = viewModel.state

    LaunchedEffect(familyId) {
        viewModel.loadNotes(familyId)
    }

    // Phase 5 MVP uses polling; WebSocket can replace this loop without changing screen state.
    LaunchedEffect(familyId) {
        while (familyId > 0) {
            delay(5000)
            viewModel.loadNotes(familyId, quiet = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Family Notes",
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Brown
                        )
                        Text(
                            text = "Polling sync mỗi 5 giây",
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            NotesComposer(
                state = state,
                onInputChange = viewModel::updateInput,
                onSend = { viewModel.sendNote(familyId) }
            )
            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Orange)
                    }
                }
                state.notes.isEmpty() -> {
                    Text(
                        text = "Chưa có ghi chú. Dùng khu vực này để bàn nhanh về menu, shopping hoặc nấu nướng.",
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.notes, key = { it.id }) { note ->
                            NoteCard(note = note)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesComposer(
    state: FamilyNotesState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FoodFestTextField(
                value = state.input,
                onValueChange = onInputChange,
                placeholder = "VD: Nay ăn cá kho nhé?"
            )
            if (!state.errorMessage.isNullOrBlank()) {
                Text(text = state.errorMessage.orEmpty(), color = AppColors.Error, fontSize = 12.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onSend,
                    enabled = !state.isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = if (state.isSending) "Đang gửi..." else "Gửi")
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: FamilyNote) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = note.authorName,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = note.message, color = AppColors.TextPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = note.createdAt, color = AppColors.TextSecondary, fontSize = 11.sp)
        }
    }
}
