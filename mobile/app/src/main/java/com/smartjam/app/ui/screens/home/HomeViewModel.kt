package com.smartjam.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartjam.app.domain.model.Connection
import com.smartjam.app.domain.model.UserRole
import com.smartjam.app.domain.repository.AuthRepository
import com.smartjam.app.domain.repository.ConnectionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val currentRole: UserRole = UserRole.STUDENT,
    val activeConnections: List<Connection> = emptyList(),
    val pendingConnections: List<Connection> = emptyList(),
    val inviteCodeInput: String = "",
    val teacherGeneratedCode: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class HomeEvent {
    object NavigateToLogin : HomeEvent()
    data class NavigateToRoom(val connectionId: String) : HomeEvent()
    data class ShowToast(val message: String) : HomeEvent()
}

class HomeViewModel(
    private val connectionRepository: ConnectionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<HomeEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var connectionJob: Job? = null

    init {
        startObservingConnections()
    }

    fun toggleDebugRole() {
        val newRole = if (_state.value.currentRole == UserRole.STUDENT) {
            UserRole.TEACHER
        } else {
            UserRole.STUDENT
        }

        _state.update { it.copy(
            currentRole = newRole,
            activeConnections = emptyList(),
            pendingConnections = emptyList(),
            errorMessage = null
        ) }

        startObservingConnections()
    }

    private fun startObservingConnections() {
        connectionJob?.cancel()

        connectionJob = viewModelScope.launch {
            val role = _state.value.currentRole

            launch {
                connectionRepository.getConnectionsFlow(role).collect { connections ->
                    _state.update { currentState ->
                        currentState.copy(
                            activeConnections = connections.filter { it.status == "ACTIVE" },
                            pendingConnections = connections.filter { it.status == "PENDING" }
                        )
                    }
                }
            }

            syncNetworkData()
        }
    }

    fun syncNetworkData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val result = connectionRepository.syncConnections(_state.value.currentRole)

            if (result.isFailure) {
                _state.update { it.copy(errorMessage = "Не удалось обновить данные с сервера") }
            }

            _state.update { it.copy(isLoading = false) }
        }
    }

    fun onInviteCodeInputChanged(code: String) {
        _state.update { it.copy(inviteCodeInput = code, errorMessage = null) }
    }

    fun onJoinRoomClicked() {
        val code = _state.value.inviteCodeInput
        if (code.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val result = connectionRepository.joinByCode(code)

            _state.update { it.copy(isLoading = false) }

            if (result.isSuccess) {
                _state.update { it.copy(inviteCodeInput = "") }
                eventChannel.send(HomeEvent.ShowToast("Заявка успешно отправлена!"))
                syncNetworkData()
            } else {
                _state.update { it.copy(errorMessage = "Неверный код или ошибка сервера") }
            }
        }
    }

    fun onGenerateCodeClicked() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val result = connectionRepository.generateInviteCode()

            _state.update { it.copy(isLoading = false) }

            if (result.isSuccess) {
                _state.update { it.copy(teacherGeneratedCode = result.getOrNull()) }
            } else {
                _state.update { it.copy(errorMessage = "Не удалось сгенерировать код") }
            }
        }
    }

    fun onRespondToRequest(connectionId: String, accept: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val result = connectionRepository.respondToRequest(connectionId, accept)

            _state.update { it.copy(isLoading = false) }

            if (result.isSuccess) {
                val msg = if (accept) "Ученик добавлен" else "Заявка отклонена"
                eventChannel.send(HomeEvent.ShowToast(msg))
                syncNetworkData()
            } else {
                _state.update { it.copy(errorMessage = "Ошибка при обработке заявки") }
            }
        }
    }

    fun onConnectionClicked(connectionId: String) {
        viewModelScope.launch {
            eventChannel.send(HomeEvent.NavigateToRoom(connectionId))
        }
    }

    fun onLogoutClicked() {
        viewModelScope.launch {
            authRepository.logout()
            eventChannel.send(HomeEvent.NavigateToLogin)
        }
    }
}

class HomeViewModelFactory(
    private val connectionRepository: ConnectionRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(connectionRepository, authRepository) as T
    }
}