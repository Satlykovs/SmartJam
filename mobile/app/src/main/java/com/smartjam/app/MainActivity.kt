package com.smartjam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.openapitools.client.infrastructure.ApiClient
import okhttp3.OkHttpClient
import com.smartjam.app.api.AuthApi
import com.smartjam.app.api.ConnectionsApi
import com.smartjam.app.data.api.AuthAuthenticator

import com.smartjam.app.data.local.SmartJamDatabase
import com.smartjam.app.data.local.TokenStorage
import com.smartjam.app.domain.repository.AuthRepository
import com.smartjam.app.domain.repository.ConnectionRepository
import com.smartjam.app.ui.navigation.Screen
import com.smartjam.app.ui.navigation.SmartJamNavGraph
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenStorage = TokenStorage(context = this)

        val appDatabase = Room.databaseBuilder(
            applicationContext,
            SmartJamDatabase::class.java,
            "smartjam_database"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

        val baseUrl = BuildConfig.BASE_URL
        val authenticator = AuthAuthenticator(tokenStorage, baseUrl)

        val okHttpClientBuilder = OkHttpClient.Builder()
            .authenticator(authenticator)

        val apiClient = ApiClient(
            baseUrl = baseUrl,
            okHttpClientBuilder = okHttpClientBuilder,
            authNames = arrayOf("bearerAuth")
        )
        authenticator.apiClient = apiClient

        val token = runBlocking { tokenStorage.accessToken.first() }
        if (token != null) {
            apiClient.setBearerToken(token)
        }

        val authApi = apiClient.createService(AuthApi::class.java)
        val connectionsApi = apiClient.createService(ConnectionsApi::class.java)

        val authRepository = AuthRepository(tokenStorage, authApi, apiClient)
        val connectionRepository = ConnectionRepository(connectionsApi, appDatabase.connectionDao())

        setContent {
            val navController = rememberNavController()
            var startDestination by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                val tokenExists = tokenStorage.isAuthenticated()
                startDestination = if (tokenExists) {
                    val isValid = try {
                        authRepository.verifyAuthentication()
                    } catch (e: Exception) {
                        false
                    }
                    if (isValid) Screen.Home.route else Screen.Login.route
                } else {
                    Screen.Login.route
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF05050A)
            ) {
                if (startDestination != null) {
                    SmartJamNavGraph(
                        navController = navController,
                        authRepository = authRepository,
                        connectionRepository = connectionRepository,
                        tokenStorage = tokenStorage,
                        startDestination = startDestination!!
                    )
                }
            }
        }
    }
}