package com.swasthai.app.core.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.swasthai.app.data.local.database.dao.ScreeningDao
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.RiskLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules one-shot follow-up notifications for curated screenings whose
 * risk was flagged (anything above LOW). Fires the next day at 10:00, keyed
 * off the screening id so re-scheduling is idempotent.
 */
@Singleton
class FollowUpScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val screeningDao: ScreeningDao,
    private val userPreferences: UserPreferences
) {

    fun scheduleFollowUp(
        screeningId: String,
        riskLevel: RiskLevel,
        disease: String,
        hint: String
    ) {
        if (riskLevel == RiskLevel.LOW) return

        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FOLLOW_UP
            putExtra(ReminderReceiver.EXTRA_ID, screeningId)
            putExtra(ReminderReceiver.EXTRA_TITLE, "Follow-up: $disease")
            putExtra(ReminderReceiver.EXTRA_TEXT, hint)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(screeningId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    /** Re-derives and schedules follow-ups for flagged screenings (on reboot). */
    suspend fun reschedulePending() {
        val latest = screeningDao
            .getScreeningsByUser(userPreferences.stableUserId())
            .first()
            .sortedByDescending { it.createdAt }
            .take(5)

        for (screening in latest) {
            val diagnosis = screeningDao.getDiagnosisByScreening(screening.id) ?: continue
            scheduleFollowUp(
                screeningId = screening.id,
                riskLevel = try {
                    RiskLevel.valueOf(diagnosis.riskLevel)
                } catch (_: Exception) {
                    RiskLevel.LOW
                },
                disease = diagnosis.predictedDisease,
                hint = "Follow up with a health professional about ${diagnosis.predictedDisease}."
            )
        }
    }

    private fun requestCode(screeningId: String): Int = ("followup_$screeningId").hashCode() and Int.MAX_VALUE
}