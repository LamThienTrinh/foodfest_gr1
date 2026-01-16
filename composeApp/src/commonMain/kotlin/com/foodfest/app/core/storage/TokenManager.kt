package com.foodfest.app.core.storage

import com.russhwolf.settings.Settings

/**
 * TokenManager - Quản lý JWT Token
 * 
 * Lưu token vào thiết bị để tự động đăng nhập:
 * - Android → SharedPreferences
 * - iOS → NSUserDefaults
 */
object TokenManager {
    private val settings: Settings by lazy { Settings() }

    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"

    /**
     * Lưu JWT token
     */
    fun saveToken(token: String) {
        settings.putString(KEY_TOKEN, token)
        println(" Token đã được lưu vào thiết bị")
    }

    /**
     * Lấy JWT token đã lưu
     * @return Token hoặc null nếu chưa đăng nhập
     */
    fun getToken(): String? {
        return settings.getStringOrNull(KEY_TOKEN)
    }

    /**
     * Kiểm tra đã đăng nhập chưa
     */
    fun isLoggedIn(): Boolean {
        return getToken() != null
    }

    /**
     * Lưu thông tin user
     */
    fun saveUserInfo(userId: Int, username: String) {
        settings.putInt(KEY_USER_ID, userId)
        settings.putString(KEY_USERNAME, username)
        println(" Đã lưu thông tin user: ID=$userId, Username=$username")
    }

    /**
     * Lấy User ID đã lưu
     */
    fun getUserId(): Int? {
        val userId = settings.getInt(KEY_USER_ID, -1)
        return if (userId != -1) userId else null
    }

    /**
     * Lấy Username đã lưu
     */
    fun getUsername(): String? {
        return settings.getStringOrNull(KEY_USERNAME)
    }

    /**
     * Xóa toàn bộ dữ liệu đăng nhập (Logout)
     */
    fun clearAll() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_USERNAME)
        println(" Đã xóa toàn bộ dữ liệu đăng nhập")
    }

    /**
     * Chỉ xóa token (khi token hết hạn)
     */
    fun clearToken() {
        settings.remove(KEY_TOKEN)
        println(" Token đã bị xóa")
    }
}
