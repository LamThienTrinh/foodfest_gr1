package com.foodfest.app.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * Platform-specific image loader component.
 * - Android: Uses Coil AsyncImage
 * - iOS: Uses placeholder (Coil không hỗ trợ iOS)
 */
@Composable
expect fun AppImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
)
