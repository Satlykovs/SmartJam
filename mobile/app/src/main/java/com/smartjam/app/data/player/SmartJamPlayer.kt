package com.smartjam.app.data.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.*
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
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

    private val extractorsFactory =
        androidx.media3.extractor.DefaultExtractorsFactory().apply {
            setConstantBitrateSeekingAlwaysEnabled(true)
            setMp3ExtractorFlags(
                androidx.media3.extractor.mp3.Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING
            )
            setMp4ExtractorFlags(
                androidx.media3.extractor.mp4.Mp4Extractor.FLAG_READ_WITHIN_GOP_SAMPLE_DEPENDENCIES
            )
        }

    private val httpDataSourceFactory = OkHttpDataSource.Factory(callFactory)
    private val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

    private val player =
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory))
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
                        if (player.duration > 0) {
                            _duration.value = player.duration
                        }
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

        val realDurationMs =
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val time =
                    retriever.extractMetadata(
                        android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
                    )
                retriever.release()
                time?.toLong() ?: 0L
            } catch (e: Exception) {
                0L
            }

        if (realDurationMs > 0) {
            Log.d("SmartJam_Player", "Total dur$realDurationMs")
            _duration.value = realDurationMs
        }

        val safeUri =
            try {
                val urlString = uri.toString()
                Uri.parse(Uri.decode(urlString))
            } catch (e: Exception) {
                uri
            }

        val mediaItem =
            MediaItem.Builder().setUri(safeUri).setMimeType(MimeTypes.AUDIO_UNKNOWN).build()

        player.setMediaItem(mediaItem)
        player.prepare()
    }

    override fun play() = player.play()

    override fun pause() = player.pause()

    override fun seekTo(positionMs: Long) {

        if (player.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
            Log.d("SmartJam_Player", "Seek is available, seeking now to $positionMs")
            player.seekTo(positionMs)
            _currentPosition.value = positionMs
        } else {
            Log.d("SmartJam_Player", "WATAFA SEEK IS NOT AVAILABLE")
            Log.d("SmartJam_Player", player.audioFormat.toString())
        }
    }

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
                delay(100L)
            }
        }
    }
}
