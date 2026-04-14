package com.smartjam.app.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartjam.app.domain.model.Connection
import com.smartjam.app.domain.model.UserRole
import com.smartjam.app.ui.components.AppleGlassTextField
import com.smartjam.app.ui.components.AppleLiquidBackground
import com.smartjam.app.ui.components.GlassContainer
import com.smartjam.app.ui.components.GoldenStringsButton
import com.smartjam.app.ui.theme.BrandCyan
import com.smartjam.app.ui.theme.BrandGold
import com.smartjam.app.ui.theme.ErrorRed

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToRoom: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToLogin -> onNavigateToLogin()
                is HomeEvent.NavigateToRoom -> onNavigateToRoom(event.connectionId)
                is HomeEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF05050A))) {
        AppleLiquidBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            HomeHeader(
                role = state.currentRole,
                isLoading = state.isLoading,
                onLogout = viewModel::onLogoutClicked,
                onSync = viewModel::syncNetworkData,
                onToggleDebugRole = viewModel::toggleDebugRole
            )

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage!!,
                    color = Color(0xFFFF5252),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    fontSize = 14.sp
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.currentRole == UserRole.TEACHER) {
                    item {
                        TeacherInviteSection(
                            code = state.teacherGeneratedCode,
                            isLoading = state.isLoading,
                            onGenerate = viewModel::onGenerateCodeClicked
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    item { SectionTitle("Мои ученики") }

                } else {
                    item {
                        StudentJoinSection(
                            inputValue = state.inviteCodeInput,
                            isLoading = state.isLoading,
                            onInputChange = viewModel::onInviteCodeInputChanged,
                            onJoin = {
                                keyboard?.hide()
                                viewModel.onJoinRoomClicked()
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    item { SectionTitle("Мои преподаватели") }
                }

                if (state.connections.isEmpty()) {
                    item {
                        Text(
                            text = "Список пуст",
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                } else {
                    items(state.connections) { connection ->
                        ActiveConnectionCard(
                            connection = connection,
                            onClick = { viewModel.onConnectionClicked(connection.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    role: UserRole,
    isLoading: Boolean,
    onLogout: () -> Unit,
    onSync: () -> Unit,
    onToggleDebugRole: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.clickable { onToggleDebugRole() }) {
            Text(
                text = "SmartJam",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = if (role == UserRole.TEACHER) "Режим преподавателя" else "Режим ученика",
                fontSize = 12.sp,
                color = BrandCyan
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(16.dp))
            }

            IconButton(onClick = onLogout) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Выйти", tint = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White.copy(alpha = 0.8f)
    )
}

@Composable
private fun TeacherInviteSection(code: String?, isLoading: Boolean, onGenerate: () -> Unit) {
    GlassContainer {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Код приглашения", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            if (code != null) {
                Text(text = code, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = BrandGold, letterSpacing = 4.sp)
                Spacer(modifier = Modifier.height(16.dp))
            }

            GoldenStringsButton(
                text = if (code == null) "Сгенерировать код" else "Обновить код",
                onClick = onGenerate,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )
        }
    }
}


@Composable
private fun StudentJoinSection(inputValue: String, isLoading: Boolean, onInputChange: (String) -> Unit, onJoin: () -> Unit) {
    GlassContainer {
        Column {
            Text("Присоединиться к классу", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            AppleGlassTextField(
                value = inputValue,
                onValueChange = onInputChange,
                hint = "Введите код (напр. A1B2C)",
                icon = Icons.Default.Person,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onJoin() }),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))

            GoldenStringsButton(
                text = "Отправить заявку",
                onClick = onJoin,
                enabled = !isLoading && inputValue.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )
        }
    }
}

@Composable
private fun PendingRequestCard(connection: Connection, onAccept: (String) -> Unit, onReject: (String) -> Unit) {
    GlassContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Новая заявка", color = BrandGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(connection.peerName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
            Row {
                IconButton(onClick = { onReject(connection.id) }, modifier = Modifier.background(ErrorRed.copy(0.2f), RoundedCornerShape(12.dp))) {
                    Icon(Icons.Default.Close, contentDescription = "Отклонить", tint = ErrorRed)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { onAccept(connection.id) }, modifier = Modifier.background(BrandCyan.copy(0.2f), RoundedCornerShape(12.dp))) {
                    Icon(Icons.Default.Check, contentDescription = "Принять", tint = BrandCyan)
                }
            }
        }
    }
}

@Composable
private fun ActiveConnectionCard(connection: Connection, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(connection.peerName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Нажмите, чтобы открыть", color = Color.White.copy(0.5f), fontSize = 13.sp)
            }
        }
    }
}

