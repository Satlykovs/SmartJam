package com.smartjam.app.domain.repository

import com.smartjam.app.api.AuthApi
import com.smartjam.app.model.LoginRequest
import com.smartjam.app.model.RefreshRequest
import com.smartjam.app.model.RegisterRequest
import com.smartjam.app.data.local.TokenStorage
import com.smartjam.app.domain.model.UserRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.openapitools.client.infrastructure.ApiClient
import com.smartjam.app.BuildConfig

class AuthRepository (
    private val tokenStorage: TokenStorage,
    private val authApi: AuthApi,
    private val apiClient: ApiClient
) {

    suspend fun register(email: String, password: String, username: String, role: UserRole): Result<Unit> {
        return try {
            val response = authApi.registerUser(RegisterRequest(email, username, password))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenStorage.saveToken(
                    accessToken = authResponse.accessToken,
                    refreshToken = authResponse.refreshToken
                )
                apiClient.setBearerToken(authResponse.accessToken)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Registration failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e;
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            if (BuildConfig.DEBUG && email == "admin" && password == "admin") {
                tokenStorage.saveToken(
                    accessToken = "mock_admin_access_token",
                    refreshToken = "mock_admin_refresh_token"
                )
                apiClient.setBearerToken("mock_admin_access_token")
                return Result.success(Unit)
            }

            val response = authApi.loginUser(LoginRequest(email, password))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenStorage.saveToken(
                    accessToken = authResponse.accessToken,
                    refreshToken = authResponse.refreshToken
                )
                apiClient.setBearerToken(authResponse.accessToken)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception){
            if (e is CancellationException) throw e;
            Result.failure(e)
        }
    }

    suspend fun refreshToken(): Boolean {
        return try{
            val refreshTokenStr = tokenStorage.refreshToken.first()

            if (refreshTokenStr == null){
                return false
            }
            val response = authApi.refreshToken(RefreshRequest(refreshTokenStr))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenStorage.saveToken(
                    accessToken = authResponse.accessToken,
                    refreshToken = authResponse.refreshToken
                )
                apiClient.setBearerToken(authResponse.accessToken)
                return true
            } else {
                tokenStorage.clearTokens()
                apiClient.setBearerToken("")
                return false
            }

        } catch (e: Exception){
            if (e is CancellationException) {
                throw e
            }
            else{
                tokenStorage.clearTokens()
                apiClient.setBearerToken("")
                return false
            }

        }
    }

    suspend fun logout() {
        tokenStorage.clearTokens()
        apiClient.setBearerToken("")
    }

    suspend fun isAuthenticated(): Boolean{
        return tokenStorage.isAuthenticated()
    }

    suspend fun getAccessToken(): String?{
        return tokenStorage.accessToken.first()
    }

    suspend fun getRefreshToken(): String?{
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
}