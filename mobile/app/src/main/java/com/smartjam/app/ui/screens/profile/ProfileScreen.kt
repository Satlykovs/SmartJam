package com.smartjam.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartjam.app.ui.components.AppleLiquidBackground
import com.smartjam.app.ui.components.GlassContainer
import com.smartjam.app.ui.theme.CoreBackground
import com.smartjam.app.ui.theme.ErrorRed

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onBack: () -> Unit, onNavigateToLogin: () -> Unit) {
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(CoreBackground)) {
        AppleLiquidBackground()

        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Spacer(Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding()))

            // Заголовок
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Text("Профиль", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.weight(1f))

            // Мемный контент
            GlassContainer {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("🚲", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Упс! Сюда не смотрите...",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Мы еще не допердолили этот велосипед до конца. Скоро здесь будет готов ваш профиль, статистика и настройки.",
                        color = Color.White.copy(0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.weight(1.2f))

            Button(
                onClick = { viewModel.onLogoutClicked() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(0.2f)),
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = ErrorRed)
                Spacer(Modifier.width(12.dp))
                Text("Выйти из аккаунта", color = ErrorRed, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
