package com.smartjam.app.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.smartjam.app.model.FeedbackEvent
import com.smartjam.app.model.FeedbackType
import kotlinx.coroutines.delay

@Composable
fun AudioPlayerWithErrorTimeline(
    audioUri: Uri,
    feedback: List<FeedbackEvent>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val resolvedUri = remember(audioUri) {
        if (audioUri.scheme == "http" || audioUri.scheme == "https") {
            val host = audioUri.host
            if (host == "localhost" || host == "127.0.0.1") {
                val port = audioUri.port
                val newAuthority = if (port > 0) "10.0.2.2:$port" else "10.0.2.2"
                audioUri.buildUpon()
                    .encodedAuthority(newAuthority)
                    .build()
            } else {
                audioUri
            }
        } else {
            audioUri
        }
    }

    val originalHost = remember(audioUri) {
        if (audioUri.scheme == "http" || audioUri.scheme == "https") audioUri.host else null
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    val dataSourceFactory = remember(resolvedUri, originalHost) {
        val httpFactory = DefaultHttpDataSource.Factory()
        if (!originalHost.isNullOrBlank() && originalHost != resolvedUri.host) {
            httpFactory.setDefaultRequestProperties(mapOf("Host" to originalHost))
        }
        DefaultDataSource.Factory(context, httpFactory)
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .build().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true
                )
                volume = 1f
            }
    }

    var durationMs by remember { mutableLongStateOf(0L) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }

    val fallbackDurationMs = remember(feedback) {
        ((feedback.maxOfOrNull { it.teacherEndTime } ?: 0.0) * 1000.0).toLong()
    }


    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                val duration = exoPlayer.duration
                if (duration != C.TIME_UNSET && duration > 0L) {
                    durationMs = duration
                }
                isReady = state == Player.STATE_READY || state == Player.STATE_BUFFERING
                isPlaying = exoPlayer.isPlaying
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("AudioPlayer", "Playback error: ${error.message}", error)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPositionMs = exoPlayer.currentPosition
            delay(200L)
        }
    }

    LaunchedEffect(resolvedUri, dataSourceFactory) {
        @androidx.annotation.OptIn(UnstableApi::class)
        val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(resolvedUri))
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
    }

    val safeDurationMs = maxOf(durationMs, fallbackDurationMs).coerceAtLeast(1L)


    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(12.dp)
    ) {
        ErrorTimelineBar(
            feedback = feedback,
            durationMs = safeDurationMs,
            currentPositionMs = currentPositionMs,
            onSeekTo = { targetMs ->
                exoPlayer.seekTo(targetMs)
                currentPositionMs = targetMs
                exoPlayer.playWhenReady = true
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.playWhenReady = true
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                if (isPlaying) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val barWidth = size.width * 0.22f
                        val barHeight = size.height * 0.6f
                        val gap = size.width * 0.14f
                        val left = (size.width - 2 * barWidth - gap) / 2f
                        val top = (size.height - barHeight) / 2f
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(left + barWidth + gap, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Воспроизвести",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "${formatTime(currentPositionMs)} / ${formatTime(safeDurationMs)}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        if (feedback.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeedbackLegendItem(color = Color(0xFFFF5252), label = "Неверная нота")
                FeedbackLegendItem(color = Color(0xFFFFD166), label = "Неверный ритм")
            }
        }
    }
}

@Composable
private fun FeedbackLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

@Composable
private fun ErrorTimelineBar(
    feedback: List<FeedbackEvent>,
    durationMs: Long,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit
) {
    val trackColor = Color.White.copy(alpha = 0.18f)
    val progressColor = Color.White.copy(alpha = 0.45f)
    val cursorColor = Color.White

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .pointerInput(feedback, durationMs) {
                detectTapGestures { offset ->
                    val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                    val targetMs = (ratio * durationMs).toLong()
                    onSeekTo(targetMs)
                }
            }
    ) {
        val trackHeight = 6.dp.toPx()
        val errorHeight = 14.dp.toPx()
        val centerY = size.height / 2f
        val trackTop = centerY - trackHeight / 2f
        val cornerRadius = 3.dp.toPx()

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, trackTop),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(cornerRadius)
        )

        val progressRatio = if (durationMs > 0) {
            (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        } else 0f

        if (progressRatio > 0f) {
            drawRoundRect(
                color = progressColor,
                topLeft = Offset(0f, trackTop),
                size = Size(size.width * progressRatio, trackHeight),
                cornerRadius = CornerRadius(cornerRadius)
            )
        }

        val errorTop = centerY - errorHeight / 2f
        feedback.forEach { event ->
            val startX = ((event.teacherStartTime * 1000.0) / durationMs * size.width).toFloat()
            val endX = ((event.teacherEndTime * 1000.0) / durationMs * size.width).toFloat()
            val segmentWidth = (endX - startX).coerceAtLeast(3.dp.toPx())
            val color = when (event.type) {
                FeedbackType.WRONG_NOTE -> Color(0xFFFF5252)
                FeedbackType.WRONG_RHYTHM -> Color(0xFFFFD166)
            }
            drawRoundRect(
                color = color.copy(alpha = 0.85f),
                topLeft = Offset(startX, errorTop),
                size = Size(segmentWidth, errorHeight),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }


        val cursorX = size.width * progressRatio
        val cursorRadius = 7.dp.toPx()
        drawCircle(
            color = cursorColor,
            radius = cursorRadius,
            center = Offset(cursorX, centerY)
        )
    }
}
