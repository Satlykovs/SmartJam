package com.smartjam.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartjam.app.domain.repository.AuthRepository
import com.smartjam.app.domain.repository.ConnectionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val selectedRole: com.smartjam.app.domain.model.UserRole =
        com.smartjam.app.domain.model.UserRole.STUDENT,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed class LoginEvent {
    object NavigateToHome : LoginEvent()

    data class ShowToast(val message: String) : LoginEvent()
}

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val tokenStorage: com.smartjam.app.data.local.TokenStorage,
    private val connectionRepository: ConnectionRepository
) : ViewModel(){

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val eventChannel = Channel<LoginEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun onPasswordChanged(newPassword: String) {
        _state.update { it.copy(passwordInput = newPassword, errorMessage = null) }
    }

    fun onEmailChanged(newEmail: String) {
        _state.update { it.copy(emailInput = newEmail, errorMessage = null) }
    }

    fun onRoleSelected(role: com.smartjam.app.domain.model.UserRole) {
        _state.update { it.copy(selectedRole = role) }
    }

    fun onLoginClicked() {
        if (_state.value.isLoading) {
            return
        }
        val currentEmail = _state.value.emailInput
        val currentPassword = _state.value.passwordInput
        val selectedRole = _state.value.selectedRole

        if (currentPassword.isBlank() || currentEmail.isBlank()) {
            _state.update { it.copy(errorMessage = "Fill in all fields") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result = authRepository.login(currentEmail, currentPassword, selectedRole)

                if (result.isSuccess){
                    connectionRepository.clearAllConnections()
                    eventChannel.send(LoginEvent.NavigateToHome)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Error"
                    _state.update { it.copy(errorMessage = error) }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = e.message ?: "Unknown error")
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
}

class LoginViewModelFactory(
    private val authRepository: AuthRepository,
    private val tokenStorage: com.smartjam.app.data.local.TokenStorage,
    private val connectionRepository: ConnectionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(authRepository, tokenStorage, connectionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
