package com.foodfest.app.features.notification.data

import kotlinx.serialization.Serializable

@Serializable
data class AppNotification(
    val id: Int,
    val userId: Int,
    val type: String,
    val title: String,
    val message: String,
    val relatedEntityType: String? = null,
    val relatedEntityId: Int? = null,
    val actionUrl: String? = null,
    val isRead: Boolean,
    val createdAt: String
)

@Serializable
data class NotificationUnreadCount(
    val unreadCount: Int
)

@Serializable
data class PushDeviceToken(
    val id: Int,
    val userId: Int,
    val platform: String,
    val token: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class RegisterPushDeviceTokenRequest(
    val platform: String,
    val token: String
)
