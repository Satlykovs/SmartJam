package com.smartjam.app.ui.screens.submission

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.smartjam.app.ui.components.*
import com.smartjam.app.ui.screens.assignment.formatSubmissionDate
import com.smartjam.app.ui.theme.*

@Composable
fun SubmissionDetailScreen(viewModel: SubmissionViewModel = hiltViewModel(), onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val submission = state.submission
    val player = viewModel.player

    var activeErrors by remember {
        mutableStateOf<List<com.smartjam.app.model.FeedbackEvent>>(emptyList())
    }
    var activeScrubPosition by remember { mutableStateOf<Long?>(null) }

    val selectionRange =
        remember(activeErrors) {
            if (activeErrors.isEmpty()) null
            else {
                activeErrors.minOf { it.studentStartTime }.toFloat()..activeErrors
                        .maxOf { it.studentEndTime }
                        .toFloat()
            }
        }

    val currentPosRaw = player?.currentPosition?.collectAsState()?.value ?: 0L
    val durationMs = player?.duration?.collectAsState()?.value ?: 0L
    val currentPos = activeScrubPosition ?: currentPosRaw

    Box(Modifier.fillMaxSize().background(CoreBackground)) {
        AppleLiquidBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 60.dp),
        ) {
            item {
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
                            state.assignmentTitle,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            formatSubmissionDate(state.createdAt),
                            color = Color.White.copy(0.5f),
                            fontSize = 13.sp,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            if (state.isLoading || submission == null || player == null) {
                item {
                    Box(Modifier.fillParentMaxHeight(0.7f).fillMaxWidth(), Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            } else {
                item {
                    WaveformCompare(
                        teacherRms = submission.teacherWaveform ?: emptyList(),
                        studentRms = submission.studentWaveform ?: emptyList(),
                        feedback = submission.feedback ?: emptyList(),
                        currentPositionMs = currentPos,
                        durationMs = durationMs,
                        selectedRange = selectionRange,
                        onSeek = { player.seekTo(it) },
                        onScrubbing = { activeScrubPosition = it },
                        onScrubbingFinished = {
                            player.seekTo(it)
                            activeScrubPosition = null
                        },
                        onErrorGroupClick = { activeErrors = it },
                        modifier = Modifier.height(180.dp),
                    )
                }

                item {
                    AnimatedVisibility(visible = activeErrors.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.12f)
                                ),
                            shape = RoundedCornerShape(20.dp),
                            border =
                                androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color.White.copy(0.08f),
                                ),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    Arrangement.SpaceBetween,
                                    Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Ошибки в этой зоне:",
                                        color = BrandGold,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    IconButton(
                                        onClick = { activeErrors = emptyList() },
                                        Modifier.size(24.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            null,
                                            tint = Color.White.copy(0.5f),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Box(
                                    Modifier.heightIn(max = 240.dp).pointerInput(Unit) {
                                        detectVerticalDragGestures(
                                            onDragStart = {},
                                            onDragEnd = {},
                                            onDragCancel = {},
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                            },
                                        )
                                    }
                                ) {
                                    Column(Modifier.verticalScroll(rememberScrollState())) {
                                        activeErrors.forEach { err -> ErrorRow(err, player) }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        ScoreItem("Итог", submission.totalScore?.toInt() ?: 0, BrandCyan)
                        ScoreItem("Ноты", submission.pitchScore?.toInt() ?: 0, BrandGold)
                        ScoreItem(
                            "Ритм",
                            submission.rhythmScore?.toInt() ?: 0,
                            Color.White.copy(0.7f),
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                }

                item {
                    AudioPlayerWithErrorTimeline(
                        player = player,
                        externalPosition = activeScrubPosition,
                        onScrubbing = { activeScrubPosition = it },
                        onScrubbingFinished = {
                            val finalPos = activeScrubPosition ?: player.currentPosition.value
                            player.seekTo(finalPos)
                            activeScrubPosition = null
                        },
                    )
                }

                item {
                    Spacer(Modifier.height(40.dp))
                    Text(
                        "Комментарии преподавателя",
                        color = Color.White.copy(0.4f),
                        fontSize = 14.sp,
                    )
                    Box(
                        Modifier.fillMaxWidth()
                            .height(120.dp)
                            .padding(top = 12.dp)
                            .background(Color.White.copy(0.04f), RoundedCornerShape(20.dp))
                            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(20.dp)),
                        Alignment.Center,
                    ) {
                        Text("Пока пусто", color = Color.White.copy(0.2f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorRow(
    err: com.smartjam.app.model.FeedbackEvent,
    player: com.smartjam.app.domain.player.MusicPlayer,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp),
    ) {
        val color =
            if (err.type == com.smartjam.app.model.FeedbackType.WRONG_NOTE) Color(0xFFFF5252)
            else Color(0xFFFFD166)
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(
            text =
                "${if (err.type == com.smartjam.app.model.FeedbackType.WRONG_NOTE) "Нота" else "Ритм"}: ${formatTime(err.studentStartTime)} - ${formatTime(err.studentEndTime)}",
            color = Color.White,
            fontSize = 13.sp,
        )
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = {
                player.seekTo((err.studentStartTime * 1000).toLong())
                player.play()
            }
        ) {
            Text("Слушать", color = BrandCyan, fontSize = 12.sp)
        }
    }
}

private fun formatTime(seconds: Double): String {
    val s = seconds.toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

@Composable
private fun ScoreItem(label: String, score: Int, color: Color) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        Text("$score%", color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}
