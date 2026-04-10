package com.smartjam.app.domain.repository

import com.smartjam.app.data.api.SmartJamApi
import com.smartjam.app.data.local.dao.ConnectionDao
import com.smartjam.app.data.local.entity.ConnectionEntity
import com.smartjam.app.data.model.JoinRequest
import com.smartjam.app.data.model.RespondConnectionRequest
import com.smartjam.app.domain.model.Connection
import com.smartjam.app.domain.model.UserRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConnectionRepository (
    private val api: SmartJamApi,
    private val dao: ConnectionDao
){
    fun getConnectionsFlow(role: UserRole): Flow<List<Connection>> {
        return dao.getConnectionsFlow(role.name).map { entities ->
            entities.map { entity ->
                Connection(
                    id = entity.connectionId,
                    peerId = entity.peerId,
                    peerName = entity.peerName,
                    status = entity.status
                )
            }
        }
    }

    suspend fun syncConnections(role: UserRole): Result<Unit> {
        return try {
            val activeResponse = api.getActiveConnections()
            val pendingResponse = api.getPendingConnections()

            if (activeResponse.isSuccessful && pendingResponse.isSuccessful) {
                val active = activeResponse.body() ?: emptyList()
                val pending = pendingResponse.body() ?: emptyList()

                val allEntities = (active + pending).map { dto ->
                    ConnectionEntity(
                        connectionId = dto.connectionId,
                        peerId = dto.peerId,
                        peerName = dto.peerName,
                        status = dto.status,
                        myRole = role.name
                    )
                }

                dao.clearConnections(role.name)
                dao.insertConnections(allEntities)

                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch connections"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun generateInviteCode(): Result<String> {
        return try {
            val response = api.generateInviteCode()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.code)
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
            val response = api.joinByCode(JoinRequest(code))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid invite code"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun respondToRequest(connectionId: String, accept: Boolean): Result<Unit> {
        return try {
            val response = api.respondToConnection(connectionId, RespondConnectionRequest(accept))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to respond"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

}