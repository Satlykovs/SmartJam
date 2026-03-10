package com.smartjam.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.smartjam.app.data.api.NetworkModule
import com.smartjam.app.domain.repository.AuthRepository
import com.smartjam.app.domain.repository.RoomRepository
import com.smartjam.app.ui.screens.home.HomeScreen
import com.smartjam.app.ui.screens.home.HomeViewModel
import com.smartjam.app.ui.screens.home.HomeViewModelFactory
import com.smartjam.app.ui.screens.login.LoginScreen
import com.smartjam.app.ui.screens.login.LoginViewModel
import com.smartjam.app.ui.screens.login.LoginViewModelFactory
import com.smartjam.app.ui.screens.register.RegisterScreen
import com.smartjam.app.ui.screens.register.RegisterViewModel
import com.smartjam.app.ui.screens.register.RegisterViewModelFactory

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    object Home : Screen("home_screen")
}

@Composable
fun SmartJamNavGraph(
    navController: NavHostController,
    authRepository: AuthRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {

        composable(route = Screen.Login.route) {
            val viewModel: LoginViewModel = viewModel(
                factory = LoginViewModelFactory(authRepository)
            )

            LoginScreen(
                viewModel = viewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(route = Screen.Register.route) {
            val viewModel: RegisterViewModel = viewModel(
                factory = RegisterViewModelFactory(authRepository)
            )

            RegisterScreen(
                viewModel = viewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Home.route) {
            val roomRepo = RoomRepository(NetworkModule.roomApi)

            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(roomRepo)
            )

            HomeScreen(
                viewModel = viewModel,
                onLogoutClicked = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}