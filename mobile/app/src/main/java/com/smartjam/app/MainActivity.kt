package com.smartjam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.smartjam.app.ui.navigation.SmartJamNavGraph
import com.smartjam.app.ui.screens.main.MainViewModel
import com.smartjam.app.ui.theme.SmartJamTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartJamTheme {
                val mainViewModel: MainViewModel = hiltViewModel()
                val startDestination by mainViewModel.startDestination.collectAsState()
                val navController = rememberNavController()

                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF05050A)) {
                    if (startDestination == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    } else {
                        SmartJamNavGraph(
                            navController = navController,
                            startDestination = startDestination!!,
                        )
                    }
                }
            }
        }
    }
}
