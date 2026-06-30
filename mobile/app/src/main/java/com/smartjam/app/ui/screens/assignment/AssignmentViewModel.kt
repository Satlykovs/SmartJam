package com.smartjam.app.ui.screens.assignment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjam.app.data.local.entity.SubmissionResultEntity
import com.smartjam.app.domain.repository.ConnectionRepository
import com.smartjam.app.domain.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AssignmentUiState(
    val assignment: com.smartjam.app.data.local.entity.AssignmentEntity? = null,
    val peerName: String = "",
    val peerFirstName: String? = null,
    val peerLastName: String? = null,
    val peerAvatarUrl: String? = null,
    val submissions: List<SubmissionResultEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
)

@HiltViewModel
class AssignmentViewModel
@Inject
constructor(
    private val repository: RoomRepository,
    private val connectionRepository: ConnectionRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val assignmentId: UUID = UUID.fromString(checkNotNull(savedStateHandle["assignmentId"]))
    private val connectionId: UUID = UUID.fromString(checkNotNull(savedStateHandle["connectionId"]))

    private val _uiState = MutableStateFlow(AssignmentUiState())
    val uiState = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        loadAssignmentInfo()
        observePeerInfo()
        observeSubmissions()
        refreshSubmissions()
    }

    private fun loadAssignmentInfo() {
        viewModelScope.launch {
            val entity = repository.getAssignment(assignmentId)
            _uiState.update { it.copy(assignment = entity) }

            repository.ensureAssignmentDetailsCached(assignmentId).onSuccess { updated ->
                _uiState.update { it.copy(assignment = updated) }
            }
        }
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

    private fun observeSubmissions() {
        viewModelScope.launch {
            repository.getSubmissionsFlow(assignmentId).collect { list ->
                _uiState.update { it.copy(submissions = list) }
                checkAndStartPolling(list)
            }
        }
    }

    fun refreshSubmissions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.syncSubmissions(assignmentId)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun checkAndStartPolling(subs: List<SubmissionResultEntity>) {
        val hasActive = subs.any {
            it.status == "AWAITING_UPLOAD" || it.status == "ANALYZING" || it.status == "UPLOADED"
        }
        if (hasActive && pollingJob?.isActive != true) {
            startPolling()
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(3000L)
                repository.syncSubmissions(assignmentId)
                val current = _uiState.value.submissions
                if (current.none { it.status == "ANALYZING" || it.status == "UPLOADED" }) break
            }
        }
    }

    fun uploadSubmission(file: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            repository.createSubmission(assignmentId).onSuccess { info ->
                repository.uploadFileToS3(info.uploadUrl.toString(), file)
                refreshSubmissions()
            }
            _uiState.update { it.copy(isUploading = false) }
        }
    }

    fun downloadReference(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.ensureAssignmentDetailsCached(assignmentId)
            onResult(res.getOrNull()?.referenceAudioLocalPath)
        }
    }
}
