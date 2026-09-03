package com.swasthai.app.core.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.swasthai.app.data.local.datastore.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires both health reminders and follow-up alerts at the scheduled moment.
 *
 * Honors the notifications toggle: if the user disabled notifications, the
 * firing alarm is silently dropped (the toggle is enforced here centrally,
 * no matter how the alarm was scheduled).
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REMINDER, ACTION_FOLLOW_UP -> Unit
            else -> return
        }

        val title = intent.getStringExtra(EXTRA_TITLE)
        val id = intent.getStringExtra(EXTRA_ID)
        if (title.isNullOrBlank() || id == null) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = UserPreferences(context.applicationContext)
                val enabled = prefs.notificationsEnabledFlow.first()
                if (!enabled) return@launch
                ReminderNotifications.show(
                    context,
                    id.hashCode(),
                    title,
                    intent.getStringExtra(EXTRA_TEXT) ?: ""
                )
                // Recurring reminders re-arm themselves for the next occurrence.
                if (intent.action == ACTION_REMINDER) {
                    val firedAt = intent.getLongExtra(EXTRA_FIRE_TIME, System.currentTimeMillis())
                    ReminderScheduler(context.applicationContext)
                        .advanceRecurring(id, firedAt)
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMINDER = "com.swasthai.app.action.HEALTH_REMINDER"
        const val ACTION_FOLLOW_UP = "com.swasthai.app.action.FOLLOW_UP"
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_FIRE_TIME = "extra_fire_time"
        const val EXTRA_REPEAT = "extra_repeat"
    }
}