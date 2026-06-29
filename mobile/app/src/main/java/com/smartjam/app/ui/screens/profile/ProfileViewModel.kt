package com.smartjam.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjam.app.domain.repository.AuthRepository
import com.smartjam.app.domain.repository.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class ProfileEvent {
    object NavigateToLogin : ProfileEvent()
}

@HiltViewModel
class ProfileViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val connectionRepository: ConnectionRepository,
) : ViewModel() {

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onLogoutClicked() {
        viewModelScope.launch {
            connectionRepository.clearAllConnections()
            authRepository.logout()
            _events.send(ProfileEvent.NavigateToLogin)
        }
    }
}
