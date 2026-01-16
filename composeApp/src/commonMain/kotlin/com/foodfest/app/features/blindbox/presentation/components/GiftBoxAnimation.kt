package com.foodfest.app.features.blindbox.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.components.AppImage
import com.foodfest.app.features.blindbox.presentation.models.Confetti
import com.foodfest.app.features.blindbox.presentation.models.DishUI
import com.foodfest.app.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GiftBoxAnimation(
    isOpening: Boolean,
    showResult: Boolean,
    winningDish: DishUI?, // Nhận cả object món ăn
    onViewDetailsClick: () -> Unit // Callback khi bấm nút chi tiết
) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    var confettiList by remember { mutableStateOf<List<Confetti>>(emptyList()) }

    // Animation states
    val shakeOffset = remember { Animatable(0f) }
    // Animation cho nắp hộp bật ra
    val lidOffset = remember { Animatable(0f) }
    val lidRotation = remember { Animatable(0f) }

    val boxAlpha = remember { Animatable(1f) }

    // Result presentation animations
    val resultScale = remember { Animatable(0f) }
    val resultAlpha = remember { Animatable(0f) }

    LaunchedEffect(isOpening, showResult) {
        // Giai đoạn 1: Rung lắc và mở hộp
        if (isOpening && !showResult) {
            // Reset trạng thái
            lidOffset.snapTo(0f)
            lidRotation.snapTo(0f)
            boxAlpha.snapTo(1f)

            launch {
                // Rung lắc mạnh hơn một chút
                repeat(6) {
                    shakeOffset.animateTo(12f, animationSpec = tween(40, easing = LinearEasing))
                    shakeOffset.animateTo(-12f, animationSpec = tween(40, easing = LinearEasing))
                }
                shakeOffset.animateTo(0f, tween(40))

                // Hiệu ứng BẬT NẮP!
                launch {
                    lidOffset.animateTo(-250f, tween(400, easing = LinearOutSlowInEasing)) // Bay lên cao
                }
                launch {
                    lidRotation.animateTo(Random.nextFloat() * 30f - 15f, tween(400)) // Xoay nhẹ
                }

                delay(100) // Đợi nắp bật lên một chút rồi mới fade thân hộp
                boxAlpha.animateTo(0f, animationSpec = tween(300))
            }

            // Tạo pháo giấy (Confetti) - Giữ nguyên logic cũ
            delay(300) // Pháo nổ khi nắp bật ra
            confettiList = List(60) {
                val angle = Random.nextFloat() * 360f
                // Tăng tốc độ pháo giấy để trông nổ mạnh hơn
                val speed = Random.nextFloat() * 400f + 300f
                Confetti(
                    x = 0f,
                    y = 0f,
                    color = listOf(Color(0xFFFFC107), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF00BCD4), Color(0xFFFF5722), Color(0xFF4CAF50)).random(),
                    rotation = Random.nextFloat() * 360f,
                    velocity = Offset(
                        cos(angle.toDouble() * PI / 180.0).toFloat() * speed,
                        sin(angle.toDouble() * PI / 180.0).toFloat() * speed
                    ),
                    size = Random.nextFloat() * 12f + 8f
                )
            }
        }

        // Giai đoạn 2: Hiển thị kết quả
        if (showResult && winningDish != null) {
            resultScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
            resultAlpha.animateTo(1f, animationSpec = tween(400))
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. Lớp Pháo giấy (Vẽ dưới cùng)
        if (confettiList.isNotEmpty()) {
            val animatedTime by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(animation = tween(2500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                label = "time"
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                confettiList.forEach { confetti ->
                    val currentX = size.width / 2 + confetti.x + confetti.velocity.x * animatedTime
                    val currentY = size.height / 2 + confetti.y + confetti.velocity.y * animatedTime
                    // Thêm hiệu ứng rơi xuống (gravity nhẹ)
                    val gravityY = currentY + (animatedTime * animatedTime * 200f)

                    if (currentX in -100f..size.width + 100f && gravityY in -100f..size.height + 100f) {
                        rotate(confetti.rotation + animatedTime * 720f, pivot = Offset(currentX, gravityY)) {
                            drawCircle(color = confetti.color, radius = confetti.size, center = Offset(currentX, gravityY))
                        }
                    }
                }
            }
        }

        // 2. Lớp Hộp quà (Body và Lid)
        if (!showResult) {
            Box(
                modifier = Modifier
                    .alpha(boxAlpha.value)
                    .offset(x = shakeOffset.value.dp)
                    .align(Alignment.Center)
            ) {
                // Thân hộp (Ở dưới)
                GiftBoxBody(
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = 60.dp) // Điều chỉnh vị trí cho khớp
                )
                // Nắp hộp (Ở trên, có animation bật lên)
                GiftBoxLid(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = lidOffset.value.dp)
                        .rotate(lidRotation.value)
                )
            }
        }

        // 3. Lớp Kết quả (Ảnh + Tên + Nút)
        if (showResult && winningDish != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(resultScale.value)
                    .alpha(resultAlpha.value)
                    .padding(16.dp)
            ) {
                Text(
                    "✨ Tadaa! Món của bạn là: ✨",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Hình ảnh món ăn
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(3.dp, AppColors.Orange, RoundedCornerShape(16.dp))
                ) {
                    // Sử dụng AppImage (expect/actual) để load ảnh đa nền tảng
                    AppImage(
                        url = winningDish.imageUrl,
                        contentDescription = winningDish.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tên món ăn - có thể click để xem chi tiết
                Text(
                    winningDish.name,
                    color = AppColors.Orange,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable { onViewDetailsClick() }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Nút xem chi tiết
                Button(
                    onClick = onViewDetailsClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Text("Xem chi tiết món này", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
