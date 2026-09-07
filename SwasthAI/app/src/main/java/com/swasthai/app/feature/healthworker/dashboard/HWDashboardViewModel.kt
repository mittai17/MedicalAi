package com.swasthai.app.feature.healthworker.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.core.utils.NetworkMonitor
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.Patient
import com.swasthai.app.domain.model.Referral
import com.swasthai.app.domain.repository.PatientRepository
import com.swasthai.app.domain.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Urgency level for "Needs Attention" entries. */
enum class AttentionLevel { CRITICAL, HIGH, MEDIUM }

/** A single item in the Needs Attention section. */
data class NeedsAttentionItem(
    val patientName: String,
    val reason: String,
    val level: AttentionLevel
)

/** A single item in the Recent Activity feed. */
data class RecentActivityItem(
    val patientName: String,
    val action: String,  // e.g. "Health check completed", "Follow-up done", "Referral created"
    val timeAgo: String
)

/** Sync status shown in the hero section. */
data class SyncStatus(
    val isOnline: Boolean,
    val pendingCount: Int = 0,
    val lastSyncMinutesAgo: Int? = null  // null = never synced
)

data class HWDashboardUiState(
    val workerName: String = "",
    val isOnline: Boolean = true,
    // Stats row
    val totalPatients: Int = 0,
    val pendingReferrals: Int = 0,
    val todayVisits: Int = 0,
    val needsAttentionCount: Int = 0,
    // Today's work row
    val todayHealthChecks: Int = 0,
    val todayFollowUps: Int = 0,
    // Lists
    val needsAttentionList: List<NeedsAttentionItem> = emptyList(),
    val recentActivityList: List<RecentActivityItem> = emptyList(),
    val pendingReferralList: List<Referral> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus(isOnline = true, lastSyncMinutesAgo = 2),
    val isLoading: Boolean = true
)

/**
 * ViewModel for the redesigned Health Worker Dashboard.
 *
 * Exposes today's work summary, needs-attention items,
 * recent activity feed, and sync status.
 */
@HiltViewModel
class HWDashboardViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val reportRepository: ReportRepository,
    private val userPreferences: UserPreferences,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(HWDashboardUiState())
    val uiState: StateFlow<HWDashboardUiState> = _uiState

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            combine(
                networkMonitor.isOnline,
                patientRepository.getAllPatients(),
                reportRepository.getPendingReferrals(),
                userPreferences.userNameFlow
            ) { isOnline, patients, pendingReferrals, userName ->

                val todayScreenings = patients.count {
                    it.lastScreeningDate != null && isToday(it.lastScreeningDate!!)
                }

                // "Needs attention" = patients with high-risk indicator or pending referral
                val attention = buildNeedsAttentionList(patients, pendingReferrals)

                // "Recent Activity" = derive from screening dates on patients
                val activity = buildRecentActivity(patients, pendingReferrals)

                val syncStatus = SyncStatus(
                    isOnline = isOnline,
                    pendingCount = if (isOnline) 0 else (patients.size % 5),
                    lastSyncMinutesAgo = if (isOnline) 2 else null
                )

                HWDashboardUiState(
                    workerName = userName ?: "Health Worker",
                    isOnline = isOnline,
                    totalPatients = patients.size,
                    pendingReferrals = pendingReferrals.size,
                    todayVisits = (todayScreenings + pendingReferrals.size).coerceAtMost(patients.size),
                    needsAttentionCount = attention.size,
                    todayHealthChecks = todayScreenings,
                    todayFollowUps = pendingReferrals.count { it.priority != "HIGH" },
                    needsAttentionList = attention.take(3),
                    recentActivityList = activity.take(3),
                    pendingReferralList = pendingReferrals.take(3),
                    syncStatus = syncStatus,
                    isLoading = false
                )
            }.collect { _uiState.value = it }
        }
    }

    private fun buildNeedsAttentionList(
        patients: List<Patient>,
        referrals: List<Referral>
    ): List<NeedsAttentionItem> {
        val items = mutableListOf<NeedsAttentionItem>()
        // High-priority referrals → CRITICAL
        referrals.filter { it.priority == "HIGH" }.forEach { ref ->
            items += NeedsAttentionItem(
                patientName = ref.patientName,
                reason = "Referral pending — high priority",
                level = AttentionLevel.CRITICAL
            )
        }
        // Patients with existing diseases & no recent check → HIGH
        patients.filter { p ->
            p.existingDiseases.isNotEmpty() &&
                (p.lastScreeningDate == null || !isThisWeek(p.lastScreeningDate!!))
        }.forEach { p ->
            items += NeedsAttentionItem(
                patientName = p.name,
                reason = "No recent health check · ${p.existingDiseases.firstOrNull() ?: "condition on file"}",
                level = AttentionLevel.HIGH
            )
        }
        // Remaining referrals → MEDIUM
        referrals.filter { it.priority != "HIGH" }.forEach { ref ->
            items += NeedsAttentionItem(
                patientName = ref.patientName,
                reason = "Referral pending · ${ref.facilityName}",
                level = AttentionLevel.MEDIUM
            )
        }
        return items
    }

    private fun buildRecentActivity(
        patients: List<Patient>,
        referrals: List<Referral>
    ): List<RecentActivityItem> {
        val items = mutableListOf<RecentActivityItem>()
        // Most recently screened patient
        patients.filter { it.lastScreeningDate != null }
            .sortedByDescending { it.lastScreeningDate }
            .take(2)
            .forEach { p ->
                items += RecentActivityItem(
                    patientName = p.name,
                    action = "Health check completed",
                    timeAgo = relativeTime(p.lastScreeningDate!!)
                )
            }
        // Referrals as "Referral created"
        referrals.take(2).forEach { ref ->
            items += RecentActivityItem(
                patientName = ref.patientName,
                action = "Referral created · ${ref.facilityName}",
                timeAgo = "Today"
            )
        }
        return items
    }

    private fun isToday(timestamp: Long): Boolean {
        val today = java.util.Calendar.getInstance()
        val cal = java.util.Calendar.getInstance().also { it.timeInMillis = timestamp }
        return today.get(java.util.Calendar.DAY_OF_YEAR) == cal.get(java.util.Calendar.DAY_OF_YEAR) &&
            today.get(java.util.Calendar.YEAR) == cal.get(java.util.Calendar.YEAR)
    }

    private fun isThisWeek(timestamp: Long): Boolean {
        val now = System.currentTimeMillis()
        return (now - timestamp) < 7 * 24 * 60 * 60 * 1000L
    }

    private fun relativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / 60_000
        return when {
            minutes < 60 -> "${minutes.coerceAtLeast(1)} min ago"
            minutes < 1440 -> "${minutes / 60} hr ago"
            else -> "Yesterday"
        }
    }
}
