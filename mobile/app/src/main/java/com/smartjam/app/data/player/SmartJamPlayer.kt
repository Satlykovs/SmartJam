package com.smartjam.app.data.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.*
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.smartjam.app.domain.player.MusicPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Call

@OptIn(UnstableApi::class)
class SmartJamPlayer(
    private val context: Context,
    private val callFactory: Call.Factory,
    private val player: ExoPlayer,
) : MusicPlayer {

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    override val duration: StateFlow<Long> = _duration

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady

    private val _currentSpeed = MutableStateFlow(1.0f)
    override val currentSpeed: StateFlow<Float> = _currentSpeed

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        _isReady.value = true
                        if (player.duration > 0) _duration.value = player.duration
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                    if (playing) startPositionUpdates() else job?.cancel()
                }

                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    if (player.duration > 0) _duration.value = player.duration
                }
            }
        )
    }

    override fun prepare(uri: Uri, title: String, subtitle: String) {
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val time =
                retriever.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
                )
            retriever.release()

            val durationMs = time?.toLong() ?: 0L
            if (durationMs > 0) {
                _duration.value = durationMs
                Log.d("SmartJam_Player", "Manual duration detected: $durationMs")
            }
        } catch (e: Exception) {
            Log.e("SmartJam_Player", "Error getting duration", e)
        }

        val mediaMetadata =
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(subtitle)
                .setArtworkUri(
                    "android.resource://${context.packageName}/mipmap/ic_launcher".toUri()
                )
                .build()

        val mediaItem =
            MediaItem.Builder()
                .setUri(uri)
                .setMimeType(MimeTypes.AUDIO_UNKNOWN)
                .setMediaMetadata(mediaMetadata)
                .build()

        player.setMediaItem(mediaItem)
        player.prepare()
    }

    override fun play() {
        player.play()
    }

    override fun pause() = player.pause()

    override fun seekTo(positionMs: Long) {
        if (player.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
            player.seekTo(positionMs)
            _currentPosition.value = positionMs
        }
    }

    override fun seekRelative(offsetMs: Long) {
        val newPos = (player.currentPosition + offsetMs).coerceIn(0, player.duration)
        player.seekTo(newPos)
        _currentPosition.value = newPos
    }

    override fun setPlaybackSpeed(speed: Float) {
        _currentSpeed.value = speed
        player.playbackParameters = PlaybackParameters(speed)
    }

    override fun release() {
        job?.cancel()
        scope.cancel()
    }

    private fun startPositionUpdates() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                _currentPosition.value = player.currentPosition
                delay(100L)
            }
        }
    }
}
