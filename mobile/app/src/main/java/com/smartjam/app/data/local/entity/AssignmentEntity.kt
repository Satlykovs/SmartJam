package com.smartjam.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.net.URI
import java.time.Instant
import java.util.UUID

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey val id: UUID,
    val connectionId: UUID,
    val title: String,
    val description: String?,
    val referenceAudioUrl: URI?,
    val referenceAudioLocalPath: String?,
    val status: String,
    val createdAt: Instant,
)
