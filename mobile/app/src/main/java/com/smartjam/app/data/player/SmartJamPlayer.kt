package com.smartjam.app.data.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.smartjam.app.domain.player.MusicPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Call

@OptIn(UnstableApi::class)
class SmartJamPlayer(private val context: Context, private val callFactory: Call.Factory) :
    MusicPlayer {

    private val httpDataSourceFactory = OkHttpDataSource.Factory(callFactory)
    private val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
    private val player =
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
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
                    if (state == Player.STATE_READY) {
                        _isReady.value = true
                        _duration.value = if (player.duration > 0) player.duration else 0L
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                    if (playing) startPositionUpdates() else job?.cancel()
                }

                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    val dur = player.duration
                    if (dur > 0) _duration.value = dur
                }
            }
        )
    }

    override fun prepare(uri: Uri) {
        val mediaItem =
            MediaItem.Builder()
                .setUri(uri)
                .setMimeType(androidx.media3.common.MimeTypes.AUDIO_MPEG)
                .build()

        player.setMediaItem(mediaItem)
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
                if (player.duration > 0) _duration.value = player.duration
                delay(16L)
            }
        }
    }
}
