package com.smartjam.app.data.model

data class LoginResponce (
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAt: Long,
    val refreshExpiredAt: Long
)