package com.foodfest.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image

// Shared HttpClient for iOS image loading
private val iosImageClient = HttpClient()

@Composable
actual fun AppImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    if (url.isNullOrEmpty()) {
        ImagePlaceholder(modifier = modifier)
        return
    }

    var imageBitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(url) { mutableStateOf(true) }
    var hasError by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        isLoading = true
        hasError = false
        try {
            val bitmap = loadImageFromUrl(url)
            imageBitmap = bitmap
        } catch (e: Exception) {
            hasError = true
            println("Failed to load image: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.Gray)
                }
            }
            hasError || imageBitmap == null -> {
                ImagePlaceholder(modifier = Modifier.fillMaxSize())
            }
            else -> {
                androidx.compose.foundation.Image(
                    bitmap = imageBitmap!!,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
        }
    }
}

private suspend fun loadImageFromUrl(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        val response: HttpResponse = iosImageClient.get(url)
        val bytes = response.readBytes()
        
        val skiaImage = Image.makeFromEncoded(bytes)
        skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        println("Error loading image from $url: ${e.message}")
        null
    }
}
