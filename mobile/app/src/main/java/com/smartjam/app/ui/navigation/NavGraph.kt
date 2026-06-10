package com.smartjam.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.smartjam.app.ui.screens.home.HomeScreen
import com.smartjam.app.ui.screens.home.HomeViewModel
import com.smartjam.app.ui.screens.login.LoginScreen
import com.smartjam.app.ui.screens.login.LoginViewModel
import com.smartjam.app.ui.screens.register.RegisterScreen
import com.smartjam.app.ui.screens.register.RegisterViewModel
import com.smartjam.app.ui.screens.room.AssignmentDetailsScreen
import com.smartjam.app.ui.screens.room.RoomScreen
import com.smartjam.app.ui.screens.room.RoomViewModel
import com.smartjam.app.ui.screens.submission.SubmissionDetailScreen
import com.smartjam.app.ui.theme.BlurPurpleDark
import com.smartjam.app.ui.theme.CoreBackground
import java.util.*

sealed class Screen(val route: String) {

    object SubmissionDetail :
        Screen("submission_detail/{connectionId}/{assignmentId}/{submissionId}") {
        fun createRoute(connectionId: String, assignmentId: String, submissionId: String) =
            "submission_detail/$connectionId/$assignmentId/$submissionId"
    }

    object Login : Screen("login_screen")

    object Register : Screen("register_screen")

    object Home : Screen("home_screen")

    object Profile : Screen("profile_screen")

    object Comments : Screen("comments_screen")

    object Room : Screen("room_screen/{connectionId}/{role}") {
        fun createRoute(connectionId: String, role: String) = "room_screen/$connectionId/$role"
    }

    object AssignmentDetails : Screen("assignment_screen/{connectionId}/{assignmentId}/{role}") {
        fun createRoute(connectionId: String, assignmentId: String, role: String) =
            "assignment_screen/$connectionId/$assignmentId/$role"
    }
}

@Composable
fun SmartJamNavGraph(navController: NavHostController, startDestination: String) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val appBackground =
        Brush.verticalGradient(
            colors =
                listOf(
                    CoreBackground,
                    Color(0xFF0A0A14),
                    BlurPurpleDark.copy(alpha = 0.28f),
                    CoreBackground,
                )
        )
    val glassBarBrush =
        Brush.verticalGradient(
            colors =
                listOf(
                    CoreBackground.copy(alpha = 0.56f),
                    Color(0xFF0A0A14).copy(alpha = 0.36f),
                    BlurPurpleDark.copy(alpha = 0.14f),
                    Color(0xFF0A0A14).copy(alpha = 0.44f),
                )
        )

    Box(modifier = Modifier.fillMaxSize().background(appBackground)) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(route = Screen.Login.route) {
                val viewModel: LoginViewModel = hiltViewModel()
                LoginScreen(
                    viewModel = viewModel,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                )
            }

            composable(route = Screen.Register.route) {
                val viewModel: RegisterViewModel = hiltViewModel()
                RegisterScreen(
                    viewModel = viewModel,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(route = Screen.Home.route) {
                val viewModel: HomeViewModel = hiltViewModel()
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
                    },
                )
            }

            composable(route = Screen.Profile.route) {
                PlaceholderScreen(
                    title = "Профиль",
                    subtitle = "Настройки аккаунта",
                    icon = Icons.Filled.Person,
                )
            }

            composable(route = Screen.Comments.route) {
                PlaceholderScreen(
                    title = "Комментарии",
                    subtitle = "Ваши чаты",
                    icon = Icons.Filled.Email,
                )
            }

            composable(route = Screen.Room.route) {
                val viewModel: RoomViewModel = hiltViewModel()
                val connectionId = UUID.fromString(it.arguments?.getString("connectionId"))
                val role =
                    com.smartjam.app.domain.model.UserRole.valueOf(
                        it.arguments?.getString("role") ?: "STUDENT"
                    )

                RoomScreen(
                    connectionId = connectionId,
                    role = role,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenAssignment = { assignmentId ->
                        navController.navigate(
                            Screen.AssignmentDetails.createRoute(
                                connectionId.toString(),
                                assignmentId.toString(),
                                role.name,
                            )
                        )
                    },
                )
            }

            composable(route = Screen.AssignmentDetails.route) { backStackEntry ->
                val viewModel: RoomViewModel = hiltViewModel()
                val connectionId =
                    UUID.fromString(backStackEntry.arguments?.getString("connectionId"))
                val assignmentId =
                    UUID.fromString(backStackEntry.arguments?.getString("assignmentId"))
                val role =
                    com.smartjam.app.domain.model.UserRole.valueOf(
                        backStackEntry.arguments?.getString("role") ?: "STUDENT"
                    )

                AssignmentDetailsScreen(
                    assignmentId = assignmentId,
                    connectionId = connectionId,
                    role = role,
                    viewModel = viewModel,
                    navController = navController,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(route = Screen.SubmissionDetail.route) { backStackEntry ->
                val viewModel: RoomViewModel = hiltViewModel()
                val connectionId =
                    UUID.fromString(backStackEntry.arguments?.getString("connectionId"))
                val assignmentId =
                    UUID.fromString(backStackEntry.arguments?.getString("assignmentId"))
                val submissionId =
                    UUID.fromString(backStackEntry.arguments?.getString("submissionId"))

                SubmissionDetailScreen(
                    submissionId = submissionId,
                    assignmentId = assignmentId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        if (currentRoute != Screen.Login.route && currentRoute != Screen.Register.route) {
            BottomNavigationBar(navController, currentRoute, glassBarBrush)
        }
    }
}

@Composable
fun BoxScope.BottomNavigationBar(
    navController: NavHostController,
    currentRoute: String?,
    brush: Brush,
) {
    val glassShape = RoundedCornerShape(38.dp)
    Box(
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth()
                .height(88.dp)
                .clip(glassShape)
                .shadow(elevation = 28.dp, shape = glassShape)
                .background(brush)
    ) {
        NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
            val items = listOf(Screen.Home, Screen.Profile, Screen.Comments)
            items.forEach { screen ->
                NavigationBarItem(
                    selected = currentRoute == screen.route,
                    onClick = {
                        navController.navigate(screen.route) { popUpTo(Screen.Home.route) }
                    },
                    icon = {
                        val icon =
                            when (screen) {
                                is Screen.Home -> Icons.Filled.Home
                                is Screen.Profile -> Icons.Filled.Person
                                is Screen.Comments -> Icons.Filled.Email
                                else -> Icons.Filled.Home
                            }
                        Icon(imageVector = icon, contentDescription = null)
                    },
                    label = {
                        Text(
                            text =
                                when (screen) {
                                    is Screen.Home -> "Комнаты"
                                    is Screen.Profile -> "Профиль"
                                    is Screen.Comments -> "Чаты"
                                    else -> ""
                                },
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    colors =
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.White.copy(alpha = 0.45f),
                            indicatorColor = Color.White.copy(alpha = 0.10f),
                        ),
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    val background =
        Brush.radialGradient(
            colors = listOf(BlurPurpleDark.copy(alpha = 0.34f), CoreBackground, CoreBackground)
        )
    Box(
        modifier = Modifier.fillMaxSize().background(background).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.06f),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier =
                        Modifier.size(76.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = Color.White.copy(alpha = 0.68f))
            }
        }
    }
}
