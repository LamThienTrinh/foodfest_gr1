package com.foodfest.app.features.blindbox.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.components.FoodFestToggleRow
import com.foodfest.app.features.blindbox.presentation.models.DishSourceType
import com.foodfest.app.features.blindbox.presentation.components.DishSelectionSheet
import com.foodfest.app.features.blindbox.presentation.components.GiftBoxAnimation
import com.foodfest.app.features.blindbox.presentation.components.GiftBoxBody
import com.foodfest.app.features.blindbox.presentation.components.GiftBoxLid
import com.foodfest.app.theme.AppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlindBoxScreen(
    onNavigateToDishDetail: (Int) -> Unit = {},
    onNavigateToPersonalDishDetail: (Int) -> Unit = {},
    onNavigateToCreateMyDish: () -> Unit = {},
    personalDishRefreshVersion: Int = 0,
    forcePersonalSourceRefreshVersion: Int = 0
) {
    val viewModel = remember { BlindBoxViewModel() }
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(personalDishRefreshVersion, forcePersonalSourceRefreshVersion) {
        viewModel.loadInitialData(preferPersonalSource = forcePersonalSourceRefreshVersion > 0)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Hôm nay ăn gì?", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Nguồn món",
                            color = AppColors.Brown,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FoodFestToggleRow(
                            label = "Món hệ thống",
                            checked = viewModel.includeSystemDishes,
                            onCheckedChange = {
                                scope.launch { viewModel.toggleIncludeSystemDishes() }
                            }
                        )
                        FoodFestToggleRow(
                            label = "Món của tôi",
                            checked = viewModel.includePersonalDishes,
                            onCheckedChange = {
                                scope.launch { viewModel.toggleIncludePersonalDishes() }
                            }
                        )
                        if (!viewModel.includeSystemDishes && !viewModel.includePersonalDishes) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Chọn ít nhất một nguồn món",
                                color = AppColors.Error,
                                fontSize = 12.sp
                            )
                        }
                        if (viewModel.shouldShowCreatePersonalDishCta) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Chưa có món cá nhân phù hợp để random.",
                                color = AppColors.GrayPlaceholder,
                                fontSize = 13.sp
                            )
                            TextButton(onClick = onNavigateToCreateMyDish) {
                                Text(
                                    text = "Tạo món của tôi",
                                    color = AppColors.Orange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Wheel items selection card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, AppColors.Orange, RoundedCornerShape(12.dp))
                        .background(AppColors.Orange.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Đang chọn ${viewModel.wheelItems.size} món",
                                color = AppColors.Orange,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(onClick = { showBottomSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Thêm/Lọc",
                                    tint = AppColors.Orange
                                )
                            }
                        }

                        if (viewModel.wheelItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = AppColors.Orange.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(viewModel.wheelItems) { dish ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .padding(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(dish.name, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.size(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Red)
                                                    .clickable { viewModel.removeDishFromWheel(dish) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (viewModel.randomError != null) {
                    Text(viewModel.randomError!!, color = Color.Red, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (viewModel.sourceErrorMessage != null) {
                    Text(viewModel.sourceErrorMessage!!, color = AppColors.Error, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Gift Box Animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Chỉ hiển thị animation khi đang mở hoặc đã có kết quả
                    if (viewModel.isOpeningBox || viewModel.showResult) {
                        GiftBoxAnimation(
                            isOpening = viewModel.isOpeningBox,
                            showResult = viewModel.showResult,
                            winningDish = viewModel.winningDish, // Truyền cả object món ăn
                            onViewDetailsClick = {
                                viewModel.winningDish?.let { dish ->
                                    when (dish.sourceType) {
                                        DishSourceType.SYSTEM -> onNavigateToDishDetail(dish.id)
                                        DishSourceType.PERSONAL -> onNavigateToPersonalDishDetail(dish.id)
                                    }
                                }
                            },
                            onShareResultClick = {
                                scope.launch { viewModel.shareWinningDishToFollowers() }
                            },
                            isPostingResult = viewModel.isPostingResult,
                            shareResultEnabled = !viewModel.hasPostedCurrentWinningDish,
                            postResultMessage = viewModel.postResultMessage
                        )
                    } else {
                        // Trạng thái tĩnh ban đầu: Hiển thị hộp quà đóng
                        // Chúng ta dùng lại 2 component con để ghép thành hộp đóng
                        Box(contentAlignment = Alignment.Center) {
                            GiftBoxBody(modifier = Modifier.align(Alignment.BottomCenter).offset(y = 60.dp))
                            GiftBoxLid(modifier = Modifier.align(Alignment.TopCenter))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        scope.launch {
                            viewModel.randomDish()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange),
                    enabled = !viewModel.isOpeningBox &&
                        !viewModel.isLoadingRandom &&
                        (viewModel.includeSystemDishes || viewModel.includePersonalDishes)
                ) {
                    Text(
                        if (viewModel.isOpeningBox || viewModel.isLoadingRandom) "Đang mở..." else "Mở hộp quà (Random)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (showBottomSheet) {
                DishSelectionSheet(
                    viewModel = viewModel,
                    wheelItems = viewModel.wheelItems,
                    onDismiss = { showBottomSheet = false }
                )
            }
        }
    }
}
