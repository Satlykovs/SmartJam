package com.smartjam.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey val connectionId: String,
    val peerId: String,
    val peerName: String,
    val status: String,
    val myRole: String
)