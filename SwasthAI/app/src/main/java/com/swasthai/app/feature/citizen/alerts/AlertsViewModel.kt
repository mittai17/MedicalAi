package com.swasthai.app.feature.citizen.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.ai.engine.DiseaseKnowledgeBase
import com.swasthai.app.core.reminders.ReminderScheduler
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.Reminder
import com.swasthai.app.domain.model.RiskLevel
import com.swasthai.app.domain.repository.ScreeningRepository
import com.swasthai.app.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * A concrete, data-driven follow-up suggestion derived from a past screening
 * whose risk was flagged (anything above LOW).
 */
data class FollowUpAlert(
    val screeningId: String,
    val disease: String,
    val riskLevel: RiskLevel,
    val hint: String,
    val date: Long
)

/**
 * State for the Alerts & Reminders screen.
 */
data class AlertsUiState(
    val loading: Boolean = true,
    val reminders: List<Reminder> = emptyList(),
    val followUps: List<FollowUpAlert> = emptyList(),
    val syncCount: Int? = null,
    val isSyncing: Boolean = false,
    val lastSyncMessage: String? = null,
    val lastSyncTimestamp: Long = 0L,
    val notificationsEnabled: Boolean = true
)

/**
 * Backs the Alerts & Reminders screen with real data:
 *  - reminders are the persisted ones from this device,
 *  - follow-up alerts come from past screenings whose risk != LOW,
 *  - the sync card reflects the actual offline sync queue.
 */
@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val screeningRepository: ScreeningRepository,
    private val syncRepository: SyncRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private data class AlertsExtra(
        val loading: Boolean = true,
        val followUps: List<FollowUpAlert> = emptyList(),
        val isSyncing: Boolean = false,
        val lastSyncMessage: String? = null
    )

    private val _extra = MutableStateFlow(AlertsExtra())
    private val dismissedFollowUps = mutableSetOf<String>()

    val uiState: StateFlow<AlertsUiState> = combine(
        userPreferences.remindersFlow,
        userPreferences.notificationsEnabledFlow,
        userPreferences.lastSyncTimestampFlow,
        syncRepository.getPendingSyncCount(),
        _extra
    ) { reminders, notif, lastSync, syncCount, extra ->
        AlertsUiState(
            loading = extra.loading,
            reminders = reminders,
            followUps = extra.followUps,
            syncCount = syncCount,
            isSyncing = extra.isSyncing,
            lastSyncMessage = extra.lastSyncMessage,
            lastSyncTimestamp = lastSync,
            notificationsEnabled = notif
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlertsUiState()
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            buildFollowUps()
            _extra.update { it.copy(loading = false) }
        }
    }

    private suspend fun buildFollowUps() {
        val userId = userPreferences.stableUserId()
        val latest = screeningRepository
            .getScreeningsByUser(userId)
            .first()
            .sortedByDescending { it.createdAt }

        // Only inspect the most recent handful; group per disease.
        val alerts = latest
            .take(5)
            .mapNotNull { screeningRepository.getScreeningDetail(it.id) }
            .filter { it.diagnosis != null }
            .groupBy { it.diagnosis!!.predictedDisease.lowercase() }
            .mapValues { (_, details) -> details.maxByOrNull { it.screening.createdAt }!! }
            .values
            .filterNot { it.diagnosis!!.riskLevel == RiskLevel.LOW }
            .filter { it.screening.id !in dismissedFollowUps }
            .map { detail ->
                val diagnosis = detail.diagnosis!!
                val advice = diagnosis.medicalAdvice
                    ?: DiseaseKnowledgeBase.adviceFor(diagnosis.predictedDisease)
                FollowUpAlert(
                    screeningId = detail.screening.id,
                    disease = diagnosis.predictedDisease,
                    riskLevel = diagnosis.riskLevel,
                    hint = advice?.consultHint()
                        ?: "Follow up with a health professional about ${diagnosis.predictedDisease}.",
                    date = detail.screening.createdAt
                )
            }

        _extra.update { it.copy(followUps = alerts) }
    }

    fun syncNow() {
        if (_extra.value.isSyncing) return
        viewModelScope.launch {
            _extra.update { it.copy(isSyncing = true, lastSyncMessage = null) }
            val result = syncRepository.syncPendingData()
            if (result.isSuccess) {
                userPreferences.setLastSyncTimestamp(System.currentTimeMillis())
            }
            _extra.update {
                it.copy(
                    lastSyncMessage = result.fold(
                        onSuccess = { count -> "Synced $count record(s)." },
                        onFailure = { "Sync failed — records will retry." }
                    ),
                    isSyncing = false
                )
            }
        }
    }

    fun addReminder(title: String, note: String, timeInMillis: Long) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val reminder = Reminder(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                note = note.trim(),
                timeInMillis = timeInMillis,
                createdAt = System.currentTimeMillis()
            )
            userPreferences.addReminder(reminder)
            reminderScheduler.schedule(reminder)
        }
    }

    fun removeReminder(id: String) {
        viewModelScope.launch {
            userPreferences.removeReminder(id)
            reminderScheduler.cancel(id)
        }
    }

    fun dismissFollowUp(screeningId: String) {
        dismissedFollowUps += screeningId
        _extra.update { it.copy(followUps = it.followUps.filterNot { a -> a.screeningId == screeningId }) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setNotificationsEnabled(enabled)
            val current = userPreferences.remindersFlow.first()
            if (enabled) {
                reminderScheduler.reschedule(current)
            } else {
                current.forEach { reminderScheduler.cancel(it.id) }
            }
        }
    }
}

private fun com.swasthai.app.domain.model.MedicalAdvice.consultHint(): String =
    if (doctorToConsult.isNotBlank()) "Consult a $doctorToConsult for follow-up." else remedy