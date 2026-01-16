package com.foodfest.app.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy

/**
 * iOS implementation - đọc bytes từ file path
 */
@OptIn(ExperimentalForeignApi::class)
actual suspend fun readBytesFromPath(path: String): ByteArray {
    val url = when {
        path.startsWith("file://") -> NSURL.URLWithString(path)
        else -> NSURL.fileURLWithPath(path)
    } ?: throw IllegalArgumentException("Không thể tạo URL từ path: $path")
    
    val data = NSData.dataWithContentsOfURL(url) 
        ?: throw IllegalArgumentException("Không thể đọc file: $path")
    
    return data.toByteArray()
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    if (size == 0) return byteArrayOf()
    
    val bytes = ByteArray(size)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return bytes
}
