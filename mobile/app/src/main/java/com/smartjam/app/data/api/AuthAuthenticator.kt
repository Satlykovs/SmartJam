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
import org.openapitools.client.infrastructure.ApiClient

class AuthAuthenticator(
    private val tokenStorage: TokenStorage,
    private val baseUrl: String
) : Authenticator {

    private val mutex = Mutex()
    var apiClient: ApiClient? = null

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.responseCount > 2) return null

        return runBlocking {
            mutex.withLock {
                val currentToken = tokenStorage.accessToken.first()
                val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
                if (currentToken != null && currentToken != requestToken) {
                    return@runBlocking response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                val refreshToken = tokenStorage.refreshToken.first() ?: return@runBlocking null

                val authApiClient = ApiClient(baseUrl = baseUrl)
                val authApi = authApiClient.createService(AuthApi::class.java)

                try {
                    val refreshResponse = authApi.refreshToken(RefreshRequest(refreshToken))

                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val newAuthResponse = refreshResponse.body()!!
                        tokenStorage.saveToken(
                            accessToken = newAuthResponse.accessToken,
                            refreshToken = newAuthResponse.refreshToken
                        )

                        apiClient?.setBearerToken(newAuthResponse.accessToken)

                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${newAuthResponse.accessToken}")
                            .build()
                    } else {
                        tokenStorage.clearTokens()
                        apiClient?.setBearerToken("")
                        null
                    }
                } catch (e: Exception) {
                    tokenStorage.clearTokens()
                    apiClient?.setBearerToken("")
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
}
