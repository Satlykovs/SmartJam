package com.smartjam.app.domain.repository


import com.smartjam.app.data.local.dao.ConnectionDao
import com.smartjam.app.data.local.entity.ConnectionEntity
import com.smartjam.app.domain.model.Connection
import com.smartjam.app.domain.model.UserRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.collections.emptyList

import com.smartjam.app.api.ConnectionsApi
import com.smartjam.app.model.JoinRequest

class ConnectionRepository (
    private val api: ConnectionsApi,
    private val dao: ConnectionDao
){
    fun getConnectionsFlow(role: UserRole): Flow<List<Connection>> {
        return dao.getConnectionsFlow(role.name).map { entities ->
            entities.map { entity ->
                Connection(
                    id = entity.connectionId.toString(),
                    peerId = entity.peerId.toString(),
                    peerName = entity.peerUsername
                )
            }
        }
    }

    suspend fun syncConnections(role: UserRole): Result<Unit> {
        return try {
            val activeResponse = api.getMyConnections()

            if (activeResponse.isSuccessful) {

                val activeItems = activeResponse.body()?.content ?: emptyList()

                val allEntities = activeItems.map { dto ->
                    ConnectionEntity(
                        connectionId = dto.id,
                        peerId = dto.peerId,
                        peerUsername = dto.peerUsername,
                        createdAt = dto.createdAt,
                        peerFirstName = dto.peerFirstName,
                        peerLastName = dto.peerLastName,
                        peerAvatarUrl = dto.peerAvatarUrl,
                        myRole = role.name
                    )
                }

                dao.clearConnections(role.name)
                dao.insertConnections(allEntities)

                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch connections: ${activeResponse.code()}"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun generateInviteCode(): Result<String> {
        return try {
            val response = api.createInvite()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.inviteCode)
            } else {
                Result.failure(Exception("Failed to generate code"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun joinByCode(code: String): Result<Unit> {
        return try {
            val response = api.joinTeacher(JoinRequest(code))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid invite code: ${response.code()}"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun respondToRequest(connectionId: String, accept: Boolean): Result<Unit> {
        return Result.success(Unit)
    }


}