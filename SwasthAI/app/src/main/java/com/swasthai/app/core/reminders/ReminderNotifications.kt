package com.swasthai.app.core.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Small helpers for the SwasthAI health-reminder notification channel.
 */
object ReminderNotifications {

    const val CHANNEL_ID = "swasthai_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Health reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Medication and health follow-up reminders"
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a reminder notification. Silently does nothing when the
     * POST_NOTIFICATIONS permission is missing/denied.
     */
    fun show(context: Context, id: Int, title: String, text: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied — skip rather than crash.
        }
    }
}