package com.foodfest.app.utils

/**
 * iOS fallback implementation.
 *
 * TODO: implement real image resizing/compression on iOS if needed.
 */
actual suspend fun resizeImage(
    imageBytes: ByteArray,
    maxWidth: Int,
    maxHeight: Int,
    quality: Int
): ByteArray {
    return imageBytes
}
