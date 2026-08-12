package com.swasthai.app.feature.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.UserRole
import com.swasthai.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for Authentication screens.
 */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val role: UserRole) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

/**
 * ViewModel for Login and Register screens.
 *
 * Handles authentication logic via the AuthRepository,
 * supporting both online and offline authentication.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun login(phone: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.login(phone, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Success(user.role)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "Login failed. Please check your credentials."
                    )
                }
            )
        }
    }

    fun loginOffline(phone: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.loginOffline(phone, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Success(user.role)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "Offline login failed. Please register first."
                    )
                }
            )
        }
    }

    fun register(name: String, phone: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            // Get the selected role from preferences (set during role selection)
            val role = userPreferences.userRoleFlow.first() ?: UserRole.CITIZEN

            val result = authRepository.register(name, phone, password, role)
            result.fold(
                onSuccess = { user ->
                    userPreferences.setOnboardingCompleted()
                    _uiState.value = AuthUiState.Success(user.role)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "Registration failed. Please try again."
                    )
                }
            )
        }
    }
}
