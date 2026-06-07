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
    val connections: List<Connection> = emptyList(),
    val inviteCodeInput: String = "",
    val teacherGeneratedCode: String? = null,
    val isLoading: Boolean = false,
    val isPaging: Boolean = false,
    val endReached: Boolean = false,
    val nextPage: Int = 1,
    val pageSize: Int = 20,
    val errorMessage: String? = null,
)

sealed class HomeEvent {
    object NavigateToLogin : HomeEvent()

    data class NavigateToRoom(val connectionId: String) : HomeEvent()

    data class ShowToast(val message: String) : HomeEvent()
}

class HomeViewModel(
    private val connectionRepository: ConnectionRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<HomeEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var connectionJob: Job? = null
    private var pollingJob: Job? = null
    private var hasStarted = false

    init {
        viewModelScope.launch {
            authRepository.userRole.collect { roleString ->
                val newRole =
                    try {
                        UserRole.valueOf(roleString ?: "STUDENT")
                    } catch (e: Exception) {
                        UserRole.STUDENT
                    }

                if (!hasStarted || _state.value.currentRole != newRole) {
                    hasStarted = true
                    _state.update { it.copy(currentRole = newRole) }
                    startObservingConnections()
                }
            }
        }
    }

    fun toggleDebugRole() {
        val newRole =
            if (_state.value.currentRole == UserRole.STUDENT) {
                UserRole.TEACHER
            } else {
                UserRole.STUDENT
            }

        viewModelScope.launch {
            val refreshed = authRepository.refreshWithRole(newRole)
            if (!refreshed) {
                eventChannel.send(HomeEvent.NavigateToLogin)
            }
        }
    }

    private fun startObservingConnections() {
        connectionJob?.cancel()
        pollingJob?.cancel()

        _state.update { it.copy(nextPage = 1, endReached = false) }

        connectionJob = viewModelScope.launch {
            val role = _state.value.currentRole

            launch {
                connectionRepository.getConnectionsFlow(role).collect { connections ->
                    _state.update { currentState -> currentState.copy(connections = connections) }
                }
            }

            refreshFirstPage()
            startPolling()
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(50_000)
                refreshFirstPage()
            }
        }
    }

    fun onListScrolled(lastVisibleIndex: Int, totalCount: Int) {
        val state = _state.value
        if (state.isPaging || state.endReached || totalCount == 0) return

        val threshold = (state.pageSize / 2).coerceAtLeast(1)
        if (lastVisibleIndex >= totalCount - threshold) {
            loadNextPage()
        }
    }

    private fun refreshFirstPage() {
        viewModelScope.launch {
            // 1. Включаем загрузку и сбрасываем старую ошибку
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val result =
                connectionRepository.syncConnectionsPage(
                    _state.value.currentRole,
                    page = 0,
                    size = _state.value.pageSize,
                )

            // 2. Обрабатываем результат и выключаем загрузку в зависимости от исхода
            if (result.isSuccess) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        // Здесь также можно обновить список студентов, если они берутся из state
                        // students = result.getOrNull()?.content ?: emptyList()
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Не удалось обновить данные с сервера",
                    )
                }
            }
        }
    }

    private fun loadNextPage() {
        viewModelScope.launch {
            _state.update { it.copy(isPaging = true, errorMessage = null) }

            val result =
                connectionRepository.syncConnectionsPage(
                    _state.value.currentRole,
                    page = _state.value.nextPage,
                    size = _state.value.pageSize,
                )

            if (result.isSuccess) {
                val pageInfo = result.getOrNull()!!
                val endReached = pageInfo.pageNumber + 1 >= pageInfo.totalPages
                _state.update {
                    it.copy(nextPage = pageInfo.pageNumber + 1, endReached = endReached)
                }
            } else {
                _state.update { it.copy(errorMessage = "Не удалось загрузить следующую страницу") }
            }

            _state.update { it.copy(isPaging = false) }
        }
    }

    fun syncNetworkData() {
        refreshFirstPage()
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
        viewModelScope.launch { eventChannel.send(HomeEvent.NavigateToRoom(connectionId)) }
    }

    fun onLogoutClicked() {
        viewModelScope.launch {
            connectionRepository.clearAllConnections()
            authRepository.logout()
            eventChannel.send(HomeEvent.NavigateToLogin)
        }
    }
}

class HomeViewModelFactory(
    private val connectionRepository: ConnectionRepository,
    private val authRepository: AuthRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(connectionRepository, authRepository) as T
    }
}
