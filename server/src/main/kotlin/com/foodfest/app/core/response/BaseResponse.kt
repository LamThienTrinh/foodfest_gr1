package com.foodfest.app.core.response

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
) {
    companion object {
        fun <T> success(data: T, message: String? = null) = ApiResponse(
            success = true,
            message = message,
            data = data
        )
        
        fun <T> error(message: String) = ApiResponse<T>(
            success = false,
            message = message,
            data = null
        )
    }
}

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val page: Int,
    val limit: Int,
    val total: Int
) {
    val totalPages: Int get() = (total + limit - 1) / limit
    val hasNext: Boolean get() = page < totalPages
    val hasPrev: Boolean get() = page > 1
}
