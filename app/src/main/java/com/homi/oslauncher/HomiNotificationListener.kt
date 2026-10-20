package com.homi.oslauncher

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONArray
import org.json.JSONObject

class HomiNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshStoredNotifications()
    }
    override fun onNotificationPosted(sbn: StatusBarNotification) = refreshStoredNotifications()
    override fun onNotificationRemoved(sbn: StatusBarNotification) = refreshStoredNotifications()

    private fun refreshStoredNotifications() {
        try {
            val items = activeNotifications.orEmpty()
                .filterNot { it.packageName == packageName }
                .sortedByDescending { it.postTime }
                .take(30)
            val array = JSONArray()
            items.forEach { sbn ->
                val extras = sbn.notification.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                    ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
                if (title.isNotBlank() || text.isNotBlank()) {
                    array.put(JSONObject()
                        .put("key", sbn.key)
                        .put("package", sbn.packageName)
                        .put("title", title)
                        .put("text", text)
                        .put("time", sbn.postTime))
                }
            }
            getSharedPreferences("homi_notifications", MODE_PRIVATE).edit()
                .putString("items", array.toString()).apply()
        } catch (_: Exception) {
            // Keep the launcher usable if Android temporarily disconnects the listener.
        }
    }
}
