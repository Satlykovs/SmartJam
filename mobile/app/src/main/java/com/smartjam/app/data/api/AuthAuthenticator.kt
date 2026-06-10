package com.smartjam.app.data.api

import com.smartjam.app.api.AuthApi
import com.smartjam.app.data.local.TokenStorage
import com.smartjam.app.model.RefreshRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class AuthAuthenticator(
    private val tokenStorage: TokenStorage,
    private val authApiProvider: dagger.Lazy<AuthApi>,
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.responseCount > 2) return null

        return runBlocking {
            mutex.withLock {
                val currentToken = tokenStorage.accessToken.first()
                val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

                if (currentToken != null && currentToken != requestToken) {
                    return@runBlocking response.request
                        .newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                val refreshToken = tokenStorage.refreshToken.first() ?: return@runBlocking null
                val storedRole = tokenStorage.userRole.first()

                try {
                    val refreshResponse =
                        authApiProvider
                            .get()
                            .refreshToken(
                                RefreshRequest(refreshToken, storedRole?.let { toApiRole(it) })
                            )

                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val newAuthResponse = refreshResponse.body()!!
                        tokenStorage.saveToken(
                            accessToken = newAuthResponse.accessToken,
                            refreshToken = newAuthResponse.refreshToken,
                            role = storedRole,
                        )

                        response.request
                            .newBuilder()
                            .header("Authorization", "Bearer ${newAuthResponse.accessToken}")
                            .build()
                    } else {
                        tokenStorage.clearTokens()
                        null
                    }
                } catch (e: Exception) {
                    tokenStorage.clearTokens()
                    null
                }
            }
        }
    }

    private val Response.responseCount: Int
        get() {
            var result = 1
            var priorResponse = this.priorResponse
            while (priorResponse != null) {
                result++
                priorResponse = priorResponse.priorResponse
            }
            return result
        }

    private fun toApiRole(role: String): com.smartjam.app.model.UserRole? {
        return try {
            com.smartjam.app.model.UserRole.valueOf(role)
        } catch (e: Exception) {
            null
        }
    }
}
