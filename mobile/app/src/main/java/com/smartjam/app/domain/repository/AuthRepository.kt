package com.smartjam.app.domain.repository

import androidx.compose.animation.core.animateDpAsState
import com.smartjam.app.data.api.AuthApi
import com.smartjam.app.data.api.NetworkModule
import com.smartjam.app.data.local.TokenStorage
import com.smartjam.app.data.model.LoginResponce
import com.smartjam.app.data.model.LoginRequest
import com.smartjam.app.data.model.RefreshRequest
import kotlinx.coroutines.flow.first

class AuthRepository (
    private val tokenStorage: TokenStorage,
    private val authApi: AuthApi = NetworkModule.authApi
) {


    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val responce = authApi.login(LoginRequest(email, password))

            tokenStorage.saveToken(
                accessToken = responce.accessToken,
                refreshToken = responce.refreshToken,
                accessExpiredAt = responce.accessExpiresAt,
                refreshExpiredAt = responce.refreshExpiredAt
            )
            Result.success(Unit)
        } catch (e: Exception){
            Result.failure(e)
        }
    }

    suspend fun refreshToken(): Boolean {
        return try{
            if (tokenStorage.isRefreshTokenExpired()){
                tokenStorage.clearTokens()
                return false
            }

            val refreshToken = tokenStorage.refreshToken.first()

            if (refreshToken == null){
                return false
            }
            val responce = authApi.refresh(RefreshRequest(refreshToken))

            tokenStorage.saveToken(
                accessToken = responce.accessToken,
                refreshToken = responce.refreshToken,
                accessExpiredAt = responce.accessExpiresAt,
                refreshExpiredAt = responce.refreshExpiredAt
            )

            return true

        } catch (e: Exception){
            tokenStorage.clearTokens()
            return false
        }
    }

    suspend fun logout() {
        tokenStorage.clearTokens()
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

    suspend fun getAccessTokenExpiredIn(): Long?{
        val accessExpires = tokenStorage.accessExpiredAt.first()

        if (accessExpires == null){
            return null
        }

        val currTime = System.currentTimeMillis()
        return accessExpires - currTime
    }

    suspend fun getRefreshTokenExpiredIn(): Long?{
        val refreshExpires = tokenStorage.refreshExpiredAt.first()

        if (refreshExpires == null){
            return null
        }

        val currTime = System.currentTimeMillis()
        return refreshExpires - currTime
    }

}