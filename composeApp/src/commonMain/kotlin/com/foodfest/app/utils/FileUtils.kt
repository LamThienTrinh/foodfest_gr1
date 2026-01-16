package com.foodfest.app.utils

/**
 * Đọc bytes từ file path hoặc URI.
 * Hỗ trợ cả file:// và content:// URI trên Android
 * 
 * @param path Đường dẫn hoặc URI của file
 * @return ByteArray của file
 */
expect suspend fun readBytesFromPath(path: String): ByteArray
