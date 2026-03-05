package com.smartjam.app.data.api

import com.smartjam.app.data.model.LoginResponce
import com.smartjam.app.data.model.LoginRequest
import com.smartjam.app.data.model.RefreshRequest

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponce

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): LoginResponce
}