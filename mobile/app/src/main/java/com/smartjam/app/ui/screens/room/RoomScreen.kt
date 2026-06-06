package com.smartjam.app.ui.screens.room

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
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
import com.smartjam.app.domain.model.UserRole
import com.smartjam.app.ui.components.AppleGlassTextField
import com.smartjam.app.ui.components.AppleLiquidBackground
import com.smartjam.app.ui.components.GlassContainer
import com.smartjam.app.ui.components.GoldenStringsButton
import com.smartjam.app.ui.theme.BrandGold
import java.io.File
import java.util.UUID

@Composable
fun RoomScreen(
    connectionId: UUID,
    role: UserRole,
    viewModel: RoomViewModel,
    onBack: () -> Unit,
    onOpenAssignment: (UUID) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var pendingAssignmentTitle by remember { mutableStateOf("") }
    var pendingAssignmentDescription by remember { mutableStateOf("") }
    var pendingSavePath by remember { mutableStateOf<String?>(null) }

    val assignmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = File(context.cacheDir, "temp_assignment_upload.wav")
            context.contentResolver.openInputStream(it)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            viewModel.uploadAssignment(file, pendingAssignmentTitle, pendingAssignmentDescription.ifBlank { null })
            pendingAssignmentTitle = ""
            pendingAssignmentDescription = ""
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

    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible to total
        }.collect { (lastVisible, total) ->
            viewModel.onListScrolled(lastVisible, total)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF05050A))) {
        AppleLiquidBackground()

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Room",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
                            enabled = !state.isUploading
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AppleGlassTextField(
                            value = pendingAssignmentDescription,
                            onValueChange = { pendingAssignmentDescription = it },
                            hint = "Описание (опционально)",
                            icon = Icons.Default.Edit,
                            enabled = !state.isUploading
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        GoldenStringsButton(
                            text = if (state.isUploading) "Загрузка..." else "Загрузить эталон (.wav)",
                            enabled = !state.isUploading && pendingAssignmentTitle.isNotBlank(),
                            onClick = { assignmentPicker.launch("audio/*") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (state.error != null) {
                Text(state.error ?: "", color = Color(0xFFFF5252), modifier = Modifier.padding(vertical = 8.dp))
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.assignments) { assignment ->
                    AssignmentCard(
                        assignment = assignment,
                        role = role,
                        onOpenAssignment = { onOpenAssignment(assignment.id) },
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
                        }
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
    onOpenAssignment: () -> Unit,
    onSaveAudio: (String) -> Unit,
    onDownloadReference: (UUID) -> Unit
) {
    GlassContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(assignment.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Статус: ${assignment.status}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                IconButton(onClick = onOpenAssignment) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open",
                        tint = Color.White
                    )
                }
            }

            assignment.description?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }

            val localPath = assignment.referenceAudioLocalPath
            if (!localPath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                GoldenStringsButton(
                    text = "Сохранить эталон",
                    onClick = { onSaveAudio(localPath) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (role == UserRole.STUDENT) {
                Spacer(modifier = Modifier.height(12.dp))
                GoldenStringsButton(
                    text = "Скачать эталон",
                    onClick = { onDownloadReference(assignment.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (role == UserRole.STUDENT) {
                Spacer(modifier = Modifier.height(12.dp))
                GoldenStringsButton(
                    text = "Открыть урок",
                    onClick = onOpenAssignment,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
