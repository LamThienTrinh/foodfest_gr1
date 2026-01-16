package com.foodfest.app.utils

/**
 * Resize ảnh xuống kích thước tối đa để giảm dung lượng
 * @param imageBytes Byte array của ảnh gốc
 * @param maxWidth Chiều rộng tối đa (px)
 * @param maxHeight Chiều cao tối đa (px)
 * @param quality Chất lượng nén (0-100)
 * @return Byte array của ảnh đã resize
 */
expect suspend fun resizeImage(
    imageBytes: ByteArray,
    maxWidth: Int = 1024,
    maxHeight: Int = 1024,
    quality: Int = 85
): ByteArray
