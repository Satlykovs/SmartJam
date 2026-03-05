package com.smartjam.app

import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartjam.app.ui.screens.login.LoginScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.smartjam.app.data.api.NetworkModule
import com.smartjam.app.data.local.TokenStorage
import com.smartjam.app.domain.repository.AuthRepository
import com.smartjam.app.ui.screens.login.LoginScreen
import com.smartjam.app.ui.screens.login.LoginViewModel
import com.smartjam.app.ui.screens.login.LoginViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        val tokenStorage = TokenStorage(context = this)

        val authApi = NetworkModule.authApi

        val authRepository = AuthRepository(tokenStorage, authApi)

        val factory = LoginViewModelFactory(authRepository)

        setContent {
            val viewModel: LoginViewModel = viewModel(factory = factory)

            LoginScreen(
                viewModel = viewModel,
                onNavigateToHome = {

                    println("SUCCESS: Успешный вход, переходим на Home!")
                },
                onNavigateToRegister = {
                    println("CLICK: Переход на экран регистрации")
                }
            )
        }
    }
}