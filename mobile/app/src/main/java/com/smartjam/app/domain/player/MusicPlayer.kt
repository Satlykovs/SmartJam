package com.smartjam.app.domain.player

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

interface MusicPlayer {
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val isPlaying: StateFlow<Boolean>
    val isReady: StateFlow<Boolean>
    val currentSpeed: StateFlow<Float>

    fun setPlaybackSpeed(speed: Float)

    fun prepare(uri: Uri, title: String, subtitle: String)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun seekRelative(offsetMs: Long)

    fun release()
}
