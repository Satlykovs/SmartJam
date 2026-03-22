package com.smartjam.app.data.model

data class CreateAssignmentRequest(
    val connectionId: String,
    val title: String,
    val description: String?
)

data class CreateSubmissionRequest(
    val assignmentId: String
)

data class PresignedUrlResponse(
    val uploadUrl: String,
    val entityId: String
)

data class SubmissionStatusResponse(
    val id: String,
    val status: String,
    val pitchScore: Int?,
    val rhythmScore: Int?,
    val errorMessage: String?
)