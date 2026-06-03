package com.foodfest.app.features.notification.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.notification.data.AppNotification
import com.foodfest.app.features.notification.data.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class NotificationState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val isMarkingAllRead: Boolean = false
)

class NotificationViewModel(
    private val repository: NotificationRepository = NotificationRepository()
) {
    var state by mutableStateOf(NotificationState())
        private set

    private val scope = CoroutineScope(Dispatchers.Main)

    fun loadNotifications() {
        if (state.isLoading) return
        state = state.copy(isLoading = true, errorMessage = null)

        scope.launch {
            val notificationResult = repository.getNotifications()
            val countResult = repository.getUnreadCount()

            val notifications = notificationResult.getOrNull()
            if (notifications == null) {
                state = state.copy(
                    isLoading = false,
                    errorMessage = notificationResult.exceptionOrNull()?.message ?: "Không tải được thông báo"
                )
                return@launch
            }

            state = state.copy(
                isLoading = false,
                notifications = notifications,
                unreadCount = countResult.getOrNull() ?: notifications.count { !it.isRead }
            )
        }
    }

    fun markRead(notificationId: Int) {
        if (notificationId <= 0) return
        scope.launch {
            repository.markRead(notificationId).fold(
                onSuccess = { updated ->
                    state = state.copy(
                        notifications = state.notifications.map {
                            if (it.id == updated.id) updated else it
                        },
                        unreadCount = state.notifications
                            .map { if (it.id == updated.id) updated else it }
                            .count { !it.isRead }
                    )
                },
                onFailure = { error ->
                    state = state.copy(errorMessage = error.message ?: "Không thể cập nhật thông báo")
                }
            )
        }
    }

    fun markAllRead() {
        if (state.isMarkingAllRead) return
        state = state.copy(isMarkingAllRead = true, errorMessage = null)

        scope.launch {
            repository.markAllRead().fold(
                onSuccess = {
                    state = state.copy(
                        isMarkingAllRead = false,
                        notifications = state.notifications.map { it.copy(isRead = true) },
                        unreadCount = 0
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        isMarkingAllRead = false,
                        errorMessage = error.message ?: "Không thể đánh dấu đã đọc"
                    )
                }
            )
        }
    }
}
