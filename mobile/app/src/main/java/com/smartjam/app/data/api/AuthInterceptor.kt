package com.smartjam.app.data.api

import com.smartjam.app.data.local.TokenStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor (
    private val tokenStorage: TokenStorage
): Interceptor{
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            tokenStorage.accessToken.first()
        }
        val originalRequest = chain.request()

        val requestBuilder = originalRequest.newBuilder()
        if (token != null){
            requestBuilder.addHeader("Authorization",token)
        }

        val newRequest = requestBuilder.build()
        return chain.proceed(newRequest)
    }
}