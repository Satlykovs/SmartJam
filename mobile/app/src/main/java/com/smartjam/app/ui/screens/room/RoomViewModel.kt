package com.smartjam.app.ui.screens.room

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjam.app.data.local.entity.AssignmentEntity
import com.smartjam.app.domain.repository.AuthRepository
import com.smartjam.app.domain.repository.ConnectionRepository
import com.smartjam.app.domain.repository.RoomRepository
import com.smartjam.app.model.CreateAssignmentRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import java.io.File
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class RoomViewModel
@Inject
constructor(
    private val repository: RoomRepository,
    private val connectionRepository: ConnectionRepository,
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val connectionId: UUID = UUID.fromString(checkNotNull(savedStateHandle["connectionId"]))

    private val _uiState = MutableStateFlow(RoomUiState())
    val uiState = _uiState.asStateFlow()

    private var isSyncingAssignments = false

    val roomTicker = flow {
        while (true) {
            emit(Unit)
            delay(15_000L)
        }
    }

    init {
        observePeerInfo()
        observeAssignments()
    }

    private fun observePeerInfo() {
        repository
            .getConnectionFlow(connectionId)
            .onEach { entity ->
                if (entity != null) {
                    _uiState.update {
                        it.copy(
                            peerName = entity.peerUsername,
                            peerFirstName = entity.peerFirstName,
                            peerLastName = entity.peerLastName,
                            peerAvatarUrl = entity.peerAvatarUrl,
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeAssignments() {
        viewModelScope.launch {
            repository.getAssignmentsFlow(connectionId).collect { list ->
                _uiState.update { it.copy(assignments = list) }
            }
        }
    }

    fun refreshRoomData() {
        if (isSyncingAssignments) return
        viewModelScope.launch {
            isSyncingAssignments = true
            try {
                repository.syncAssignmentsPage(connectionId, 0, 20)
            } finally {
                isSyncingAssignments = false
            }
        }
    }

    fun uploadAssignment(file: File, title: String, description: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            repository
                .createAssignment(CreateAssignmentRequest(connectionId, title, description))
                .onSuccess { info ->
                    repository.uploadFileToS3(info.uploadUrl.toString(), file)
                    refreshRoomData()
                }
                .onFailure { info -> Log.e("SmartJam_Assignment_room", info.message.toString()) }
            _uiState.update { it.copy(isUploading = false) }
        }
    }
}

data class RoomUiState(
    val peerName: String = "Загрузка...",
    val peerFirstName: String? = null,
    val peerLastName: String? = null,
    val peerAvatarUrl: String? = null,
    val assignments: List<AssignmentEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
)
