package com.smartjam.app.ui.screens.room

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjam.app.data.local.entity.AssignmentEntity
import com.smartjam.app.data.local.entity.SubmissionResultEntity
import com.smartjam.app.domain.repository.ConnectionRepository
import com.smartjam.app.domain.repository.RoomRepository
import com.smartjam.app.model.CreateAssignmentRequest
import com.smartjam.app.model.FeedbackEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import java.io.File
import java.util.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RoomUiState(
    val peerName: String = "Загрузка...",
    val assignments: List<AssignmentEntity> = emptyList(),
    val submissionsByAssignment: Map<UUID, List<SubmissionResultEntity>> = emptyMap(),
    val feedbackBySubmission: Map<UUID, List<FeedbackEvent>> = emptyMap(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class RoomViewModel
@Inject
constructor(
    private val repository: RoomRepository,
    private val connectionRepository: ConnectionRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val connectionId: UUID = UUID.fromString(checkNotNull(savedStateHandle["connectionId"]))
    private val _uiState = MutableStateFlow(RoomUiState())
    val uiState = _uiState.asStateFlow()

    private val submissionPollingJobs = mutableMapOf<UUID, Job>()
    private var isSyncingAssignments = false

    val roomTicker = flow {
        while (true) {
            emit(Unit)
            delay(15_000L)
        }
    }

    init {
        loadPeerName()
        observeAssignments()
    }

    private fun loadPeerName() {
        viewModelScope.launch {
            val name = connectionRepository.getPeerName(connectionId)
            _uiState.update { it.copy(peerName = name ?: "Комната") }
        }
    }

    private fun observeAssignments() {
        viewModelScope.launch {
            repository.getAssignmentsFlow(connectionId).collect { list ->
                _uiState.update { it.copy(assignments = list) }
            }
        }
    }

    fun observeSubmissionsForAssignment(assignmentId: UUID) {
        if (submissionPollingJobs.containsKey(assignmentId)) return

        submissionPollingJobs[assignmentId] = viewModelScope.launch {
            repository.getSubmissionsFlow(assignmentId).collect { subs ->
                _uiState.update { state ->
                    val newMap = state.submissionsByAssignment.toMutableMap()
                    newMap[assignmentId] = subs
                    state.copy(submissionsByAssignment = newMap)
                }
            }
        }
    }

    fun onAssignmentExpanded(assignmentId: UUID) {
        viewModelScope.launch {
            observeSubmissionsForAssignment(assignmentId)

            repository.ensureAssignmentDetailsCached(assignmentId)
            repository.syncSubmissions(assignmentId).onSuccess {
                _uiState.value.submissionsByAssignment[assignmentId]?.forEach { sub ->
                    if (sub.status == "ANALYZING" || sub.status == "UPLOADED") {
                        startSubmissionPolling(sub.id, assignmentId)
                    }
                }
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

    private fun syncSubmissionsFor(assignmentId: UUID) {
        viewModelScope.launch {
            repository.syncSubmissions(assignmentId).onSuccess {
                repository.getSubmissionsFlow(assignmentId).first().forEach { sub ->
                    if (sub.status == "ANALYZING" || sub.status == "UPLOADED") {
                        startSubmissionPolling(sub.id, assignmentId)
                    }
                }
            }
        }
    }

    private fun startSubmissionPolling(submissionId: UUID, assignmentId: UUID) {
        if (submissionPollingJobs[submissionId]?.isActive == true) return
        submissionPollingJobs[submissionId] = viewModelScope.launch {
            var currentDelay = 2_000L
            while (isActive) {
                val res = repository.getSubmissionResult(submissionId, assignmentId)
                if (res.isSuccess) {
                    val status = res.getOrNull()!!.status.name
                    if (status == "COMPLETED" || status == "FAILED") break
                }
                delay(currentDelay)
                currentDelay = (currentDelay * 1.5).toLong().coerceAtMost(10000L)
            }
        }
    }

    fun downloadReference(assignmentId: UUID, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.ensureAssignmentDetailsCached(assignmentId)
            onResult(res.getOrNull()?.referenceAudioLocalPath)
        }
    }

    fun downloadSubmissionAudio(
        submissionId: UUID,
        assignmentId: UUID,
        fileUrl: String?,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val res = repository.cacheSubmissionAudioIfNeeded(submissionId, assignmentId, fileUrl)
            onResult(res.getOrNull())
        }
    }

    fun uploadAssignment(file: File, title: String, description: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            // ТУТ ОПИСАНИЕ ПЕРЕДАЕТСЯ КОРРЕКТНО
            repository
                .createAssignment(CreateAssignmentRequest(connectionId, title, description))
                .onSuccess { info ->
                    repository.uploadFileToS3(info.uploadUrl.toString(), file)
                    refreshRoomData()
                }
            _uiState.update { it.copy(isUploading = false) }
        }
    }

    fun uploadSubmission(assignmentId: UUID, file: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            repository.createSubmission(assignmentId).onSuccess { info ->
                repository.uploadFileToS3(info.uploadUrl.toString(), file)
                startSubmissionPolling(info.submissionId, assignmentId)
            }
            _uiState.update { it.copy(isUploading = false) }
        }
    }

    fun loadFullSubmissionDetail(submissionId: UUID, assignmentId: UUID) {

        observeSubmissionsForAssignment(assignmentId)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = repository.getSubmissionResult(submissionId, assignmentId)

            if (result.isSuccess) {
                val dto = result.getOrNull()!!

                if (dto.status.name == "ANALYZING" || dto.status.name == "UPLOADED") {
                    startSubmissionPolling(submissionId, assignmentId)
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
