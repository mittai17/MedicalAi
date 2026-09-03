package com.swasthai.app.core.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Schedules persisted health reminders as OS alarms.
 *
 * Uses exact alarms when the app is allowed (the user grants
 * SCHEDULE_EXACT_ALARM), otherwise falls back to a ~5 minute window alarm so
 * reminders still fire reliably without special privileges.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: Reminder) {
        val triggerAt = reminder.timeInMillis
        if (triggerAt <= System.currentTimeMillis()) return

        val pendingIntent = buildPendingIntent(reminder)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Exact alarms not permitted — schedule within a 5-min window.
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                FALLBACK_WINDOW_MILLIS,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    fun cancel(reminderId: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
        }
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                requestCode(reminderId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    fun reschedule(reminders: List<Reminder>) {
        reminders.forEach { schedule(it) }
    }

    fun buildPendingIntent(reminder: Reminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            putExtra(ReminderReceiver.EXTRA_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderReceiver.EXTRA_TEXT, reminder.note.ifBlank { "Time for your health reminder." })
            putExtra(ReminderReceiver.EXTRA_FIRE_TIME, reminder.timeInMillis)
            putExtra(ReminderReceiver.EXTRA_REPEAT, reminder.repeatIntervalMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(reminder.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Advance a recurring reminder to its next occurrence and re-arm the
     * alarm. Returns true when a next occurrence was scheduled.
     */
    suspend fun advanceRecurring(id: String, lastFireMillis: Long): Boolean {
        val prefs = UserPreferences(context)
        val reminder = prefs.remindersFlow.first().firstOrNull { it.id == id }
            ?: return false
        if (reminder.repeatIntervalMillis <= 0L) return false

        var next = lastFireMillis + reminder.repeatIntervalMillis
        if (next <= System.currentTimeMillis()) {
            next = System.currentTimeMillis() + reminder.repeatIntervalMillis
        }
        val updated = reminder.copy(timeInMillis = next)
        prefs.updateReminder(updated)
        schedule(updated)
        return true
    }

    private fun requestCode(id: String): Int = id.hashCode() and Int.MAX_VALUE

    companion object {
        private const val FALLBACK_WINDOW_MILLIS = 5L * 60L * 1000L
    }
}