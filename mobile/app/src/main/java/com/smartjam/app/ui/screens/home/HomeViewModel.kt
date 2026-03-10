package com.smartjam.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartjam.app.domain.repository.RoomRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val inviteCodeInput: String = "",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

sealed class HomeEvent {
    object RoomJoined : HomeEvent()
}

class HomeViewModel(
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<HomeEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onInviteCodeChanged(code: String) {
        _state.update { it.copy(inviteCodeInput = code.trim(), errorMessage = null, successMessage = null) }
    }

    fun onJoinRoomClicked() {
        val code = _state.value.inviteCodeInput
        if (code.isBlank()) {
            _state.update { it.copy(errorMessage = "Введите код") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            val result = roomRepository.joinRoomByCode(code)

            _state.update { it.copy(isLoading = false) }

            if (result.isSuccess) {
                val room = result.getOrNull()
                _state.update {
                    it.copy(
                        successMessage = "Вы присоединились к классу: ${room?.teacherName}",
                        inviteCodeInput = ""
                    )
                }
                eventChannel.send(HomeEvent.RoomJoined)
            } else {
                _state.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }
}

class HomeViewModelFactory(
    private val roomRepository: RoomRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(roomRepository) as T
    }
}