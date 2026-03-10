package com.smartjam.app.data.model

data class JoinRoomRequest(
    val inviteCode: String
)

data class RoomResponse(
    val id: String,
    val teacherName: String,
    val title: String
)