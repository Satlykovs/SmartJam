package com.smartjam.app.domain.model

data class Connection(
    val id: String,
    val peerId: String,
    val peerName: String,
    val peerFirstName: String?,
    val peerLastName: String?,
    val peerAvatarUrl: String?,
    val createdAt: java.time.Instant,
)
