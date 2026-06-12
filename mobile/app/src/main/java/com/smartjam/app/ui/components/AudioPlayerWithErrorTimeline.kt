package com.smartjam.app.ui.components

import android.util.Log
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartjam.app.domain.player.MusicPlayer
import com.smartjam.app.ui.theme.BrandCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerWithErrorTimeline(
    player: MusicPlayer,
    modifier: Modifier = Modifier,
    externalPosition: Long? = null,
    onScrubbing: (Long) -> Unit = {},
    onScrubbingFinished: () -> Unit = {},
) {
    val currentPos by player.currentPosition.collectAsState()
    val duration by player.duration.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()

    val safeDuration = if (duration > 0f) duration.toFloat() else 1f

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(currentPos, externalPosition, isDragging) {
        if (!isDragging) {
            val targetPos = externalPosition ?: currentPos
            sliderPosition = targetPos.toFloat().coerceIn(0f, safeDuration)
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { if (isPlaying) player.pause() else player.play() },
                    modifier =
                        Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(0.15f)),
                ) {
                    if (isPlaying) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(4.dp, 16.dp).background(Color.White))
                            Box(Modifier.size(4.dp, 16.dp).background(Color.White))
                        }
                    } else Icon(Icons.Filled.PlayArrow, null, tint = Color.White)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Slider(
                    value = sliderPosition,
                    onValueChange = { newPos ->
                        isDragging = true
                        sliderPosition = newPos
                        onScrubbing(newPos.toLong())
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        player.seekTo(sliderPosition.toLong())
                        Log.d("SmartJam_Player", "Final Seek to: ${sliderPosition.toLong()}")
                        onScrubbingFinished()
                    },
                    valueRange = 0f..safeDuration,
                    modifier = Modifier.weight(1f),
                    colors =
                        SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = BrandCyan,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                        ),
                    thumb = {
                        Box(
                            modifier = Modifier.size(12.dp).background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {}
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            modifier = Modifier.height(3.dp),
                            colors =
                                SliderDefaults.colors(
                                    activeTrackColor = BrandCyan,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                                ),
                        )
                    },
                )

                Spacer(modifier = Modifier.width(16.dp))

                val displayTime = if (isDragging) sliderPosition.toLong() else currentPos
                Text(
                    text = "${formatTime(displayTime)} / ${formatTime(duration)}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FeedbackLegendItem(Color(0xFFFF5252), "Неверная нота")
                FeedbackLegendItem(Color(0xFFFFD166), "Неверный ритм")
            }
        }
    }
}

@Composable
private fun FeedbackLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White.copy(0.6f), fontSize = 12.sp)
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
