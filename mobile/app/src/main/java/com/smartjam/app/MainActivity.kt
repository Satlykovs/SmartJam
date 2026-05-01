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
import com.smartjam.app.data.api.AuthApi
import com.smartjam.app.data.api.NetworkModule
import com.smartjam.app.data.api.SmartJamApi
import com.smartjam.app.data.local.SmartJamDatabase
import com.smartjam.app.data.local.TokenStorage
import com.smartjam.app.domain.repository.AuthRepository
import com.smartjam.app.domain.repository.ConnectionRepository
import com.smartjam.app.ui.navigation.SmartJamNavGraph
import kotlin.jvm.java

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenStorage = TokenStorage(context = this)

        val appDatabase = Room.databaseBuilder(
            applicationContext,
            SmartJamDatabase::class.java,
            "smartjam_database"
        ).build()


        val retrofit = NetworkModule.createRetrofit(tokenStorage)
        val smartJamApi = retrofit.create(SmartJamApi::class.java)
        val authApi = retrofit.create(AuthApi::class.java)

        val authRepository = AuthRepository(tokenStorage, authApi)
        val connectionRepository = ConnectionRepository(smartJamApi, appDatabase.connectionDao())

        setContent {
            val navController = rememberNavController()

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF05050A)
            ) {

                SmartJamNavGraph(
                    navController = navController,
                    authRepository = authRepository,
                    connectionRepository = connectionRepository,
                    tokenStorage = tokenStorage
                )
            }
        }
    }
}