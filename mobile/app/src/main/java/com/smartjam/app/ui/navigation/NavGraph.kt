package com.smartjam.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.smartjam.app.data.local.TokenStorage
import com.smartjam.app.domain.repository.AuthRepository
import com.smartjam.app.domain.repository.ConnectionRepository
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
import com.smartjam.app.ui.screens.room.RoomScreen
import com.smartjam.app.ui.screens.room.RoomViewModel
import com.smartjam.app.ui.screens.room.RoomViewModelFactory
import com.smartjam.app.ui.theme.BlurCyan
import com.smartjam.app.ui.theme.BlurPurpleDark
import com.smartjam.app.ui.theme.CoreBackground
import java.util.*


sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    object Home : Screen("home_screen")
    object Profile : Screen("profile_screen")
    object Comments : Screen("comments_screen")
    object Room : Screen("room_screen/{connectionId}/{role}") {
        fun createRoute(connectionId: String, role: String) = "room_screen/$connectionId/$role"
    }
}

@Composable
fun SmartJamNavGraph(
    navController: NavHostController,
    authRepository: AuthRepository,
    connectionRepository: ConnectionRepository,
    roomRepository: RoomRepository,
    tokenStorage: TokenStorage,
    startDestination: String = Screen.Login.route
) {

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val appBackground = Brush.verticalGradient(
        colors = listOf(
            CoreBackground,
            Color(0xFF0A0A14),
            BlurPurpleDark.copy(alpha = 0.28f),
            CoreBackground
        )
    )
    val glassShape = RoundedCornerShape(38.dp)
    val glassBarBrush = Brush.verticalGradient(
        colors = listOf(
            CoreBackground.copy(alpha = 0.56f),
            Color(0xFF0A0A14).copy(alpha = 0.36f),
            BlurPurpleDark.copy(alpha = 0.14f),
            Color(0xFF0A0A14).copy(alpha = 0.44f)
        )
    )
    val navBarTransition = rememberInfiniteTransition(label = "nav_bar_bg")
    val navBarPhase by navBarTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "nav_bar_phase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground)
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {

            composable(route = Screen.Login.route) {
                val loginViewModel: LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(authRepository, tokenStorage)
                )

                LoginScreen(
                    viewModel = loginViewModel,
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
                        val role = viewModel.state.value.currentRole.name
                        navController.navigate(Screen.Room.createRoute(connectionId, role))
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(route = Screen.Profile.route) {
                PlaceholderScreen(
                    title = "Профиль",
                    subtitle = "Здесь появится аватар, настройки и данные аккаунта",
                    icon = Icons.Filled.Person
                )
            }

            composable(route = Screen.Comments.route) {
                PlaceholderScreen(
                    title = "Комментарии",
                    subtitle = "Здесь будут сообщения, обсуждения и обратная связь",
                    icon = Icons.Filled.Email
                )
            }

           composable(route = Screen.Room.route) { backStackEntry ->
                val connectionIdStr =
                    backStackEntry.arguments?.getString("connectionId") ?: return@composable
                val roleStr = backStackEntry.arguments?.getString("role") ?: return@composable
                val connectionId = UUID.fromString(connectionIdStr)
                val role = com.smartjam.app.domain.model.UserRole.valueOf(roleStr)

                val viewModel: RoomViewModel = viewModel(
                    factory = RoomViewModelFactory(connectionId, roomRepository)
                )

                RoomScreen(
                    connectionId = connectionId,
                    role = role,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        if (currentRoute != Screen.Login.route && currentRoute != Screen.Register.route) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .fillMaxWidth()
                            .height(88.dp)
                    .clip(glassShape)
                    .shadow(
                                elevation = 28.dp,
                        shape = glassShape,
                                ambientColor = Color.Black.copy(alpha = 0.12f),
                                spotColor = Color.Black.copy(alpha = 0.22f)
                    )
                    .background(glassBarBrush)
            ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.08f),
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.03f)
                                        )
                                    )
                                )
                        )

                NavigationBar(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent),
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    windowInsets = androidx.compose.foundation.layout.WindowInsets(0)
                ) {
                    val items = listOf(Screen.Home, Screen.Profile, Screen.Comments)
                    items.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route)
                                }
                            },
                            icon = {
                                when (screen) {
                                    is Screen.Home -> Icon(
                                        imageVector = Icons.Filled.Home,
                                        contentDescription = "Home"
                                    )
                                    is Screen.Profile -> Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Profile"
                                    )
                                    is Screen.Comments -> Icon(
                                        imageVector = Icons.Filled.Email,
                                        contentDescription = "Comments"
                                    )
                                    else -> {}
                                }
                            },
                            label = {
                                Text(
                                    text = when (screen) {
                                        is Screen.Home -> "Комнаты"
                                        is Screen.Profile -> "Профиль"
                                        is Screen.Comments -> "Чаты"
                                        else -> ""
                                    },
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = Color.White.copy(alpha = 0.45f),
                                selectedTextColor = Color.White,
                                unselectedTextColor = Color.White.copy(alpha = 0.48f),
                                indicatorColor = Color.White.copy(alpha = 0.10f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val background = Brush.radialGradient(
        colors = listOf(
            BlurPurpleDark.copy(alpha = 0.34f),
            CoreBackground,
            CoreBackground
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.06f),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.12f)
            ),
            tonalElevation = 0.dp,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    BlurCyan.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.68f)
                )
            }
        }
    }
}

