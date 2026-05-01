package com.smartjam.app.data.model

import com.smartjam.app.domain.model.UserRole

data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String,
    val role: UserRole
)