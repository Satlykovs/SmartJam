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
import com.smartjam.app.api.AuthApi
import com.smartjam.app.api.ConnectionsApi
import com.smartjam.app.api.AssignmentsApi
import com.smartjam.app.api.SubmissionsApi
import com.smartjam.app.data.api.AuthAuthenticator
import com.smartjam.app.data.api.InstantAdapter
import com.smartjam.app.data.local.SmartJamDatabase
import com.smartjam.app.data.local.TokenStorage
import com.smartjam.app.data.local.AudioFileStore
import com.smartjam.app.domain.repository.AuthRepository
import com.smartjam.app.domain.repository.ConnectionRepository
import com.smartjam.app.domain.repository.RoomRepository
import com.smartjam.app.ui.navigation.Screen
import com.smartjam.app.ui.navigation.SmartJamNavGraph
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.openapitools.client.infrastructure.ApiClient

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

        val serializerBuilder = org.openapitools.client.infrastructure.Serializer.gsonBuilder
            .registerTypeAdapter(Instant::class.java, InstantAdapter())

        val okHttpClientBuilder = OkHttpClient.Builder()
            .authenticator(authenticator)
            .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor { chain ->
                val original = chain.request()
                val token = runBlocking { tokenStorage.accessToken.first() }
                if (token != null && original.header("Authorization") == null) {
                    val request = original.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                    chain.proceed(request)
                } else {
                    chain.proceed(original)
                }
            }

        val apiClient = ApiClient(
            baseUrl = baseUrl,
            okHttpClientBuilder = okHttpClientBuilder,
            serializerBuilder = serializerBuilder,
            authNames = arrayOf("bearerAuth")
        )
        authenticator.apiClient = apiClient

        val token = runBlocking { tokenStorage.accessToken.first() }
        if (token != null) {
            apiClient.setBearerToken(token)
        }

        val authApi = apiClient.createService(AuthApi::class.java)
        val connectionsApi = apiClient.createService(ConnectionsApi::class.java)
        val assignmentsApi = apiClient.createService(AssignmentsApi::class.java)
        val submissionsApi = apiClient.createService(SubmissionsApi::class.java)

        val authRepository = AuthRepository(tokenStorage, authApi, apiClient)
        val connectionRepository = ConnectionRepository(connectionsApi, appDatabase.connectionDao())
        val roomRepository = RoomRepository(
            assignmentsApi = assignmentsApi,
            submissionsApi = submissionsApi,
            assignmentDao = appDatabase.assignmentDao(),
            submissionResultDao = appDatabase.submissionResultDao(),
            audioFileStore = AudioFileStore(applicationContext)
        )

        lifecycleScope.launch {
            tokenStorage.accessToken.collect { newToken ->
                if (newToken != null) {
                    apiClient.setBearerToken(newToken)
                }
            }
        }

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

            LaunchedEffect(startDestination) {
                if (startDestination != null) {
                    tokenStorage.refreshToken.collect { token ->
                        if (token.isNullOrEmpty()) {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
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
                        roomRepository = roomRepository,
                        tokenStorage = tokenStorage,
                        startDestination = startDestination!!
                    )
                }
            }
        }
    }
}