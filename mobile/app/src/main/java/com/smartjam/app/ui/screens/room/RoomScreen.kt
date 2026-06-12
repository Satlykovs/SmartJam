package com.smartjam.app.ui.screens.room

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.smartjam.app.data.local.entity.AssignmentEntity
import com.smartjam.app.domain.model.UserRole
import com.smartjam.app.ui.components.*
import com.smartjam.app.ui.theme.*
import java.io.File
import java.util.UUID

@Composable
fun RoomScreen(
    connectionId: UUID,
    role: UserRole,
    viewModel: RoomViewModel,
    onBack: () -> Unit,
    onOpenAssignment: (UUID) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.roomTicker.collect { viewModel.refreshRoomData() }
        }
    }

    var pendingTitle by remember { mutableStateOf("") }
    var pendingDesc by remember { mutableStateOf("") } // ВЕРНУЛИ ОПИСАНИЕ
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    val audioPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
            selectedFileUri = it
        }

    Box(Modifier.fillMaxSize().background(CoreBackground)) {
        AppleLiquidBackground()

        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(
                Modifier.height(
                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = state.peerName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            ) {
                if (role == UserRole.TEACHER) {
                    item {
                        Text("Новое задание", color = BrandGold, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        GlassContainer {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                AppleGlassTextField(
                                    pendingTitle,
                                    { pendingTitle = it },
                                    "Название урока",
                                    Icons.Default.Edit,
                                    enabled = !state.isUploading,
                                )

                                // ВЕРНУЛИ ПОЛЕ ОПИСАНИЯ В UI
                                AppleGlassTextField(
                                    pendingDesc,
                                    { pendingDesc = it },
                                    "Описание (необязательно)",
                                    Icons.Default.Edit,
                                    enabled = !state.isUploading,
                                )

                                GoldenStringsButton(
                                    text =
                                        if (selectedFileUri == null) "Выбрать аудиофайл"
                                        else "Файл готов ✓",
                                    onClick = { audioPicker.launch("audio/*") },
                                    enabled = !state.isUploading,
                                )

                                if (selectedFileUri != null && pendingTitle.isNotBlank()) {
                                    Button(
                                        onClick = {
                                            val file = File(context.cacheDir, "temp_up")
                                            context.contentResolver
                                                .openInputStream(selectedFileUri!!)
                                                ?.use { input ->
                                                    file.outputStream().use { input.copyTo(it) }
                                                }
                                            // ПЕРЕДАЕМ И ТИТУЛ И ОПИСАНИЕ
                                            viewModel.uploadAssignment(
                                                file,
                                                pendingTitle,
                                                pendingDesc.ifBlank { null },
                                            )
                                            pendingTitle = ""
                                            pendingDesc = ""
                                            selectedFileUri = null
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        colors =
                                            ButtonDefaults.buttonColors(containerColor = BrandCyan),
                                        enabled = !state.isUploading,
                                    ) {
                                        Text("Опубликовать урок", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Уроки", color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                }

                val displayList =
                    if (role == UserRole.STUDENT) state.assignments.filter { it.status != "FAILED" }
                    else state.assignments

                if (displayList.isEmpty() && !state.isLoading) {
                    item {
                        val msg =
                            if (role == UserRole.TEACHER) "У вас нет уроков" else "Уроков пока нет"
                        Text(msg, color = Color.White.copy(0.4f), fontSize = 14.sp)
                    }
                }

                items(displayList) { assignment ->
                    AssignmentListCard(assignment) { onOpenAssignment(assignment.id) }
                }
            }
        }
    }
}

@Composable
private fun AssignmentListCard(assignment: AssignmentEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (assignment.status == "FAILED") ErrorRed.copy(0.1f)
                    else Color.White.copy(0.06f)
            ),
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    assignment.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                if (assignment.status != "COMPLETED") {
                    val color = if (assignment.status == "FAILED") ErrorRed else BrandCyan
                    Text(text = "Статус: ${assignment.status}", color = color, fontSize = 12.sp)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White.copy(0.4f))
        }
    }
}
