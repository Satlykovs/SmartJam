package com.smartjam.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjam.app.domain.model.Connection
import com.smartjam.app.domain.model.UserRole
import com.smartjam.app.domain.repository.AuthRepository
import com.smartjam.app.domain.repository.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeState(
    val currentRole: UserRole = UserRole.STUDENT,
    val connections: List<Connection> = emptyList(),
    val inviteCodeInput: String = "",
    val teacherGeneratedCode: String? = null,
    val isLoading: Boolean = false,
    val isPaging: Boolean = false,
    val endReached: Boolean = false,
    val nextPage: Int = 0,
    val pageSize: Int = 20,
    val errorMessage: String? = null,
)

sealed class HomeEvent {
    object NavigateToLogin : HomeEvent()

    data class NavigateToRoom(val connectionId: String) : HomeEvent()

    data class ShowToast(val message: String) : HomeEvent()
}

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val connectionRepository: ConnectionRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<HomeEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var connectionJob: Job? = null
    private var isSyncing = false

    val refreshTicker = flow {
        while (true) {
            emit(Unit)
            delay(45_000L)
        }
    }

    init {
        viewModelScope.launch {
            authRepository.userRole.collect { roleString ->
                val newRole =
                    try {
                        UserRole.valueOf(roleString ?: "STUDENT")
                    } catch (e: Exception) {
                        UserRole.STUDENT
                    }
                _state.update { it.copy(currentRole = newRole) }

                connectionRepository.clearAllConnections()
                startObservingConnections()
            }
        }
    }

    private fun startObservingConnections() {
        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            connectionRepository.getConnectionsFlow(_state.value.currentRole).collect { list ->
                _state.update { it.copy(connections = list) }
            }
        }
        syncNetworkData()
    }

    fun syncNetworkData() {
        if (isSyncing) return
        viewModelScope.launch {
            isSyncing = true
            try {
                connectionRepository.syncConnectionsPage(
                    _state.value.currentRole,
                    0,
                    _state.value.pageSize,
                )
            } finally {
                isSyncing = false
            }
        }
    }

    private suspend fun refreshFirstPage() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        val result =
            connectionRepository.syncConnectionsPage(
                _state.value.currentRole,
                page = 0,
                size = _state.value.pageSize,
            )
        if (result.isSuccess) {
            _state.update { it.copy(isLoading = false, errorMessage = null) }
        } else {
            _state.update { it.copy(isLoading = false, errorMessage = "Ошибка обновления") }
        }
    }

    fun loadNextPage() {
        if (_state.value.isPaging || _state.value.endReached) return
        viewModelScope.launch {
            _state.update { it.copy(isPaging = true) }
            val result =
                connectionRepository.syncConnectionsPage(
                    _state.value.currentRole,
                    page = _state.value.nextPage,
                    size = _state.value.pageSize,
                )
            if (result.isSuccess) {
                val pageInfo = result.getOrNull()!!
                _state.update {
                    it.copy(
                        nextPage = pageInfo.pageNumber + 1,
                        endReached = pageInfo.pageNumber + 1 >= pageInfo.totalPages,
                    )
                }
            }
            _state.update { it.copy(isPaging = false) }
        }
    }

    fun onInviteCodeInputChanged(code: String) {
        _state.update { it.copy(inviteCodeInput = code, errorMessage = null) }
    }

    fun onJoinRoomClicked() {
        if (_state.value.inviteCodeInput.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = connectionRepository.joinByCode(_state.value.inviteCodeInput)
            if (result.isSuccess) {
                _state.update { it.copy(inviteCodeInput = "") }
                eventChannel.send(HomeEvent.ShowToast("Заявка отправлена!"))
                refreshFirstPage()
            } else {
                _state.update { it.copy(errorMessage = "Неверный код") }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun onGenerateCodeClicked() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = connectionRepository.generateInviteCode()
            if (result.isSuccess) {
                _state.update { it.copy(teacherGeneratedCode = result.getOrNull()) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun toggleDebugRole() {
        val newRole =
            if (_state.value.currentRole == UserRole.STUDENT) UserRole.TEACHER else UserRole.STUDENT
        viewModelScope.launch {
            if (!authRepository.refreshWithRole(newRole)) {
                eventChannel.send(HomeEvent.NavigateToLogin)
            }
        }
    }

    fun onConnectionClicked(id: String) {
        viewModelScope.launch { eventChannel.send(HomeEvent.NavigateToRoom(id)) }
    }

    fun onLogoutClicked() {
        viewModelScope.launch {
            connectionRepository.clearAllConnections()
            authRepository.logout()
            eventChannel.send(HomeEvent.NavigateToLogin)
        }
    }

    fun onListScrolled(lastVisible: Int, total: Int) {
        if (total > 0 && lastVisible >= total - 5) loadNextPage()
    }
}
