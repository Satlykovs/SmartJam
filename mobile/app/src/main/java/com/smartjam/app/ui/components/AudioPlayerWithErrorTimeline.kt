package com.smartjam.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartjam.app.domain.player.MusicPlayer
import com.smartjam.app.model.FeedbackEvent

@Composable
fun AudioPlayerWithErrorTimeline(
    player: MusicPlayer,
    feedback: List<FeedbackEvent>,
    modifier: Modifier = Modifier,
) {
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
                if (isPlaying) PauseIcon()
                else Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White)
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
private fun FeedbackLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.White.copy(0.6f), fontSize = 10.sp)
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
