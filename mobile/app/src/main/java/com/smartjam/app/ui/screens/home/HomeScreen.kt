package com.smartjam.app.ui.screens.home

import android.Manifest
import android.content.ClipData
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.smartjam.app.domain.model.Connection
import com.smartjam.app.model.UserRole
import com.smartjam.app.ui.components.*
import com.smartjam.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToRoom: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToProfile: () -> Unit,
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
                role = state.currentRole,
                isLoading = state.isLoading,
                onProfileClick = onNavigateToProfile,
                onToggleRole = viewModel::toggleDebugRole,
                myAvatarUrl = state.myAvatarUrl,
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
                contentPadding = PaddingValues(24.dp, 16.dp, 24.dp, 100.dp),
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
                    item { Text("Список пуст", color = Color.White.copy(0.4f), fontSize = 14.sp) }
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
    onProfileClick: () -> Unit,
    onToggleRole: () -> Unit,
    myAvatarUrl: String?,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 48.dp, start = 24.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.clickable { onToggleRole() }) {
            Text("SmartJam", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                text = if (role == UserRole.TEACHER) "Преподаватель" else "Ученик",
                fontSize = 12.sp,
                color = BrandCyan,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(
                    Modifier.size(18.dp).padding(end = 8.dp),
                    Color.White,
                    2.dp,
                )
            }
            Surface(
                onClick = onProfileClick,
                shape = CircleShape,
                color = Color.White.copy(0.05f),
                modifier = Modifier.size(42.dp),
                border = BorderStroke(1.dp, Color.White.copy(0.1f)),
            ) {
                SmartJamAvatar(url = myAvatarUrl, size = 42.dp)
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

    val displayCode =
        remember(code) {
            if (code != null && code.length == 6) {
                "${code.take(3)}-${code.drop(3)}"
            } else code ?: ""
        }

    GlassContainer {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Код приглашения", color = Color.White.copy(0.6f), fontSize = 14.sp)

            if (code != null) {
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
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
                        text = displayCode,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGold,
                        letterSpacing = 4.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Нажмите, чтобы скопировать",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.4f),
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
                value = value,
                onValueChange = { input -> onValueChange(formatRawCode(input)) },
                "Введите код",
                Icons.Default.Person,
                enabled = !isLoading,
                visualTransformation = InviteCodeTransformation(),
            )
            GoldenStringsButton(
                "Отправить заявку",
                onJoin,
                enabled = !isLoading && value.length == 6,
            )
        }
    }
}

@Composable
private fun ActiveConnectionCard(connection: Connection, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SmartJamAvatar(url = connection.peerAvatarUrl)
            Spacer(Modifier.width(16.dp))
            UserIdentityBlock(
                firstName = connection.peerFirstName,
                lastName = connection.peerLastName,
                username = connection.peerName,
            )
        }
    }
}

class InviteCodeTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val input = text.text
        val out = buildString {
            for (i in input.indices) {
                append(input[i])
                if (i == 2 && input.length > 3) append("-")
            }
        }

        val offsetMapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (offset <= 2) return offset
                    if (offset <= 6) return offset + 1
                    return 7
                }

                override fun transformedToOriginal(offset: Int): Int {
                    if (offset <= 2) return offset
                    if (offset <= 7) return offset - 1
                    return 6
                }
            }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

private fun formatRawCode(input: String): String {
    return input.uppercase().filter { it.isLetterOrDigit() }.take(6)
}
