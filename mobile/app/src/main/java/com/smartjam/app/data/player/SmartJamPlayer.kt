package com.smartjam.app.data.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.smartjam.app.domain.player.MusicPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(UnstableApi::class)
class SmartJamPlayer(private val context: Context) : MusicPlayer {

    private val player =
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
        }

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    override val duration: StateFlow<Long> = _duration

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    _isReady.value = state == Player.STATE_READY
                    _duration.value = if (player.duration > 0) player.duration else 0L
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                    if (playing) startPositionUpdates() else job?.cancel()
                }
            }
        )
    }

    override fun prepare(uri: Uri) {
        val finalUri =
            if (uri.host == "localhost" || uri.host == "127.0.0.1") {
                uri.buildUpon()
                    .encodedAuthority("10.0.2.2:${uri.port.takeIf { it > 0 } ?: ""}")
                    .build()
            } else uri

        val httpFactory = DefaultHttpDataSource.Factory()
        uri.host?.let { httpFactory.setDefaultRequestProperties(mapOf("Host" to it)) }

        val dsFactory = DefaultDataSource.Factory(context, httpFactory)
        val mediaSource =
            DefaultMediaSourceFactory(dsFactory).createMediaSource(MediaItem.fromUri(finalUri))

        player.setMediaSource(mediaSource)
        player.prepare()
    }

    override fun play() = player.play()

    override fun pause() = player.pause()

    override fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    override fun release() {
        job?.cancel()
        scope.cancel()
        player.release()
    }

    private fun startPositionUpdates() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                _currentPosition.value = player.currentPosition
                delay(200)
            }
        }
    }
}
