package com.smartjam.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.net.URI
import java.time.Instant
import java.util.UUID

@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey val connectionId: UUID,
    val peerId: UUID,
    val peerUsername: String,
    val createdAt: Instant,
    val peerFirstName: String? = null,
    val peerLastName: String? = null,
    val peerAvatarUrl: URI? = null,
    val myRole: String
)