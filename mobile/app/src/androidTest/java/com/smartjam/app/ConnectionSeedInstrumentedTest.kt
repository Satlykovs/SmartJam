package com.smartjam.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smartjam.app.api.AuthApi
import com.smartjam.app.api.DevicesApi
import com.smartjam.app.data.local.SmartJamDatabase
import com.smartjam.app.data.local.TokenStorage
import com.smartjam.app.data.local.entity.ConnectionEntity
import com.smartjam.app.domain.model.UserRole
import com.smartjam.app.domain.repository.AuthRepository
import com.smartjam.app.model.AuthResponse
import com.smartjam.app.model.LoginRequest
import com.smartjam.app.model.RefreshRequest
import com.smartjam.app.model.RegisterRequest
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.openapitools.client.infrastructure.ApiClient
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class ConnectionSeedInstrumentedTest {

    @Test
    fun createUserAndSeedConnections() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tokenStorage = TokenStorage(context)
        val apiClient = ApiClient(baseUrl = "http://localhost")

        val fakeAuthApi =
            object : AuthApi {
                override suspend fun loginUser(loginRequest: LoginRequest): Response<AuthResponse> {
                    return Response.success(
                        AuthResponse(
                            accessToken = "mock_access_token",
                            refreshToken = "mock_refresh_token",
                        )
                    )
                }

                override suspend fun refreshToken(
                    refreshRequest: RefreshRequest
                ): Response<AuthResponse> {
                    return Response.success(
                        AuthResponse(
                            accessToken = "mock_access_token",
                            refreshToken = "mock_refresh_token",
                        )
                    )
                }

                override suspend fun registerUser(
                    registerRequest: RegisterRequest
                ): Response<AuthResponse> {
                    return Response.success(
                        AuthResponse(
                            accessToken = "mock_access_token",
                            refreshToken = "mock_refresh_token",
                        )
                    )
                }
            }

        val fakeDevicesApi =
            object : DevicesApi {
                override suspend fun registerDevice(
                    deviceRegistrationRequest: com.smartjam.app.model.DeviceRegistrationRequest
                ): Response<Unit> {
                    return Response.success(Unit)
                }

                override suspend fun unregisterDevice(
                    deviceRegistrationRequest: com.smartjam.app.model.DeviceRegistrationRequest
                ): Response<Unit> {
                    return Response.success(Unit)
                }
            }

        val authRepository = AuthRepository(tokenStorage, fakeAuthApi, apiClient, fakeDevicesApi)
        authRepository.register(
            email = "mmm",
            password = "Qwerty1!",
            username = "mmm",
            role = UserRole.STUDENT,
        )

        val db =
            Room.inMemoryDatabaseBuilder(context, SmartJamDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        try {
            val dao = db.connectionDao()
            val now = Instant.now()

            val connections =
                (1..50).map { index ->
                    ConnectionEntity(
                        connectionId = UUID.randomUUID(),
                        peerId = UUID.randomUUID(),
                        peerUsername = "User$index",
                        createdAt = now,
                        peerFirstName = null,
                        peerLastName = null,
                        peerAvatarUrl = null,
                        peerAvatarBytes = null,
                        myRole = UserRole.STUDENT.name,
                    )
                }

            dao.insertConnections(connections)

            val stored = dao.getConnectionsFlow(UserRole.STUDENT.name).first()
            assertEquals(50, stored.size)

            val refreshToken = tokenStorage.refreshToken.first()
            assertNotNull(refreshToken)
        } finally {
            tokenStorage.clearTokens()
            db.close()
        }
    }
}
