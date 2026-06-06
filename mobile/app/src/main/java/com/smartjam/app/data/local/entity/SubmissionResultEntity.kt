package com.smartjam.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(tableName = "submission_results")
data class SubmissionResultEntity(
    @PrimaryKey val id: UUID,
    val assignmentId: UUID,
    val status: String,
    val totalScore: Float?,
    val pitchScore: Float?,
    val rhythmScore: Float?,
    val errorMessage: String?,
    val fileUrl: String?,
    val submissionAudioLocalPath: String?,
    val createdAt: Instant,
)
