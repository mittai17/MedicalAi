package com.swasthai.app.core.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.sync.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Restores health reminders and follow-up alarms after a reboot / app update
 * and re-queues the periodic background sync.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var followUpScheduler: FollowUpScheduler

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> Unit
            else -> return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (userPreferences.notificationsEnabledFlow.first()) {
                    reminderScheduler.reschedule(userPreferences.remindersFlow.first())
                    followUpScheduler.reschedulePending()
                }
                SyncWorker.enqueuePeriodicSync(WorkManager.getInstance(context.applicationContext))
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}