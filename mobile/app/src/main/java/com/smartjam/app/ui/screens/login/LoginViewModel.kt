package com.smartjam.app.ui.screens.login


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartjam.app.domain.repository.AuthRepository
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
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class LoginEvent{
    object NavigateToHome : LoginEvent()
    data class ShowToast(val message: String) : LoginEvent()
}

class LoginViewModel (
    private val authRepository: AuthRepository
) : ViewModel(){

    private val _state = MutableStateFlow(LoginState())
    val state : StateFlow<LoginState> = _state.asStateFlow()

    private val eventChannel = Channel<LoginEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun onPasswordChanged(newPassword: String){
        _state.value = _state.value.copy(
            passwordInput = newPassword,
            errorMessage = null
        )
    }

    fun onEmailChanged(newEmail: String) {
        _state.value = _state.value.copy(
            emailInput = newEmail,
            errorMessage = null
        )
    }

    fun onLoginClicked() {
        if (_state.value.isLoading){
            return;
        }
        val currentEmail = _state.value.emailInput
        val currentPassword = _state.value.passwordInput

        if (currentPassword.isBlank() || currentEmail.isBlank()){
            _state.value = _state.value.copy(errorMessage = "Fill in all fields")
            return;
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            try {
                val result = authRepository.login(currentEmail, currentPassword)

                if (result.isSuccess){
                    eventChannel.send(LoginEvent.NavigateToHome)
                }
                else{
                    val error = result.exceptionOrNull()?.message ?: "Error"
                    _state.update { it.copy(errorMessage = error) }
                }
            } catch (e: Exception){
                _state.value = _state.value.copy(
                    errorMessage = e.message?: "Unknown error"
                )
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

}


class LoginViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}