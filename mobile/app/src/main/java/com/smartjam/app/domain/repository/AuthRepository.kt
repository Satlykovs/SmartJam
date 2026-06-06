package com.smartjam.app.domain.repository

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.smartjam.app.BuildConfig
import com.smartjam.app.api.AuthApi
import com.smartjam.app.api.DevicesApi
import com.smartjam.app.data.local.TokenStorage
import com.smartjam.app.domain.model.UserRole
import com.smartjam.app.model.DeviceRegistrationRequest
import com.smartjam.app.model.LoginRequest
import com.smartjam.app.model.RefreshRequest
import com.smartjam.app.model.RegisterRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.openapitools.client.infrastructure.ApiClient

class AuthRepository(
    private val tokenStorage: TokenStorage,
    private val authApi: AuthApi,
    private val apiClient: ApiClient,
    private val devicesApi: DevicesApi,
) {
    val userRole: Flow<String?> = tokenStorage.userRole

    suspend fun saveRole(role: String) {
        tokenStorage.saveRole(role)
    }

    suspend fun register(
        email: String,
        password: String,
        username: String,
        role: UserRole,
    ): Result<Unit> =
        try {
            val response = authApi.registerUser(RegisterRequest(email, username, password))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenStorage.saveToken(
                    accessToken = authResponse.accessToken,
                    refreshToken = authResponse.refreshToken,
                    role = role.name,
                )
                apiClient.setBearerToken(authResponse.accessToken)

                registerDevicePushToken()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Registration failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }

    suspend fun login(email: String, password: String, role: UserRole): Result<Unit> {
        return try {
            if (BuildConfig.DEBUG && email == "admin" && password == "admin") {
                tokenStorage.saveToken(
                    accessToken = "mock_admin_access_token",
                    refreshToken = "mock_admin_refresh_token",
                    role = role.name,
                )
                apiClient.setBearerToken("mock_admin_access_token")
                return Result.success(Unit)
            }

            val response = authApi.loginUser(LoginRequest(email, password))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenStorage.saveToken(
                    accessToken = authResponse.accessToken,
                    refreshToken = authResponse.refreshToken,
                    role = role.name,
                )
                apiClient.setBearerToken(authResponse.accessToken)

                refreshWithRole(role)

                registerDevicePushToken()

                Result.success(Unit)
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    private suspend fun registerDevicePushToken() {
        try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            devicesApi.registerDevice(DeviceRegistrationRequest(token = fcmToken))
            Log.d("SmartJam_Auth", "Device registered for pushes successfully")
        } catch (e: Exception) {
            Log.e("SmartJam_Auth", "Failed to register device for pushes", e)
        }
    }

    suspend fun refreshToken(): Boolean {
        return try {
            val refreshTokenStr = tokenStorage.refreshToken.first() ?: return false

            val storedRole = tokenStorage.userRole.first() ?: UserRole.STUDENT.name
            val apiRole = toApiRole(storedRole)
            val response = authApi.refreshToken(RefreshRequest(refreshTokenStr, apiRole))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenStorage.saveToken(
                    accessToken = authResponse.accessToken,
                    refreshToken = authResponse.refreshToken,
                    role = storedRole,
                )
                apiClient.setBearerToken(authResponse.accessToken)
                true
            } else {
                tokenStorage.clearTokens()
                apiClient.setBearerToken("")
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            tokenStorage.clearTokens()
            apiClient.setBearerToken("")
            false
        }
    }

    suspend fun refreshWithRole(role: UserRole): Boolean {
        return try {
            val refreshTokenStr = tokenStorage.refreshToken.first() ?: return false

            val response =
                authApi.refreshToken(RefreshRequest(refreshTokenStr, toApiRole(role.name)))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenStorage.saveToken(
                    accessToken = authResponse.accessToken,
                    refreshToken = authResponse.refreshToken,
                    role = role.name,
                )
                apiClient.setBearerToken(authResponse.accessToken)
                true
            } else {
                tokenStorage.clearTokens()
                apiClient.setBearerToken("")
                false
            }
        } catch (e: Exception) {
            tokenStorage.clearTokens()
            apiClient.setBearerToken("")
            false
        }
    }

    suspend fun logout() {
        try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            devicesApi.unregisterDevice(DeviceRegistrationRequest(token = fcmToken))
            Log.d("SmartJam_Auth", "Device unregistered successfully")
        } catch (e: Exception) {
            Log.e("SmartJam_Auth", "Failed to unregister device during logout", e)
        }

        tokenStorage.clearTokens()
        apiClient.setBearerToken("")
    }

    suspend fun isAuthenticated(): Boolean {
        return tokenStorage.isAuthenticated()
    }

    suspend fun getAccessToken(): String? {
        return tokenStorage.accessToken.first()
    }

    suspend fun getRefreshToken(): String? {
        return tokenStorage.refreshToken.first()
    }

    suspend fun verifyAuthentication(): Boolean {
        if (BuildConfig.DEBUG) {
            val token = tokenStorage.accessToken.first()
            if (token == "mock_admin_access_token") {
                return true
            }
        }
        val token = tokenStorage.refreshToken.first()
        if (token.isNullOrEmpty()) {
            return false
        }
        return refreshToken()
    }

    private fun toApiRole(role: String): com.smartjam.app.model.UserRole? {
        return try {
            com.smartjam.app.model.UserRole.valueOf(role)
        } catch (e: Exception) {
            null
        }
    }
}
