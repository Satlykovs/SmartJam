package com.smartjam.app.ui.screens.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartjam.app.data.local.entity.AssignmentEntity
import com.smartjam.app.data.local.entity.SubmissionResultEntity
import com.smartjam.app.domain.repository.RoomRepository
import com.smartjam.app.model.CreateAssignmentRequest
import com.smartjam.app.model.FeedbackEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

data class RoomUiState(
    val assignments: List<AssignmentEntity> = emptyList(),
    val submissionsByAssignment: Map<UUID, List<SubmissionResultEntity>> = emptyMap(),
    val feedbackBySubmission: Map<UUID, List<FeedbackEvent>> = emptyMap(),
    val isLoading: Boolean = false,
    val isPaging: Boolean = false,
    val endReached: Boolean = false,
    val nextPage: Int = 1,
    val pageSize: Int = 20,
    val isUploading: Boolean = false,
    val error: String? = null
)

class RoomViewModel(
    private val connectionId: UUID,
    private val repository: RoomRepository
) : ViewModel() {

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(RoomUiState())
    val uiState = _uiState.asStateFlow()

    private var submissionsJobs: MutableMap<UUID, Job> = mutableMapOf()

    init {
        observeAssignments()
        refreshFirstPage()
    }

    private fun observeAssignments() {
        viewModelScope.launch {
            repository.getAssignmentsFlow(connectionId).collect { assignments ->
                _uiState.update { it.copy(assignments = assignments) }
            }
        }
    }

    fun onListScrolled(lastVisibleIndex: Int, totalCount: Int) {
        val state = _uiState.value
        if (state.isPaging || state.endReached || totalCount == 0) return

        val threshold = (state.pageSize / 2).coerceAtLeast(1)
        if (lastVisibleIndex >= totalCount - threshold) {
            loadNextPage()
        }
    }

    fun refreshFirstPage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.syncAssignmentsPage(connectionId, page = 0, size = _uiState.value.pageSize)
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Не удалось обновить список уроков") }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadNextPage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPaging = true, error = null) }
            val result = repository.syncAssignmentsPage(
                connectionId,
                page = _uiState.value.nextPage,
                size = _uiState.value.pageSize
            )
            if (result.isSuccess) {
                val pageInfo = result.getOrNull()!!
                val endReached = pageInfo.pageNumber + 1 >= pageInfo.totalPages
                _uiState.update { it.copy(nextPage = pageInfo.pageNumber + 1, endReached = endReached) }
            } else {
                _uiState.update { it.copy(error = "Не удалось загрузить следующую страницу") }
            }
            _uiState.update { it.copy(isPaging = false) }
        }
    }

    fun onAssignmentExpanded(assignmentId: UUID) {
        viewModelScope.launch {
            repository.ensureAssignmentDetailsCached(assignmentId)
            repository.syncSubmissions(assignmentId)
            observeSubmissions(assignmentId)
        }
    }

    fun uploadAssignment(file: File, title: String, description: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null) }
            val request = CreateAssignmentRequest(connectionId, title, description)
            val result = repository.createAssignment(request)

            if (result.isSuccess) {
                val uploadInfo = result.getOrNull()!!
                val uploadResult = repository.uploadFileToS3(uploadInfo.uploadUrl.toString(), file)
                if (uploadResult.isSuccess) {
                    refreshFirstPage()
                } else {
                    _uiState.update { it.copy(error = "Upload failed") }
                }
            } else {
                _uiState.update { it.copy(error = "Creation failed") }
            }
            _uiState.update { it.copy(isUploading = false) }
        }
    }

    fun uploadSubmission(assignmentId: UUID, file: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null) }
            val result = repository.createSubmission(assignmentId)

            if (result.isSuccess) {
                val uploadInfo = result.getOrNull()!!
                val uploadResult = repository.uploadFileToS3(uploadInfo.uploadUrl.toString(), file)
                if (uploadResult.isSuccess) {
                    repository.syncSubmissions(assignmentId)
                    observeSubmissions(assignmentId)
                    startSubmissionPolling(uploadInfo.submissionId, assignmentId)
                } else {
                    _uiState.update { it.copy(error = "Upload failed") }
                }
            } else {
                _uiState.update { it.copy(error = "Submission creation failed") }
            }
            _uiState.update { it.copy(isUploading = false) }
        }
    }

    /**
     * Ensure reference audio for assignment is cached locally. Calls onResult with local path or null on failure.
     */
    fun downloadReference(assignmentId: UUID, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.ensureAssignmentDetailsCached(assignmentId)
            if (res.isSuccess) {
                onResult(res.getOrNull()?.referenceAudioLocalPath)
            } else {
                onResult(null)
            }
        }
    }

    fun downloadSubmissionAudio(submissionId: UUID, assignmentId: UUID, fileUrl: String?, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.cacheSubmissionAudioIfNeeded(submissionId, assignmentId, fileUrl)
            if (res.isSuccess) {
                onResult(res.getOrNull())
            } else {
                onResult(null)
            }
        }
    }

    private fun observeSubmissions(assignmentId: UUID) {
        submissionsJobs[assignmentId]?.cancel()
        submissionsJobs[assignmentId] = viewModelScope.launch {
            repository.getSubmissionsFlow(assignmentId).collect { submissions ->
                _uiState.update { state ->
                    state.copy(
                        submissionsByAssignment = state.submissionsByAssignment + (assignmentId to submissions)
                    )
                }
                submissions.forEach { submission ->
                    val hasFeedback = _uiState.value.feedbackBySubmission.containsKey(submission.id)
                    val needsDetailFetch = submission.pitchScore == null || submission.rhythmScore == null || !hasFeedback
                    if (needsDetailFetch) {
                        viewModelScope.launch {
                            val res = repository.getSubmissionResult(submission.id, assignmentId)
                            if (res.isSuccess) {
                                val dto = res.getOrNull()!!
                                val feedback = dto.feedback ?: emptyList()
                                _uiState.update { st ->
                                    st.copy(feedbackBySubmission = st.feedbackBySubmission + (submission.id to feedback))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startSubmissionPolling(submissionId: UUID, assignmentId: UUID) {
        viewModelScope.launch {
            repeat(30) {
                val result = repository.getSubmissionResult(submissionId, assignmentId)
                if (result.isSuccess) {
                    val dto = result.getOrNull()!!
                    val feedback = dto.feedback ?: emptyList()
                    _uiState.update { state ->
                        state.copy(feedbackBySubmission = state.feedbackBySubmission + (submissionId to feedback))
                    }
                    val status = dto.status.name
                    if (status == "COMPLETED" || status == "FAILED") {
                        return@launch
                    }
                }
                delay(2_000)
            }
        }
    }
}

class RoomViewModelFactory(
    private val connectionId: UUID,
    private val repository: RoomRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RoomViewModel(connectionId, repository) as T
    }
}
