package com.smartjam.app.data.api

import com.smartjam.app.data.model.JoinRoomRequest
import com.smartjam.app.data.model.RoomResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RoomApi {
    @POST("/api/rooms/join")
    suspend fun joinRoom(@Body request: JoinRoomRequest): Response<RoomResponse>
}