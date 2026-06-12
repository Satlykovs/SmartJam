package com.smartjam.app.ui.screens.submission

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
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
    val feedback = submission?.analysisFeedback.orEmpty()

    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PlayerEntryPoint::class.java,
        )
    }

    var activeErrors by remember {
        mutableStateOf<List<com.smartjam.app.model.FeedbackEvent>>(emptyList())
    }

    val localPath = submission?.submissionAudioLocalPath
    val isFileReady =
        remember(localPath) {
            !localPath.isNullOrBlank() && File(localPath).exists() && File(localPath).length() > 0
        }

    val player =
        remember(isFileReady, localPath) {
            if (isFileReady && localPath != null) {
                SmartJamPlayer(context, entryPoint.callFactory()).apply {
                    prepare(Uri.fromFile(File(localPath)))
                }
            } else null
        }

    var activeScrubPosition by remember { mutableStateOf<Long?>(null) }

    val currentPosRaw = player?.currentPosition?.collectAsState()?.value ?: 0L
    val durationMs = player?.duration?.collectAsState()?.value ?: 0L

    val currentPos = activeScrubPosition ?: currentPosRaw

    DisposableEffect(player) {
        onDispose {
            player?.pause()
            player?.release()
        }
    }

    LaunchedEffect(submissionId) { viewModel.loadFullSubmissionDetail(submissionId, assignmentId) }

    Box(Modifier.fillMaxSize().background(CoreBackground)) {
        AppleLiquidBackground()

        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(
                Modifier.height(
                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
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

            if (submission == null || !isFileReady) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
                return@Column
            }

            Spacer(Modifier.height(24.dp))

            WaveformCompare(
                teacherRms = submission.teacherWaveform ?: emptyList(),
                studentRms = submission.studentWaveform ?: emptyList(),
                feedback = feedback,
                currentPositionMs = currentPos,
                durationMs = durationMs,
                onSeek = { player?.seekTo(it) },
                onScrubbing = { positionMs -> activeScrubPosition = positionMs },
                onScrubbingFinished = { finalPositionMs ->
                    player?.seekTo(finalPositionMs)
                    activeScrubPosition = null
                },
                onErrorGroupClick = { activeErrors = it },
                modifier = Modifier.height(180.dp),
            )

            AnimatedVisibility(visible = activeErrors.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors =
                        CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Ошибки в этой зоне:",
                            color = BrandGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        activeErrors.forEach { err ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp),
                            ) {
                                val color =
                                    if (err.type == com.smartjam.app.model.FeedbackType.WRONG_NOTE)
                                        Color(0xFFFF5252)
                                    else Color(0xFFFFD166)
                                Box(
                                    Modifier.size(8.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(color)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text =
                                        "${if (err.type == com.smartjam.app.model.FeedbackType.WRONG_NOTE) "Нота" else "Ритм"}: ${formatTime(err.studentStartTime)} - ${formatTime(err.studentEndTime)}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                )
                                Spacer(Modifier.weight(1f))
                                TextButton(
                                    onClick = {
                                        player?.seekTo((err.studentStartTime * 1000).toLong())
                                        player?.play()
                                    }
                                ) {
                                    Text("Слушать", color = BrandCyan, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                ScoreItem("Итог", submission.totalScore?.toInt() ?: 0, BrandCyan)
                ScoreItem("Ноты", submission.pitchScore?.toInt() ?: 0, BrandGold)
                ScoreItem("Ритм", submission.rhythmScore?.toInt() ?: 0, Color.White.copy(0.7f))
            }

            Spacer(Modifier.height(32.dp))

            player?.let { smartPlayer ->
                AudioPlayerWithErrorTimeline(
                    player = smartPlayer,
                    externalPosition = activeScrubPosition,
                    onScrubbing = { positionMs -> activeScrubPosition = positionMs },
                    onScrubbingFinished = {
                        val finalPos = activeScrubPosition ?: smartPlayer.currentPosition.value
                        smartPlayer.seekTo(finalPos)
                        activeScrubPosition = null
                    },
                )
            }
        }
    }
}

private fun formatTime(seconds: Double): String {
    val s = seconds.toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun ScoreItem(label: String, score: Int, color: Color) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        Text("$score%", color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}
