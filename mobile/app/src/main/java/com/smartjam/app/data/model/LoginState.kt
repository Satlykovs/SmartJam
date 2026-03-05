package com.smartjam.app.data.model

data class LoginState (
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)