package com.smartjam.app.ui.screens.submission

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartjam.app.data.player.SmartJamPlayer
import com.smartjam.app.di.PlayerEntryPoint
import com.smartjam.app.ui.components.*
import com.smartjam.app.ui.screens.room.RoomViewModel
import com.smartjam.app.ui.theme.*
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import java.util.UUID

@Composable
fun SubmissionDetailScreen(
    submissionId: UUID,
    assignmentId: UUID,
    viewModel: RoomViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val submission = state.submissionsByAssignment[assignmentId]?.find { it.id == submissionId }
    val feedback = submission?.analysisFeedback.orEmpty() // КРИТИЧНО: Берем из объекта БД

    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PlayerEntryPoint::class.java,
        )
    }

    val audioUri =
        remember(submission) {
            val localPath = submission?.submissionAudioLocalPath
            if (!localPath.isNullOrBlank() && File(localPath).exists())
                Uri.fromFile(File(localPath))
            // ИСПРАВЛЕНО: Uri.parse вместо toUri()
            else submission?.fileUrl?.let { Uri.parse(it) }
        }

    val player =
        remember(audioUri) {
            Log.d("SmartJam_PLAYER", audioUri.toString())
            SmartJamPlayer(context, entryPoint.callFactory()).apply {
                audioUri?.let { prepare(it) }
            }
        }
    DisposableEffect(Unit) { onDispose { player.release() } }

    val currentPos by player.currentPosition.collectAsState()
    val durationMs by player.duration.collectAsState()

    LaunchedEffect(submissionId) { viewModel.loadFullSubmissionDetail(submissionId, assignmentId) }

    Box(modifier = Modifier.fillMaxSize().background(CoreBackground)) {
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Анализ попытки",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Статус: ${submission?.status ?: "..."}",
                        color = BrandCyan,
                        fontSize = 12.sp,
                    )
                }
            }

            if (submission == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
                return@Column
            }

            Spacer(modifier = Modifier.height(32.dp))

            WaveformCompare(
                teacherRms = submission.teacherWaveform ?: emptyList(),
                studentRms = submission.studentWaveform ?: emptyList(),
                feedback = feedback,
                currentPositionMs = currentPos,
                durationMs = durationMs,
                onSeek = { player.seekTo(it) },
                modifier = Modifier.height(180.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ScoreCard("Балл", submission.totalScore?.toInt() ?: 0, BrandCyan)
                ScoreCard("Ноты", submission.pitchScore?.toInt() ?: 0, BrandGold)
                ScoreCard("Ритм", submission.rhythmScore?.toInt() ?: 0, Color.White.copy(0.7f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            AudioPlayerWithErrorTimeline(player, feedback, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ScoreCard(label: String, score: Int, color: Color) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        Text("$score%", color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}
