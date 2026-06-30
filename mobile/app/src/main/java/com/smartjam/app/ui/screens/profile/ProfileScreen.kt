package com.smartjam.app.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.smartjam.app.ui.components.AppleLiquidBackground
import com.smartjam.app.ui.components.GlassContainer
import com.smartjam.app.ui.theme.BrandCyan
import com.smartjam.app.ui.theme.CoreBackground
import com.smartjam.app.ui.theme.ErrorRed

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onBack: () -> Unit, onNavigateToLogin: () -> Unit) {
    val state = viewModel.state
    val context = LocalContext.current

    val photoPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            viewModel.onImageSelected(uri)
        }

    LaunchedEffect(Unit) {
        viewModel.events.collect { if (it is ProfileEvent.NavigateToLogin) onNavigateToLogin() }
    }

    Box(Modifier.fillMaxSize().background(CoreBackground)) {
        AppleLiquidBackground()

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 48.dp, start = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { if (state.isEditing) viewModel.setEditing(false) else onBack() }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }

                TextButton(
                    onClick = {
                        if (state.isEditing) viewModel.saveProfile() else viewModel.setEditing(true)
                    }
                ) {
                    Text(
                        if (state.isEditing) "Готово" else "Изм.",
                        color = BrandCyan,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column(
                Modifier.fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(20.dp))

                Box(contentAlignment = Alignment.BottomEnd) {
                    val avatarData = state.selectedImageUri ?: state.avatarUrl

                    Box(
                        Modifier.size(130.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.08f))
                            .border(1.dp, Color.White.copy(0.1f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatarData != null) {
                            AsyncImage(
                                model =
                                    ImageRequest.Builder(context)
                                        .data(avatarData)
                                        .crossfade(true)
                                        .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(64.dp),
                            )
                        }
                    }

                    if (state.isEditing) {
                        Surface(
                            onClick = { photoPicker.launch("image/*") },
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(38.dp).offset(x = (-4).dp, y = (-4).dp),
                            shadowElevation = 8.dp,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (!state.isEditing) {
                    Text(
                        text = "${state.firstName} ${state.lastName}".ifBlank { state.username },
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(text = state.musicalTitle, color = BrandCyan.copy(0.9f), fontSize = 15.sp)
                }

                Spacer(Modifier.height(32.dp))

                GlassContainer {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        if (state.isEditing) {
                            ProfileEditField("Никнейм", state.username, viewModel::onUsernameChange)
                            ProfileEditField("Имя", state.firstName, viewModel::onFirstNameChange)
                            ProfileEditField("Фамилия", state.lastName, viewModel::onLastNameChange)
                        } else {
                            ProfileViewItem("Почта", state.email)
                            ProfileViewItem("Username", "@${state.username}")
                        }
                    }
                }

                if (state.error != null) {
                    Text(
                        state.error!!,
                        color = ErrorRed,
                        modifier = Modifier.padding(top = 16.dp),
                        fontSize = 13.sp,
                    )
                }

                Spacer(Modifier.height(40.dp))

                if (!state.isEditing) {
                    TextButton(
                        onClick = viewModel::onLogoutClicked,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = ErrorRed.copy(0.7f))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Выйти из системы",
                            color = ErrorRed.copy(0.7f),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(Modifier.height(50.dp))
            }
        }
    }
}

@Composable
private fun ProfileViewItem(label: String, value: String) {
    Column {
        Text(
            label.uppercase(),
            color = Color.White.copy(0.3f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProfileEditField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(
            label.uppercase(),
            color = BrandCyan.copy(0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle =
                TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(BrandCyan),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            decorationBox = { innerTextField ->
                if (value.isEmpty())
                    Text("Не указано", color = Color.White.copy(0.1f), fontSize = 17.sp)
                innerTextField()
            },
        )
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.1f)))
    }
}
