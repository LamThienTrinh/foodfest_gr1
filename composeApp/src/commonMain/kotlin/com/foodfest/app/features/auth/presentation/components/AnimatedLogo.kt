package com.foodfest.app.features.auth.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foodfest.app.theme.AppColors
import foodfest.composeapp.generated.resources.Res
import foodfest.composeapp.generated.resources.foodfest_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun AnimatedLogo(
    showLoginForm: Boolean,
    showLoading: Boolean = false
) {
    val logoOffsetY by animateDpAsState(
        targetValue = if (showLoginForm) 120.dp else 0.dp,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "logoOffset"
    )
    
    val logoSize by animateDpAsState(
        targetValue = if (showLoginForm) 180.dp else 280.dp,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "logoSize"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = logoOffsetY)
    ) {
        Image(
            painter = painterResource(Res.drawable.foodfest_logo),
            contentDescription = "FoodFest Logo",
            modifier = Modifier.size(logoSize)
        )
        
        if (showLoading) {
            Box(
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = AppColors.Orange,
                    strokeWidth = 3.dp
                )
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
