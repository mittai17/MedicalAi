package com.swasthai.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.Reminder
import com.swasthai.app.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Onboarding flow.
 *
 * Manages onboarding state: language selection, role selection,
 * first-launch detection, existing session detection, and the
 * persistent patient profile (name/phone/age/sex/conditions) used
 * everywhere in the citizen area.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val isLoggedIn: Flow<Boolean> = userPreferences.isLoggedInFlow
    val onboardingCompleted: Flow<Boolean> = userPreferences.onboardingCompletedFlow
    val userRole: Flow<UserRole?> = userPreferences.userRoleFlow
    val userName: Flow<String?> = userPreferences.userNameFlow
    val userPhone: Flow<String?> = userPreferences.userPhoneFlow
    val language: Flow<String> = userPreferences.languageFlow
    val userAge: Flow<Int?> = userPreferences.userAgeFlow
    val userSex: Flow<String?> = userPreferences.userSexFlow
    val userConditions: Flow<List<String>> = userPreferences.userConditionsFlow
    val notificationsEnabled: Flow<Boolean> = userPreferences.notificationsEnabledFlow
    val autoBackgroundRefresh: Flow<Boolean> = userPreferences.autoBackgroundRefreshFlow
    val darkMode: Flow<Boolean> = userPreferences.darkModeFlow
    val reminders: Flow<List<Reminder>> = userPreferences.remindersFlow

    private val _selectedRole = MutableStateFlow<UserRole?>(null)
    val selectedRole: StateFlow<UserRole?> = _selectedRole

    fun saveLanguage(languageCode: String) {
        viewModelScope.launch {
            userPreferences.saveLanguage(languageCode)
        }
    }

    fun saveRole(role: UserRole) {
        _selectedRole.value = role
        viewModelScope.launch {
            userPreferences.saveRole(role)
        }
    }

    /**
     * Enters the app as the given role without going through login.
     *
     * Uses the device's stable anonymous identity (generated once and kept
     * across logout) so records never get orphaned, and keeps an existing
     * name instead of overwriting it with a placeholder.
     */
    fun completeAsRole(role: UserRole) {
        _selectedRole.value = role
        viewModelScope.launch {
            val stableId = userPreferences.stableUserId()
            userPreferences.saveUserSession(
                userId = stableId,
                userName = if (role == UserRole.HEALTH_WORKER) "Health Worker" else "User",
                userRole = role,
                phone = ""
            )
            userPreferences.setOnboardingCompleted()
        }
    }

    /**
     * Reload the current user id for an already-persisted session.
     * Used by screens that boot after onboarding.
     */
    suspend fun currentUserId(): String = userPreferences.stableUserId()

    fun updateProfile(
        name: String?,
        phone: String?,
        age: Int?,
        sex: String?,
        conditions: List<String>
    ) {
        viewModelScope.launch {
            userPreferences.updateProfile(name, phone, age, sex, conditions)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setNotificationsEnabled(enabled)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setDarkMode(enabled)
        }
    }

    fun setAutoBackgroundRefresh(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoBackgroundRefresh(enabled)
        }
    }

    fun addReminder(title: String, note: String) {
        viewModelScope.launch {
            userPreferences.addReminder(
                Reminder(
                    id = java.util.UUID.randomUUID().toString(),
                    title = title,
                    note = note,
                    timeInMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun removeReminder(id: String) {
        viewModelScope.launch {
            userPreferences.removeReminder(id)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferences.setOnboardingCompleted()
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearSession()
        }
    }
}
