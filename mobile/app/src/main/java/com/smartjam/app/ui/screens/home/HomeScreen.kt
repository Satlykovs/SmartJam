package com.smartjam.app.ui.screens.home

import android.Manifest
import android.content.ClipData
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.smartjam.app.domain.model.Connection
import com.smartjam.app.domain.model.UserRole
import com.smartjam.app.ui.components.*
import com.smartjam.app.ui.theme.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToRoom: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refreshTicker.collect { viewModel.syncNetworkData() }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToLogin -> onNavigateToLogin()
                is HomeEvent.NavigateToRoom -> onNavigateToRoom(event.connectionId)
                is HomeEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastIndex ->
                viewModel.onListScrolled(lastIndex, listState.layoutInfo.totalItemsCount)
            }
    }

    Box(Modifier.fillMaxSize().background(CoreBackground)) {
        AppleLiquidBackground()

        Column(Modifier.fillMaxSize()) {
            HomeHeader(
                state.currentRole,
                state.isLoading,
                viewModel::onLogoutClicked,
                viewModel::toggleDebugRole,
            )

            if (state.errorMessage != null) {
                Text(
                    state.errorMessage!!,
                    color = ErrorRed,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp, 24.dp, 24.dp, 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (state.currentRole == UserRole.TEACHER) {
                    item {
                        TeacherInviteSection(
                            state.teacherGeneratedCode,
                            state.isLoading,
                            viewModel::onGenerateCodeClicked,
                        )
                    }
                    item { SectionTitle("Мои ученики") }
                } else {
                    item {
                        StudentJoinSection(
                            state.inviteCodeInput,
                            state.isLoading,
                            viewModel::onInviteCodeInputChanged,
                        ) {
                            keyboard?.hide()
                            viewModel.onJoinRoomClicked()
                        }
                    }
                    item { SectionTitle("Мои преподаватели") }
                }

                if (state.connections.isEmpty() && !state.isLoading) {
                    item { Text("Список пуст", color = Color.White.copy(0.4f)) }
                } else {
                    items(state.connections) {
                        ActiveConnectionCard(it) { viewModel.onConnectionClicked(it.id) }
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
    onToggleRole: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.clickable { onToggleRole() }) {
            Text("SmartJam", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                if (role == UserRole.TEACHER) "Преподаватель" else "Ученик",
                fontSize = 12.sp,
                color = BrandCyan,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), Color.White, 2.dp)
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Default.ExitToApp, null, tint = Color.White.copy(0.7f))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(0.8f))
}

@Composable
private fun TeacherInviteSection(code: String?, isLoading: Boolean, onGenerate: () -> Unit) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    GlassContainer {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Код приглашения", color = Color.White.copy(0.6f), fontSize = 14.sp)

            if (code != null) {
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(0.05f))
                            .clickable {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        ClipEntry(ClipData.newPlainText("invite_code", code))
                                    )
                                    Toast.makeText(context, "Код скопирован!", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                            .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = code,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGold,
                        letterSpacing = 4.sp,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Нажмите, чтобы скопировать",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
            }

            GoldenStringsButton(
                text = if (code == null) "Создать код" else "Обновить код",
                onClick = onGenerate,
                enabled = !isLoading,
            )
        }
    }
}

@Composable
private fun StudentJoinSection(
    value: String,
    isLoading: Boolean,
    onValueChange: (String) -> Unit,
    onJoin: () -> Unit,
) {
    GlassContainer {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Присоединиться", color = Color.White.copy(0.6f), fontSize = 14.sp)
            AppleGlassTextField(
                value,
                onValueChange,
                "Введите код",
                Icons.Default.Person,
                enabled = !isLoading,
            )
            GoldenStringsButton(
                "Отправить заявку",
                onJoin,
                enabled = !isLoading && value.isNotBlank(),
            )
        }
    }
}

@Composable
private fun ActiveConnectionCard(connection: Connection, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(0.05f))
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!connection.peerAvatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model =
                        ImageRequest.Builder(LocalContext.current)
                            .data(connection.peerAvatarUrl)
                            .crossfade(true)
                            .build(),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)),
                )
            } else {
                Box(
                    Modifier.size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, null, tint = Color.White)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    connection.peerName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Нажмите, чтобы открыть", color = Color.White.copy(0.5f), fontSize = 13.sp)
            }
        }
    }
}
