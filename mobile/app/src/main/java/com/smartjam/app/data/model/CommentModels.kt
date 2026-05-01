package com.smartjam.app.data.model

data class SendCommentRequest(
    val commentText: String
)

data class CommentResponse(
    val attemptId: String,
    val commentText: String,
    val timestamp: Long
)