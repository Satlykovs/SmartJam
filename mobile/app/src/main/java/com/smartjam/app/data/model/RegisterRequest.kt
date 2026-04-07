package com.smartjam.app.data.model

data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String,
    val role: String
)