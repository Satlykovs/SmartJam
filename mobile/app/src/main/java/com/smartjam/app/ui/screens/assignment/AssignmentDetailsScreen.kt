package com.smartjam.app.ui.screens.assignment

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.smartjam.app.data.local.entity.SubmissionResultEntity
import com.smartjam.app.model.UserRole
import com.smartjam.app.ui.components.*
import com.smartjam.app.ui.navigation.Screen
import com.smartjam.app.ui.theme.BrandCyan
import com.smartjam.app.ui.theme.CoreBackground
import java.io.File
import java.util.UUID

@Composable
fun AssignmentDetailsScreen(
    connectionId: UUID,
    role: UserRole,
    viewModel: AssignmentViewModel,
    navController: NavHostController,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val assignment = state.assignment

    var pendingSavePath by remember { mutableStateOf<String?>(null) }

    val submissionPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val file = File(context.cacheDir, "temp_sub")
                context.contentResolver.openInputStream(it)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                viewModel.uploadSubmission(file)
            }
        }

    val saveLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri
            ->
            if (uri != null && pendingSavePath != null) {
                File(pendingSavePath!!).inputStream().use { input ->
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        input.copyTo(output)
                    }
                }
            }
            pendingSavePath = null
        }

    Box(modifier = Modifier.fillMaxSize().background(CoreBackground)) {
        AppleLiquidBackground()

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(
                modifier =
                    Modifier.height(
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp
                    )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White)
                }

                Spacer(Modifier.width(4.dp))

                SmartJamAvatar(url = state.peerAvatarUrl, size = 42.dp)

                Spacer(Modifier.width(12.dp))

                UserIdentityBlock(
                    firstName = state.peerFirstName,
                    lastName = state.peerLastName,
                    username = state.peerName,
                    isCyanUsername = true,
                )
            }

            if (assignment == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
                return@Column
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = assignment.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            ) {
                item {
                    Column {
                        Text(
                            text = assignment.description ?: "Описание отсутствует",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppleGlassButton(
                                text = "Скачать эталон",
                                onClick = {
                                    viewModel.downloadReference { path ->
                                        if (path != null) {
                                            pendingSavePath = path
                                            saveLauncher.launch("${assignment.title}.wav")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )

                            if (role == UserRole.STUDENT) {
                                GoldenStringsButton(
                                    text = "Сдать попытку",
                                    onClick = { submissionPicker.launch("audio/*") },
                                    modifier = Modifier.weight(1.2f).height(60.dp),
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = if (role == UserRole.TEACHER) "Попытки ученика" else "Мои попытки",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }

                if (state.submissions.isEmpty() && !state.isLoading) {
                    item { Text("Попыток пока нет", color = Color.White.copy(alpha = 0.4f)) }
                }

                items(state.submissions) { submission ->
                    SubmissionRow(
                        submission = submission,
                        onClick = {
                            navController.navigate(
                                Screen.SubmissionDetail.createRoute(
                                    connectionId = connectionId.toString(),
                                    assignmentId = assignment.id.toString(),
                                    submissionId = submission.id.toString(),
                                )
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SubmissionRow(submission: SubmissionResultEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text =
                        when (submission.status) {
                            "COMPLETED" -> "Анализ готов"
                            "FAILED" -> "Ошибка обработки"
                            else -> "В обработке..."
                        },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )

                Text(
                    text = formatSubmissionDate(submission.createdAt),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )

                if (submission.totalScore != null && submission.status == "COMPLETED") {
                    Text(
                        "Точность: ${submission.totalScore.toInt()}%",
                        color = BrandCyan,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White.copy(0.4f))
        }
    }
}

fun formatSubmissionDate(instant: java.time.Instant?): String {
    if (instant == null) return ""
    val formatter =
        java.time.format.DateTimeFormatter.ofPattern(
                "d MMMM, HH:mm",
                java.util.Locale.forLanguageTag("ru"),
            )
            .withZone(java.time.ZoneId.systemDefault())
    return formatter.format(instant)
}
