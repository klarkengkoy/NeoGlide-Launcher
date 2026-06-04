package com.samidevstudio.pxllauncherneo.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PxlNotificationListener : NotificationListenerService() {

    companion object {
        private val _activeNotifications = MutableStateFlow<Map<String, Int>>(emptyMap())
        val activeNotifications: StateFlow<Map<String, Int>> = _activeNotifications.asStateFlow()

        private var instance: PxlNotificationListener? = null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        updateNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateNotifications()
    }

    private fun updateNotifications() {
        try {
            val notifications = getActiveNotifications()
            val counts = notifications?.groupBy { it.packageName }
                ?.mapValues { it.value.size } ?: emptyMap()
            _activeNotifications.value = counts
        } catch (_: Exception) {
            // Service might be disconnected
        }
    }
}
