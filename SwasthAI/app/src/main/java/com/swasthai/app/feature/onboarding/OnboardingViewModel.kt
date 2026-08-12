package com.swasthai.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Onboarding flow.
 *
 * Manages onboarding state: language selection, role selection,
 * first-launch detection, and existing session detection.
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
