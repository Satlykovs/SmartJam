package com.smartjam.app.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartjam.app.ui.screens.login.AppleGlassTextField
import com.smartjam.app.ui.screens.login.AppleLiquidBackground
import com.smartjam.app.ui.screens.login.GoldenStringsButton

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onLogoutClicked: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.RoomJoined -> {
                    Toast.makeText(context, "Успешно добавлено!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05050A))
    ) {
        AppleLiquidBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Мои классы",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                GoldenStringsButton(
                    text = "Выход",
                    onClick = onLogoutClicked,
                    modifier = Modifier.width(100.dp).height(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Присоединиться к учителю",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            AppleGlassTextField(
                value = state.inviteCodeInput,
                onValueChange = { viewModel.onInviteCodeChanged(it) },
                hint = "Введите инвайт-код (напр. A1B2C)",
                icon = Icons.Default.Add,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboard?.hide()
                    viewModel.onJoinRoomClicked()
                })
            )

            Spacer(modifier = Modifier.height(16.dp))

            GoldenStringsButton(
                text = if (state.isLoading) "Поиск..." else "Присоединиться",
                onClick = {
                    keyboard?.hide()
                    viewModel.onJoinRoomClicked()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.errorMessage != null) {
                    Text(text = state.errorMessage!!, color = Color(0xFFFF5252), fontSize = 14.sp)
                }
                if (state.successMessage != null) {
                    Text(text = state.successMessage!!, color = Color(0xFF00E5FF), fontSize = 14.sp)
                }
            }
        }
    }
}