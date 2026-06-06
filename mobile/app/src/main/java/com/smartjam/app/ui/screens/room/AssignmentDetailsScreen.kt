package com.smartjam.app.ui.screens.room

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartjam.app.data.local.entity.SubmissionResultEntity
import com.smartjam.app.domain.model.UserRole
import com.smartjam.app.model.FeedbackEvent
import com.smartjam.app.ui.components.AppleLiquidBackground
import com.smartjam.app.ui.components.GlassContainer
import com.smartjam.app.ui.components.GoldenStringsButton
import com.smartjam.app.ui.components.AudioPlayerWithErrorTimeline
import com.smartjam.app.ui.theme.BrandCyan
import java.io.File
import java.util.UUID

@Composable
fun AssignmentDetailsScreen(
    assignmentId: UUID,
    role: UserRole,
    viewModel: RoomViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val assignment = state.assignments.firstOrNull { it.id == assignmentId }

    var pendingSavePath by remember { mutableStateOf<String?>(null) }

    val submissionPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = File(context.cacheDir, "temp_submission_upload.wav")
            context.contentResolver.openInputStream(it)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            viewModel.uploadSubmission(assignmentId, file)
        }
    }

    val saveToDeviceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/wav")
    ) { uri: Uri? ->
        val path = pendingSavePath
        if (uri != null && !path.isNullOrBlank()) {
            val input = File(path)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                input.inputStream().use { it.copyTo(output) }
            }
        }
        pendingSavePath = null
    }

    LaunchedEffect(assignmentId) {
        viewModel.onAssignmentExpanded(assignmentId)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF05050A))) {
        AppleLiquidBackground()

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp))
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (assignment == null) {
                Text("Урок не найден", color = Color.White)
                return@Column
            }

            GlassContainer {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(assignment.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Статус: ${assignment.status}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    assignment.description?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    }

                    val localPath = assignment.referenceAudioLocalPath
                    if (!localPath.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        GoldenStringsButton(
                            text = "Сохранить эталон",
                            onClick = {
                                pendingSavePath = localPath
                                saveToDeviceLauncher.launch("${assignment.title}.wav")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (role == UserRole.STUDENT) {
                        Spacer(modifier = Modifier.height(12.dp))
                        GoldenStringsButton(
                            text = "Скачать эталон",
                            onClick = {
                                viewModel.downloadReference(assignmentId) { path ->
                                    if (!path.isNullOrBlank()) {
                                        pendingSavePath = path
                                        saveToDeviceLauncher.launch("${assignment.title}.wav")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (role == UserRole.STUDENT) {
                        Spacer(modifier = Modifier.height(12.dp))
                        GoldenStringsButton(
                            text = "Загрузить попытку (.wav)",
                            onClick = { submissionPicker.launch("audio/*") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val submissions = state.submissionsByAssignment[assignmentId].orEmpty()
            Text(
                text = if (role == UserRole.TEACHER) "Попытки ученика" else "Мои попытки",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (submissions.isEmpty()) {
                    item {
                        Text("Пока нет попыток", color = Color.White.copy(alpha = 0.6f))
                    }
                } else {
                    items(submissions) { submission ->
                        SubmissionCard(
                            submission = submission,
                            feedback = state.feedbackBySubmission[submission.id].orEmpty(),
                            role = role,
                            onDownloadSubmission = { id, url ->
                                viewModel.downloadSubmissionAudio(id, assignmentId, url) { path ->
                                    if (!path.isNullOrBlank()) {
                                        pendingSavePath = path
                                        saveToDeviceLauncher.launch("${assignment.title}_${id}.wav")
                                    }
                                }
                            },
                            onSaveLocal = { path ->
                                pendingSavePath = path
                                saveToDeviceLauncher.launch("${assignment.title}_${submission.id}.wav")
                            },
                            onPrepareAudio = { id, url ->
                                viewModel.downloadSubmissionAudio(id, assignmentId, url) { }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmissionCard(
    submission: SubmissionResultEntity,
    feedback: List<FeedbackEvent>,
    role: UserRole,
    onDownloadSubmission: (UUID, String?) -> Unit,
    onSaveLocal: (String) -> Unit,
    onPrepareAudio: (UUID, String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var requestedAudio by remember(submission.id) { mutableStateOf(false) }

    GlassContainer {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Статус: ${submission.status}", color = Color.White)
                    val scoreText = submission.totalScore?.toString() ?: "N/A"
                    Text("Score: $scoreText", color = BrandCyan, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = Color.White
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Результаты анализа", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Total: ${submission.totalScore ?: 0f}%", color = Color.White.copy(alpha = 0.8f))
                Text("Pitch: ${submission.pitchScore ?: 0f}", color = Color.White.copy(alpha = 0.8f))
                Text("Rhythm: ${submission.rhythmScore ?: 0f}", color = Color.White.copy(alpha = 0.8f))

                if (role == UserRole.TEACHER) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!submission.submissionAudioLocalPath.isNullOrBlank()) {
                        GoldenStringsButton(
                            text = "Скачать запись ученика",
                            onClick = { onSaveLocal(submission.submissionAudioLocalPath) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (!submission.fileUrl.isNullOrBlank()) {
                        GoldenStringsButton(
                            text = "Скачать запись ученика",
                            onClick = { onDownloadSubmission(submission.id, submission.fileUrl) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                LaunchedEffect(expanded, submission.submissionAudioLocalPath, submission.fileUrl) {
                    if (expanded && !requestedAudio && submission.submissionAudioLocalPath.isNullOrBlank() && !submission.fileUrl.isNullOrBlank()) {
                        requestedAudio = true
                        onPrepareAudio(submission.id, submission.fileUrl)
                    }
                }

                val audioUri = when {
                    !submission.submissionAudioLocalPath.isNullOrBlank() -> {
                        val localFile = File(submission.submissionAudioLocalPath)
                        if (localFile.exists() && localFile.length() > 0L) {
                            Uri.fromFile(localFile)
                        } else {
                            null
                        }
                    }
                    !submission.fileUrl.isNullOrBlank() -> Uri.parse(submission.fileUrl)
                    else -> null
                }

                if (audioUri != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AudioPlayerWithErrorTimeline(
                        audioUri = audioUri,
                        feedback = feedback,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (!submission.fileUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Готовим аудио...", color = Color.White.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(8.dp))
                    GoldenStringsButton(
                        text = "Повторить загрузку",
                        onClick = {
                            requestedAudio = true
                            onPrepareAudio(submission.id, submission.fileUrl)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Аудио недоступно", color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}
