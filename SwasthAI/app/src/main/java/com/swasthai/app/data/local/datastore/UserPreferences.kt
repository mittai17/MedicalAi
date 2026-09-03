package com.swasthai.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.swasthai.app.domain.model.Reminder
import com.swasthai.app.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "swasthai_preferences"
)

/**
 * DataStore-based user preferences manager.
 *
 * Stores lightweight user session data: logged-in user ID, role,
 * language, theme preference, onboarding completion state, the
 * patient profile used to personalise screening, and persisted
 * medication/health reminders.
 *
 * The user id doubles as the device-level anonymous identity: it is
 * generated once and intentionally NOT cleared on logout, so records
 * stay linked to the same person when they sign back in.
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_USER_PHONE = stringPreferencesKey("user_phone")

        // Patient profile used to personalise reasoning.
        private val KEY_PROFILE_AGE = intPreferencesKey("profile_age")
        private val KEY_PROFILE_SEX = stringPreferencesKey("profile_sex")
        private val KEY_PROFILE_CONDITIONS = stringPreferencesKey("profile_conditions")

        // Settings.
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_AUTO_BACKGROUND_REFRESH = booleanPreferencesKey("auto_background_refresh")

        // Dashboard health widgets (manual fallback when no sensor is present).
        private val KEY_MANUAL_STEPS = intPreferencesKey("manual_steps")
        private val KEY_MANUAL_HEART_RATE = intPreferencesKey("manual_heart_rate")

        // Live step-counter anchors (survive app restarts; re-anchored daily).
        private val KEY_SENSOR_BASELINE_DAY = stringPreferencesKey("sensor_baseline_day")
        private val KEY_SENSOR_BASELINE_VALUE = intPreferencesKey("sensor_baseline_value")

        // Persisted reminders (JSON array).
        private val KEY_USER_REMINDERS = stringPreferencesKey("user_reminders")

        // Last successful background sync (for the "Last synced" label).
        private val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }

    // ── Read flows ──

    val userIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_ID]
    }

    val userNameFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_NAME]
    }

    val userRoleFlow: Flow<UserRole?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_ROLE]?.let {
            try { UserRole.valueOf(it) } catch (_: Exception) { null }
        }
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE] ?: "en"
    }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_LOGGED_IN] ?: false
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val darkModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DARK_MODE] ?: false
    }

    val userPhoneFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_PHONE]
    }

    val userAgeFlow: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[KEY_PROFILE_AGE]
    }

    val userSexFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_PROFILE_SEX]
    }

    val userConditionsFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_PROFILE_CONDITIONS]
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val autoBackgroundRefreshFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_BACKGROUND_REFRESH] ?: true
    }

    /** Manually entered step count (fallback when the device has no sensor). */
    val manualStepsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_MANUAL_STEPS] ?: 0
    }

    /** Manually entered resting heart rate (fallback when no sensor is present). */
    val manualHeartRateFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_MANUAL_HEART_RATE] ?: 0
    }

    /** Day (yyyy-MM-dd) the live step-counter baseline was anchored. */
    val sensorBaselineDayFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SENSOR_BASELINE_DAY] ?: ""
    }

    /** Counter value the live step baseline was anchored at. */
    val sensorBaselineValueFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_SENSOR_BASELINE_VALUE]?.let(Float::fromBits) ?: 0f
    }

    val remindersFlow: Flow<List<Reminder>> = context.dataStore.data.map { preferences ->
        decodeReminders(preferences[KEY_USER_REMINDERS])
    }

    /** Epoch millis of the last successful sync, or 0 if never synced. */
    val lastSyncTimestampFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_SYNC_TIMESTAMP] ?: 0L
    }

    // ── Write operations ──

    /**
     * Existing stable anonymous identity, or a fresh one persisted for good.
     * Never re-generated — guarantees records survive logout/re-entry.
     */
    suspend fun stableUserId(): String {
        val existing = context.dataStore.data.first()[KEY_USER_ID]
        if (!existing.isNullOrBlank()) return existing
        val fresh = UUID.randomUUID().toString()
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = fresh
        }
        return fresh
    }

    suspend fun saveUserSession(
        userId: String,
        userName: String,
        userRole: UserRole,
        phone: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = userId
            if (preferences[KEY_USER_NAME].isNullOrBlank()) {
                preferences[KEY_USER_NAME] = userName
            }
            preferences[KEY_USER_ROLE] = userRole.name
            preferences[KEY_USER_PHONE] = phone
            preferences[KEY_IS_LOGGED_IN] = true
        }
    }

    suspend fun saveLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = language
        }
    }

    suspend fun saveRole(role: UserRole) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_ROLE] = role.name
        }
    }

    /**
     * Update the patient profile. Blank values keep the stored name/phone;
     * null age/sex clear them, empty conditions clear the list.
     */
    suspend fun updateProfile(
        name: String?,
        phone: String?,
        age: Int?,
        sex: String?,
        conditions: List<String>
    ) {
        context.dataStore.edit { preferences ->
            if (!name.isNullOrBlank()) preferences[KEY_USER_NAME] = name
            if (!phone.isNullOrBlank()) preferences[KEY_USER_PHONE] = phone
            if (age != null) {
                preferences[KEY_PROFILE_AGE] = age
            } else {
                preferences.remove(KEY_PROFILE_AGE)
            }
            if (sex != null) {
                preferences[KEY_PROFILE_SEX] = sex
            } else {
                preferences.remove(KEY_PROFILE_SEX)
            }
            preferences[KEY_PROFILE_CONDITIONS] =
                conditions.map { it.trim() }.filter { it.isNotBlank() }.joinToString(",")
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setAutoBackgroundRefresh(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_BACKGROUND_REFRESH] = enabled
        }
    }

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    suspend fun setManualSteps(steps: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MANUAL_STEPS] = steps.coerceIn(0, 999_999)
        }
    }

    suspend fun setManualHeartRate(bpm: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MANUAL_HEART_RATE] = bpm.coerceIn(20, 300)
        }
    }

    /** Anchor the live step counter for a given day (yyyy-MM-dd). */
    suspend fun setSensorBaseline(day: String, value: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SENSOR_BASELINE_DAY] = day
            preferences[KEY_SENSOR_BASELINE_VALUE] = value.toRawBits()
        }
    }

    suspend fun addReminder(reminder: Reminder) {
        context.dataStore.edit { preferences ->
            val current = decodeReminders(preferences[KEY_USER_REMINDERS])
            preferences[KEY_USER_REMINDERS] = encodeReminders(current + reminder)
        }
    }

    suspend fun removeReminder(id: String) {
        context.dataStore.edit { preferences ->
            val current = decodeReminders(preferences[KEY_USER_REMINDERS])
            preferences[KEY_USER_REMINDERS] = encodeReminders(current.filterNot { it.id == id })
        }
    }

    /**
     * Replace a stored reminder (used to advance recurring reminders to
     * their next fire time). No-op if the id is no longer stored.
     */
    suspend fun updateReminder(reminder: Reminder) {
        context.dataStore.edit { preferences ->
            val current = decodeReminders(preferences[KEY_USER_REMINDERS])
            preferences[KEY_USER_REMINDERS] = encodeReminders(
                current.map { if (it.id == reminder.id) reminder else it }
            )
        }
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = enabled
        }
    }

    /**
     * Logs the user out of the session (role/phone cleared) but keeps the
     * stable [KEY_USER_ID] so this device's records stay linked on re-entry.
     */
    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_USER_ROLE)
            preferences.remove(KEY_USER_PHONE)
            preferences[KEY_IS_LOGGED_IN] = false
        }
    }

    private fun decodeReminders(raw: String?): List<Reminder> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Reminder(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    note = obj.optString("note"),
                    timeInMillis = obj.getLong("time"),
                    repeatIntervalMillis = obj.optLong("repeat", 0L),
                    createdAt = obj.optLong("created", System.currentTimeMillis())
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeReminders(reminders: List<Reminder>): String {
        val array = JSONArray()
        reminders.forEach { r ->
            array.put(
                org.json.JSONObject()
                    .put("id", r.id)
                    .put("title", r.title)
                    .put("note", r.note)
                    .put("time", r.timeInMillis)
                    .put("repeat", r.repeatIntervalMillis)
                    .put("created", r.createdAt)
            )
        }
        return array.toString()
    }
}