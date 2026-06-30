package com.smartjam.app.ui.screens.profile

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjam.app.domain.repository.AuthRepository
import com.smartjam.app.domain.repository.ConnectionRepository
import com.smartjam.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val musicalTitle: String = "",
    val selectedImageUri: Uri? = null,
    val error: String? = null,
    val isSuccess: Boolean = false,
)

sealed class ProfileEvent {
    object NavigateToLogin : ProfileEvent()
}

@HiltViewModel
class ProfileViewModel
@Inject
constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val connectionRepository: ConnectionRepository,
) : ViewModel() {

    var state by mutableStateOf(ProfileUiState())
        private set

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val titles =
        listOf(
            "Любитель нулей",
            "Мастер соло",
            "Струнодёр",
            "Король баррэ",
            "Властелин медиатора",
            "Гитарист-теоретик",
            "Покоритель пентатоники",
            "Куртка Бейн",
            "Сыграл Snow без ошибок",
            "В поисках медиатора",
            "Сломал первую струну",
            "Вечно настраиваюсь",
            "Соседи вешайтесь",
            "Педальный мастер",
            "Метроном для слабых",
        )

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            val result = userRepository.getProfile()
            if (result.isSuccessful) {
                val user = result.body()
                state =
                    state.copy(
                        username = user?.username ?: "",
                        firstName = user?.firstName ?: "",
                        lastName = user?.lastName ?: "",
                        email = user?.email ?: "",
                        avatarUrl = user?.avatarUrl?.toString(),
                        musicalTitle = titles.random(),
                        isLoading = false,
                    )
            } else {
                state = state.copy(isLoading = false, error = "Не удалось загрузить данные")
            }
        }
    }

    fun setEditing(v: Boolean) {
        state = state.copy(isEditing = v)
    }

    fun onUsernameChange(v: String) {
        state = state.copy(username = v)
    }

    fun onFirstNameChange(v: String) {
        state = state.copy(firstName = v)
    }

    fun onLastNameChange(v: String) {
        state = state.copy(lastName = v)
    }

    fun onImageSelected(uri: Uri?) {
        state = state.copy(selectedImageUri = uri)
    }

    fun saveProfile() {
        viewModelScope.launch {
            val oldAvatarUrl = state.avatarUrl
            val wasImageSelected = state.selectedImageUri != null

            state = state.copy(isSaving = true, error = null)

            val result =
                userRepository.updateProfile(
                    username = state.username,
                    firstName = state.firstName.ifBlank { null },
                    lastName = state.lastName.ifBlank { null },
                    newAvatarUri = state.selectedImageUri,
                )

            if (result.isSuccess) {
                state = state.copy(isSaving = false, isSuccess = true, isEditing = false)

                if (wasImageSelected) {
                    startAvatarSync(oldAvatarUrl)
                } else {
                    loadProfile()
                }

                delay(2000)
                state = state.copy(isSuccess = false, selectedImageUri = null)
            } else {
                state = state.copy(isSaving = false, error = "Ошибка сохранения")
            }
        }
    }

    private fun startAvatarSync(oldUrl: String?) {
        viewModelScope.launch {
            var isSynced = false

            repeat(6) { attempt ->
                delay(2500)

                val result = userRepository.getProfile()
                if (result.isSuccessful && result.body() != null) {
                    val user = result.body()!!
                    val currentServerUrl = user.avatarUrl?.toString()

                    if (currentServerUrl != oldUrl) {
                        state =
                            state.copy(
                                username = user.username,
                                firstName = user.firstName ?: "",
                                lastName = user.lastName ?: "",
                                email = user.email,
                                avatarUrl = currentServerUrl,
                                isLoading = false,
                            )
                        isSynced = true
                        android.util.Log.d("SmartJam_Profile", "Avatar synced on attempt $attempt")
                        return@launch
                    }
                }
            }

            if (!isSynced) {
                loadProfile()
            }
        }
    }

    fun onLogoutClicked() {
        viewModelScope.launch {
            connectionRepository.clearAllConnections()
            authRepository.logout()
            _events.send(ProfileEvent.NavigateToLogin)
        }
    }
}
