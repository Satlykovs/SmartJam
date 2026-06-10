package com.smartjam.app.domain.repository

import android.util.Log
import com.smartjam.app.api.ConnectionsApi
import com.smartjam.app.data.local.dao.ConnectionDao
import com.smartjam.app.data.local.entity.ConnectionEntity
import com.smartjam.app.domain.model.Connection
import com.smartjam.app.domain.model.UserRole
import com.smartjam.app.model.JoinRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class ConnectionRepository
@Inject
constructor(private val api: ConnectionsApi, private val dao: ConnectionDao) {
    data class ConnectionPageInfo(
        val pageNumber: Int,
        val totalPages: Int,
        val pageSize: Int,
        val totalElements: Long,
    )

    fun getConnectionsFlow(role: UserRole): Flow<List<Connection>> {
        return dao.getConnectionsFlow(role.name).map { entities ->
            entities.map { entity ->
                Connection(
                    id = entity.connectionId.toString(),
                    peerId = entity.peerId.toString(),
                    peerName = entity.peerUsername,
                    peerAvatarUrl = entity.peerAvatarUrl,
                )
            }
        }
    }

    suspend fun syncConnectionsPage(
        role: UserRole,
        page: Int,
        size: Int,
    ): Result<ConnectionPageInfo> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val activeResponse = api.getMyConnections(page = page, size = size)

                if (activeResponse.isSuccessful && activeResponse.body() != null) {
                    val body = activeResponse.body()!!
                    val activeItems = body.content

                    // Теперь маппинг простой и чистый, без скачивания файлов!
                    val allEntities = activeItems.map { dto ->
                        ConnectionEntity(
                            connectionId = dto.id,
                            peerId = dto.peerId,
                            peerUsername = dto.peerUsername,
                            createdAt = dto.createdAt,
                            peerFirstName = dto.peerFirstName,
                            peerLastName = dto.peerLastName,
                            peerAvatarUrl = dto.peerAvatarUrl?.toString(), // Просто берём URL
                            myRole = role.name,
                        )
                    }

                    dao.insertConnections(allEntities)

                    Result.success(
                        ConnectionPageInfo(
                            pageNumber = body.page.number,
                            totalPages = body.page.totalPages,
                            pageSize = body.page.propertySize,
                            totalElements = body.page.totalElements,
                        )
                    )
                } else {
                    Result.failure(
                        Exception("Failed to fetch connections: ${activeResponse.code()}")
                    )
                }
            } catch (e: Exception) {
                Log.e("SmartJam_Connection_repo", e.message ?: "Unknown error")
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }

    @Deprecated("Use syncConnectionsPage for paged loading")
    suspend fun syncConnections(role: UserRole): Result<Unit> {
        return syncConnectionsPage(role, page = 0, size = 20).map {}
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

    suspend fun clearAllConnections(): Result<Unit> {
        return try {
            dao.clearAllConnections()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun getPeerName(connectionId: UUID): String? = dao.getPeerNameById(connectionId)
}
