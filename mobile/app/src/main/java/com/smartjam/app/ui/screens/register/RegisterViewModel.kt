package com.smartjam.app.ui.screens.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjam.app.domain.repository.AuthRepository
import com.smartjam.app.domain.data.UserRole
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider

data class RegisterState(
    val usernameInput: String = "",
    val emailInput: String = "",
    val passwordInput: String = "",
    val repeatPasswordInput: String = "",
    val selectedRole: UserRole = UserRole.STUDENT,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class RegisterEvent {
    object NavigateToHome : RegisterEvent()
    object NavigateBack : RegisterEvent()
}

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<RegisterEvent>()
    val events = eventChannel.receiveAsFlow()
    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$".toRegex()
    private val passwordRegex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}\$".toRegex()

    fun onUsernameChanged(username: String) {
        _state.update { it.copy(usernameInput = username, errorMessage = null) }
    }

    fun onEmailChanged(email: String) {
        _state.update { it.copy(emailInput = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _state.update { it.copy(passwordInput = password, errorMessage = null) }
    }

    fun onRepeatPasswordChanged(password: String) {
        _state.update { it.copy(repeatPasswordInput = password, errorMessage = null) }
    }

    fun onRoleSelected(role: UserRole) {
        _state.update { it.copy(selectedRole = role) }
    }

    fun onBackClicked() {
        viewModelScope.launch {
            eventChannel.send(RegisterEvent.NavigateBack)
        }
    }

    fun onRegisterClicked() {
        val currentState = _state.value

        if (currentState.usernameInput.isBlank()) {
            _state.update { it.copy(errorMessage = "Введите имя пользователя") }
            return
        }

        if (!emailRegex.matches(currentState.emailInput)) {
            _state.update { it.copy(errorMessage = "Некорректный формат email") }
            return
        }

        if (!passwordRegex.matches(currentState.passwordInput)) {
            _state.update { it.copy(errorMessage = "Пароль: мин. 8 символов, латинские буквы и цифры") }
            return
        }

        if (currentState.passwordInput != currentState.repeatPasswordInput) {
            _state.update { it.copy(errorMessage = "Пароли не совпадают") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.register(
                email = currentState.emailInput,
                password = currentState.passwordInput,
                username = currentState.usernameInput,
                role = currentState.selectedRole.name
            )

            _state.update { it.copy(isLoading = false) }

            if (result.isSuccess) {
                eventChannel.send(RegisterEvent.NavigateToHome)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Ошибка регистрации"
                _state.update { it.copy(errorMessage = error) }
            }
        }
    }
}

class RegisterViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            return RegisterViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}