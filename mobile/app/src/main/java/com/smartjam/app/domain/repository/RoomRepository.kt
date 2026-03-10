package com.smartjam.app.domain.repository

import com.smartjam.app.data.api.RoomApi
import com.smartjam.app.data.model.JoinRoomRequest
import com.smartjam.app.data.model.RoomResponse

class RoomRepository(
    private val roomApi: RoomApi
) {
    suspend fun joinRoomByCode(code: String): Result<RoomResponse> {
        return try {
            val response = roomApi.joinRoom(JoinRoomRequest(inviteCode = code))

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Неверный код или комната не найдена"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка сети: проверьте подключение"))
        }
    }
}
