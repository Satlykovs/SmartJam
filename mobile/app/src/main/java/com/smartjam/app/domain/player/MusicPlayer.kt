package com.smartjam.app.domain.player

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

interface MusicPlayer {
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val isPlaying: StateFlow<Boolean>
    val isReady: StateFlow<Boolean>

    fun prepare(uri: Uri)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun release()
}
