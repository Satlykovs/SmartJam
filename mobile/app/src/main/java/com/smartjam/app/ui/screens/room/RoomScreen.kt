package com.smartjam.app.ui.screens.room

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartjam.app.data.local.entity.AssignmentEntity
import com.smartjam.app.data.local.entity.SubmissionResultEntity
import com.smartjam.app.domain.model.UserRole
import com.smartjam.app.model.FeedbackEvent
import com.smartjam.app.model.FeedbackType
import com.smartjam.app.ui.components.AppleGlassTextField
import com.smartjam.app.ui.components.AppleLiquidBackground
import com.smartjam.app.ui.components.GlassContainer
import com.smartjam.app.ui.components.GoldenStringsButton
import com.smartjam.app.ui.theme.BrandCyan
import com.smartjam.app.ui.theme.BrandGold
import java.io.File
import java.util.UUID

@Composable
fun RoomScreen(connectionId: UUID, role: UserRole, viewModel: RoomViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var pendingAssignmentTitle by remember { mutableStateOf("") }
    var pendingAssignmentDescription by remember { mutableStateOf("") }
    var pendingSubmissionAssignmentId by remember { mutableStateOf<UUID?>(null) }
    var pendingSavePath by remember { mutableStateOf<String?>(null) }

    val assignmentPicker =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
            uri: Uri? ->
            uri?.let {
                val file = File(context.cacheDir, "temp_assignment_upload.wav")
                context.contentResolver.openInputStream(it)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                viewModel.uploadAssignment(
                    file,
                    pendingAssignmentTitle,
                    pendingAssignmentDescription.ifBlank { null },
                )
                pendingAssignmentTitle = ""
                pendingAssignmentDescription = ""
            }
        }

    val submissionPicker =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
            uri: Uri? ->
            val assignmentId =
                pendingSubmissionAssignmentId ?: return@rememberLauncherForActivityResult
            uri?.let {
                val file = File(context.cacheDir, "temp_submission_upload.wav")
                context.contentResolver.openInputStream(it)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                viewModel.uploadSubmission(assignmentId, file)
            }
        }

    val saveToDeviceLauncher =
        rememberLauncherForActivityResult(
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

    LaunchedEffect(listState) {
        snapshotFlow {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val total = listState.layoutInfo.totalItemsCount
                lastVisible to total
            }
            .collect { (lastVisible, total) -> viewModel.onListScrolled(lastVisible, total) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF05050A))) {
        AppleLiquidBackground()

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(
                modifier =
                    Modifier.height(
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp
                    )
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Room",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (role == UserRole.TEACHER) {
                GlassContainer {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Новый урок", color = BrandGold, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        AppleGlassTextField(
                            value = pendingAssignmentTitle,
                            onValueChange = { pendingAssignmentTitle = it },
                            hint = "Название урока",
                            icon = Icons.Default.Edit,
                            enabled = !state.isUploading,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AppleGlassTextField(
                            value = pendingAssignmentDescription,
                            onValueChange = { pendingAssignmentDescription = it },
                            hint = "Описание (опционально)",
                            icon = Icons.Default.Edit,
                            enabled = !state.isUploading,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        GoldenStringsButton(
                            text =
                                if (state.isUploading) "Загрузка..." else "Загрузить эталон (.wav)",
                            enabled = !state.isUploading && pendingAssignmentTitle.isNotBlank(),
                            onClick = { assignmentPicker.launch("audio/*") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (state.error != null) {
                Text(
                    state.error ?: "",
                    color = Color(0xFFFF5252),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.assignments) { assignment ->
                    AssignmentCard(
                        assignment = assignment,
                        role = role,
                        submissions = state.submissionsByAssignment[assignment.id].orEmpty(),
                        feedbackBySubmission = state.feedbackBySubmission,
                        onExpand = { viewModel.onAssignmentExpanded(assignment.id) },
                        onUploadSubmission = {
                            pendingSubmissionAssignmentId = assignment.id
                            submissionPicker.launch("audio/*")
                        },
                        onSaveAudio = { path ->
                            pendingSavePath = path
                            saveToDeviceLauncher.launch("${assignment.title}.wav")
                        },
                        onDownloadReference = { aId ->
                            viewModel.downloadReference(aId) { path ->
                                if (!path.isNullOrBlank()) {
                                    pendingSavePath = path
                                    saveToDeviceLauncher.launch("${assignment.title}.wav")
                                }
                            }
                        },
                        onDownloadSubmission = { submissionId, url ->
                            viewModel.downloadSubmissionAudio(submissionId, assignment.id, url) {
                                path: String? ->
                                if (!path.isNullOrBlank()) {
                                    pendingSavePath = path
                                    saveToDeviceLauncher.launch(
                                        "${assignment.title}_${submissionId}.wav"
                                    )
                                }
                            }
                        },
                        onSaveLocalSubmission = { path, submissionId ->
                            pendingSavePath = path
                            saveToDeviceLauncher.launch("${assignment.title}_${submissionId}.wav")
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AssignmentCard(
    assignment: AssignmentEntity,
    role: UserRole,
    submissions: List<SubmissionResultEntity>,
    feedbackBySubmission: Map<UUID, List<FeedbackEvent>>,
    onExpand: () -> Unit,
    onUploadSubmission: () -> Unit,
    onSaveAudio: (String) -> Unit,
    onDownloadReference: (UUID) -> Unit,
    onDownloadSubmission: (UUID, String?) -> Unit,
    onSaveLocalSubmission: (String, UUID) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    GlassContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        assignment.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Text(
                        "Статус: ${assignment.status}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                    )
                }
                IconButton(
                    onClick = {
                        expanded = !expanded
                        if (expanded) onExpand()
                    }
                ) {
                    Icon(
                        imageVector =
                            if (expanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = Color.White,
                    )
                }
            }

            if (expanded) {
                assignment.description?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }

                val localPath = assignment.referenceAudioLocalPath
                if (!localPath.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GoldenStringsButton(
                        text = "Сохранить на устройство",
                        onClick = { onSaveAudio(localPath) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (role == UserRole.STUDENT) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GoldenStringsButton(
                        text = "Скачать эталон",
                        onClick = { onDownloadReference(assignment.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (role == UserRole.STUDENT) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GoldenStringsButton(
                        text = "Загрузить попытку (.wav)",
                        onClick = onUploadSubmission,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (submissions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (role == UserRole.TEACHER) "Попытки ученика" else "Мои попытки",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    submissions.forEach { submission ->
                        SubmissionCard(
                            submission = submission,
                            feedback = feedbackBySubmission[submission.id].orEmpty(),
                            role = role,
                            onDownloadSubmission = { id, url -> onDownloadSubmission(id, url) },
                            onSaveLocal = { path -> onSaveLocalSubmission(path, submission.id) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
) {
    var expanded by remember { mutableStateOf(false) }

    GlassContainer {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Статус: ${submission.status}", color = Color.White)
                    val scoreText = submission.totalScore?.toString() ?: "N/A"
                    Text("Score: $scoreText", color = BrandCyan, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector =
                            if (expanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = Color.White,
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Результаты анализа", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Total: ${submission.totalScore ?: 0f}%",
                    color = Color.White.copy(alpha = 0.8f),
                )
                Text(
                    "Pitch: ${submission.pitchScore ?: 0f}",
                    color = Color.White.copy(alpha = 0.8f),
                )
                Text(
                    "Rhythm: ${submission.rhythmScore ?: 0f}",
                    color = Color.White.copy(alpha = 0.8f),
                )

                if (role == UserRole.TEACHER) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!submission.submissionAudioLocalPath.isNullOrBlank()) {
                        GoldenStringsButton(
                            text = "Скачать запись ученика",
                            onClick = { onSaveLocal(submission.submissionAudioLocalPath) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else if (!submission.fileUrl.isNullOrBlank()) {
                        GoldenStringsButton(
                            text = "Скачать запись ученика",
                            onClick = { onDownloadSubmission(submission.id, submission.fileUrl) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (feedback.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ErrorTimelineChart(feedback)
                }
            }
        }
    }
}

@Composable
private fun ErrorTimelineChart(feedback: List<FeedbackEvent>) {
    val maxEnd = feedback.maxOfOrNull { it.teacherEndTime } ?: 1.0
    val height = 40.dp

    GlassContainer {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("График ошибок", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
                val width = size.width
                feedback.forEach { event ->
                    val startX = (event.teacherStartTime / maxEnd).toFloat() * width
                    val endX = (event.teacherEndTime / maxEnd).toFloat() * width
                    val color =
                        when (event.type) {
                            FeedbackType.WRONG_NOTE -> Color(0xFFFF5252)
                            FeedbackType.WRONG_RHYTHM -> Color(0xFFFFD166)
                        }
                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(startX, 0f),
                        size =
                            androidx.compose.ui.geometry.Size(
                                (endX - startX).coerceAtLeast(2f),
                                size.height,
                            ),
                    )
                }
            }
        }
    }
}
