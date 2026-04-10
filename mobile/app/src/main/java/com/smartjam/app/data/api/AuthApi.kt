package com.smartjam.app.data.api

import com.smartjam.app.data.model.LoginRequest
import com.smartjam.app.data.model.LoginResponse
import com.smartjam.app.data.model.RefreshRequest
import com.smartjam.app.data.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): LoginResponse

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): LoginResponse
}