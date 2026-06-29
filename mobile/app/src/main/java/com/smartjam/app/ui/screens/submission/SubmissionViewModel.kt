package com.smartjam.app.ui.screens.submission

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.smartjam.app.data.player.SmartJamPlayer
import com.smartjam.app.domain.repository.RoomRepository
import com.smartjam.app.model.SubmissionResultResponse
import com.smartjam.app.ui.screens.assignment.formatSubmissionDate
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SubmissionViewModel
@Inject
constructor(
    private val repository: RoomRepository,
    private val savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
    private val callFactory: okhttp3.Call.Factory,
    private val exoPlayer: ExoPlayer,
) : ViewModel() {

    private val submissionId: UUID = UUID.fromString(checkNotNull(savedStateHandle["submissionId"]))
    private val assignmentId: UUID = UUID.fromString(checkNotNull(savedStateHandle["assignmentId"]))

    private val _uiState = MutableStateFlow(SubmissionDetailUiState())
    val uiState = _uiState.asStateFlow()

    var player by mutableStateOf<SmartJamPlayer?>(null)
        private set

    init {
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val assignment = repository.getAssignment(assignmentId)
            val submissionEntity = repository.getSubmissionEntity(assignmentId, submissionId)

            _uiState.update {
                it.copy(
                    assignmentTitle = assignment?.title ?: "Анализ",
                    createdAt = submissionEntity?.createdAt,
                )
            }

            val result = repository.getSubmissionResult(submissionId, assignmentId)
            result.onSuccess { dto ->
                _uiState.update { it.copy(submission = dto, isLoading = false) }

                val localPath =
                    repository
                        .cacheSubmissionAudioIfNeeded(
                            submissionId,
                            assignmentId,
                            dto.submissionAudioUrl?.toString(),
                        )
                        .getOrNull()

                if (localPath != null) {
                    initPlayer(localPath)
                }
            }
        }
    }

    private fun initPlayer(path: String) {
        val state = _uiState.value
        val title = state.assignmentTitle
        val dateText = formatSubmissionDate(state.createdAt)

        player =
            SmartJamPlayer(context, callFactory, exoPlayer).apply {
                prepare(
                    uri = Uri.fromFile(File(path)),
                    title = title,
                    subtitle = "попытка от $dateText",
                )
            }
    }

    override fun onCleared() {
        player?.pause()
        player?.release()
        super.onCleared()
    }
}

data class SubmissionDetailUiState(
    val submission: SubmissionResultResponse? = null,
    val isLoading: Boolean = false,
    val createdAt: java.time.Instant? = null,
    val assignmentTitle: String = "Загрузка...",
)
