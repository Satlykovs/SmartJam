package com.smartjam.app.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartjam.app.data.player.SmartJamPlayer
import com.smartjam.app.domain.player.MusicPlayer
import com.smartjam.app.model.FeedbackEvent
import com.smartjam.app.model.FeedbackType

@Composable
fun AudioPlayerWithErrorTimeline(
    audioUri: Uri,
    feedback: List<FeedbackEvent>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val player: MusicPlayer =
        remember(audioUri) { SmartJamPlayer(context).apply { prepare(audioUri) } }

    DisposableEffect(audioUri) { onDispose { player.release() } }

    val currentPositionMs by player.currentPosition.collectAsState()
    val durationMs by player.duration.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()

    val fallbackDurationMs =
        remember(feedback) {
            ((feedback.maxOfOrNull { it.teacherEndTime } ?: 0.0) * 1000.0).toLong()
        }
    val safeDurationMs = maxOf(durationMs, fallbackDurationMs).coerceAtLeast(1L)

    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(12.dp)
    ) {
        ErrorTimelineBar(
            feedback = feedback,
            durationMs = safeDurationMs,
            currentPositionMs = currentPositionMs,
            onSeekTo = { player.seekTo(it) },
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = { if (isPlaying) player.pause() else player.play() },
                modifier =
                    Modifier.size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
            ) {
                if (isPlaying) {
                    PauseIcon()
                } else {
                    Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White)
                }
            }

            Text(
                text = "${formatTime(currentPositionMs)} / ${formatTime(safeDurationMs)}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
            )
        }

        if (feedback.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FeedbackLegendItem(Color(0xFFFF5252), "Неверная нота")
                FeedbackLegendItem(Color(0xFFFFD166), "Неверный ритм")
            }
        }
    }
}

@Composable
private fun PauseIcon() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val barWidth = size.width * 0.22f
        val barHeight = size.height * 0.6f
        val gap = size.width * 0.14f
        val left = (size.width - 2 * barWidth - gap) / 2f
        val top = (size.height - barHeight) / 2f
        drawRoundRect(
            Color.White,
            Offset(left, top),
            Size(barWidth, barHeight),
            CornerRadius(2.dp.toPx()),
        )
        drawRoundRect(
            Color.White,
            Offset(left + barWidth + gap, top),
            Size(barWidth, barHeight),
            CornerRadius(2.dp.toPx()),
        )
    }
}

@Composable
private fun ErrorTimelineBar(
    feedback: List<FeedbackEvent>,
    durationMs: Long,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
) {
    Canvas(
        modifier =
            Modifier.fillMaxWidth().height(40.dp).pointerInput(durationMs) {
                detectTapGestures { offset ->
                    val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeekTo((ratio * durationMs).toLong())
                }
            }
    ) {
        val trackHeight = 6.dp.toPx()
        val errorHeight = 14.dp.toPx()
        val centerY = size.height / 2f

        drawRoundRect(
            Color.White.copy(0.18f),
            Offset(0f, centerY - trackHeight / 2),
            Size(size.width, trackHeight),
            CornerRadius(3.dp.toPx()),
        )

        val progressWidth = (currentPositionMs.toFloat() / durationMs) * size.width
        drawRoundRect(
            Color.White.copy(0.45f),
            Offset(0f, centerY - trackHeight / 2),
            Size(progressWidth, trackHeight),
            CornerRadius(3.dp.toPx()),
        )

        feedback.forEach { event ->
            val startX =
                ((event.teacherStartTime * 1000) / durationMs.toDouble() * size.width).toFloat()
            val endX =
                ((event.teacherEndTime * 1000) / durationMs.toDouble() * size.width).toFloat()
            val color =
                if (event.type == FeedbackType.WRONG_NOTE) Color(0xFFFF5252) else Color(0xFFFFD166)
            drawRoundRect(
                color.copy(0.85f),
                Offset(startX, centerY - errorHeight / 2),
                Size((endX - startX).coerceAtLeast(4f), errorHeight),
                CornerRadius(2.dp.toPx()),
            )
        }

        drawCircle(Color.White, 7.dp.toPx(), Offset(progressWidth, centerY))
    }
}

@Composable
private fun FeedbackLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.White.copy(0.6f), fontSize = 10.sp)
    }
}

private fun formatTime(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / 1000) / 60
    return "%d:%02d".format(min, sec)
}
