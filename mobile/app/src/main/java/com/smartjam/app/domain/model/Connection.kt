package com.smartjam.app.domain.model


data class Connection(
    val id: String,
    val peerId: String,
    val peerName: String,
    val peerAvatarUrl: String? = null,
    val peerAvatarBytes: ByteArray? = null
)