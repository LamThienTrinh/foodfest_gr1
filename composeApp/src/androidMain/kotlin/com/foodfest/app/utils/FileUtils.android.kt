package com.foodfest.app.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

/**
 * Object để lưu trữ reference đến application context
 */
object AndroidContextProvider {
    private var context: Context? = null
    
    fun init(context: Context) {
        this.context = context.applicationContext
    }
    
    fun getContext(): Context {
        return context ?: throw IllegalStateException(
            "AndroidContextProvider chưa được khởi tạo. " +
            "Hãy gọi AndroidContextProvider.init(context) trong MainActivity."
        )
    }
}

/**
 * Android implementation - đọc bytes từ cả file:// và content:// URI
 */
actual suspend fun readBytesFromPath(path: String): ByteArray = withContext(Dispatchers.IO) {
    val context = AndroidContextProvider.getContext()
    
    when {
        // Content URI - cần dùng ContentResolver
        path.startsWith("content://") -> {
            val uri = Uri.parse(path)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: throw IllegalArgumentException("Không thể mở content URI: $path")
        }
        
        // File URI
        path.startsWith("file://") -> {
            val filePath = path.removePrefix("file://")
            File(filePath).readBytes()
        }
        
        // Regular file path
        else -> {
            val file = File(path)
            if (file.exists()) {
                file.readBytes()
            } else {
                // Có thể là URI không có scheme, thử parse
                try {
                    val uri = Uri.parse(path)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.readBytes()
                    } ?: throw IllegalArgumentException("Không thể mở URI: $path")
                } catch (e: Exception) {
                    throw IllegalArgumentException("File không tồn tại và không phải URI hợp lệ: $path", e)
                }
            }
        }
    }
}
