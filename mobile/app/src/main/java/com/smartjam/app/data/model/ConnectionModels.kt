package com.smartjam.app.data.model

data class InviteCodeResponse(
    val code: String
)

data class JoinRequest(
    val inviteCode: String,
)

data class ConnectionDto(
    val connectionId: String,
    val peerId: String,
    val peerName: String,
    val status: String
)

data class RespondConnectionRequest(
    val accept: Boolean
)

