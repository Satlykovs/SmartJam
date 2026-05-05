package com.smartjam.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.smartjam.app.data.local.TokenStorage
import com.smartjam.app.domain.repository.AuthRepository
import com.smartjam.app.domain.repository.ConnectionRepository
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
    object Room : Screen("room_screen")
}

@Composable
fun SmartJamNavGraph(
    navController: NavHostController,
    authRepository: AuthRepository,
    connectionRepository: ConnectionRepository,
    tokenStorage: TokenStorage,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
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
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(connectionRepository, authRepository)
            )

            HomeScreen(
                viewModel = viewModel,
                onNavigateToRoom = { connectionId ->
                    navController.navigate(Screen.Room.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

    }
}