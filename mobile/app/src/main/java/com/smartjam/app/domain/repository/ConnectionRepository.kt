package com.smartjam.app.domain.repository

import android.util.Log
import com.smartjam.app.api.ConnectionsApi
import com.smartjam.app.data.local.dao.ConnectionDao
import com.smartjam.app.data.local.entity.ConnectionEntity
import com.smartjam.app.domain.model.Connection
import com.smartjam.app.domain.model.UserRole
import com.smartjam.app.model.JoinRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class ConnectionRepository(private val api: ConnectionsApi, private val dao: ConnectionDao) {
    data class ConnectionPageInfo(
        val pageNumber: Int,
        val totalPages: Int,
        val pageSize: Int,
        val totalElements: Long,
    )

    private val avatarClient = OkHttpClient.Builder().build()

    fun getConnectionsFlow(role: UserRole): Flow<List<Connection>> {
        return dao.getConnectionsFlow(role.name).map { entities ->
            entities.map { entity ->
                Connection(
                    id = entity.connectionId.toString(),
                    peerId = entity.peerId.toString(),
                    peerName = entity.peerUsername,
                    peerAvatarUrl = entity.peerAvatarUrl,
                    peerAvatarBytes = entity.peerAvatarBytes,
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
                Log.e("SmartJam_Connections", "I am here")

                val activeResponse = api.getMyConnections(page = page, size = size)
                Log.e("SmartJam_Connections", "Now I am here")
                if (activeResponse.isSuccessful && activeResponse.body() != null) {
                    val body = activeResponse.body()!!
                    val activeItems = body.content
                    val ids = activeItems.map { it.id }
                    val existing = dao.getConnectionsByIds(ids).associateBy { it.connectionId }

                    val allEntities = activeItems.map { dto ->
                        val avatarUrl = dto.peerAvatarUrl?.toString()
                        val cached = existing[dto.id]
                        val avatarBytes =
                            when {
                                avatarUrl.isNullOrBlank() -> null
                                cached != null &&
                                    cached.peerAvatarUrl == avatarUrl &&
                                    cached.peerAvatarBytes != null -> cached.peerAvatarBytes
                                else -> downloadAvatar(avatarUrl)
                            }

                        ConnectionEntity(
                            connectionId = dto.id,
                            peerId = dto.peerId,
                            peerUsername = dto.peerUsername,
                            createdAt = dto.createdAt,
                            peerFirstName = dto.peerFirstName,
                            peerLastName = dto.peerLastName,
                            peerAvatarUrl = avatarUrl,
                            peerAvatarBytes = avatarBytes,
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
                Log.e("SmartJam_Connection_repo", e.message!!)
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

    private suspend fun downloadAvatar(url: String): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = avatarClient.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
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
}
