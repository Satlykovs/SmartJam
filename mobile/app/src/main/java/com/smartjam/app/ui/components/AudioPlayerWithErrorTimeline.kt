package com.smartjam.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Forward5
import androidx.compose.material.icons.rounded.Replay5
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    val speed by player.currentSpeed.collectAsState()

    val safeDuration = if (duration > 0f) duration.toFloat() else 1f
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    LaunchedEffect(currentPos, externalPosition, isDragging) {
        if (!isDragging) {
            val targetPos = externalPosition ?: currentPos
            sliderPosition = targetPos.toFloat().coerceIn(0f, safeDuration)
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    IconButton(
                        onClick = { player.seekRelative(-5000) },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay5,
                            contentDescription = "-5s",
                            tint = Color.White.copy(0.5f),
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    IconButton(
                        onClick = { if (isPlaying) player.pause() else player.play() },
                        modifier =
                            Modifier.size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.12f))
                                .border(1.dp, Color.White.copy(0.1f), CircleShape),
                    ) {
                        if (isPlaying) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(4.dp, 18.dp).background(Color.White))
                                Box(Modifier.size(4.dp, 18.dp).background(Color.White))
                            }
                        } else {
                            Icon(
                                Icons.Filled.PlayArrow,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }

                    IconButton(
                        onClick = { player.seekRelative(5000) },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forward5,
                            contentDescription = "+5s",
                            tint = Color.White.copy(0.5f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

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
                        onScrubbingFinished()
                    },
                    valueRange = 0f..safeDuration,
                    modifier = Modifier.weight(1f),
                    colors =
                        SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = BrandCyan,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                        ),
                    thumb = { Box(Modifier.size(10.dp).background(Color.White, CircleShape)) },
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            modifier = Modifier.height(3.dp),
                            colors =
                                SliderDefaults.colors(
                                    activeTrackColor = BrandCyan,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                                ),
                        )
                    },
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box {
                    Text(
                        text = "${speed}x",
                        color = BrandCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier.clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(0.08f))
                                .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(10.dp))
                                .clickable { showSpeedMenu = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                    )

                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false },
                        modifier =
                            Modifier.width(80.dp)
                                .background(Color(0xFF1C1C2B).copy(alpha = 0.98f))
                                .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f).forEach { s ->
                            DropdownMenuItem(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                text = {
                                    Text(
                                        "${s}x",
                                        color = if (s == speed) BrandCyan else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight =
                                            if (s == speed) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    player.setPlaybackSpeed(s)
                                    showSpeedMenu = false
                                },
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                val displayTime = if (isDragging) sliderPosition.toLong() else currentPos
                Text(
                    text = "${formatTime(displayTime)} / ${formatTime(duration)}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

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
