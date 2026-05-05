package com.smartjam.app.ui.screens.register

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartjam.app.domain.model.UserRole
import com.smartjam.app.ui.components.AppleGlassTextField
import com.smartjam.app.ui.components.AppleLiquidBackground
import com.smartjam.app.ui.components.GoldenStringsButton
import com.smartjam.app.ui.theme.BrandGold
import com.smartjam.app.ui.theme.CoreBackground
import com.smartjam.app.ui.theme.ErrorRed

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RegisterEvent.NavigateToHome -> onNavigateToHome()
                is RegisterEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CoreBackground)
    ) {
        AppleLiquidBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Создать аккаунт",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))


            GlassRoleSelector(
                selectedRole = state.selectedRole,
                onRoleSelected = { viewModel.onRoleSelected(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))


            AppleGlassTextField(
                value = state.usernameInput,
                onValueChange = { viewModel.onUsernameChanged(it) },
                hint = "Имя пользователя",
                icon = Icons.Default.Person,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            AppleGlassTextField(
                value = state.emailInput,
                onValueChange = { viewModel.onEmailChanged(it) },
                hint = "Email",
                icon = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            AppleGlassTextField(
                value = state.passwordInput,
                onValueChange = { viewModel.onPasswordChanged(it) },
                hint = "Пароль",
                icon = Icons.Default.Lock,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            AppleGlassTextField(
                value = state.repeatPasswordInput,
                onValueChange = { viewModel.onRepeatPasswordChanged(it) },
                hint = "Повторите пароль",
                icon = Icons.Default.Lock,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.onRegisterClicked() })
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = ErrorRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            GoldenStringsButton(
                text = if (state.isLoading) "Создание..." else "Зарегистрироваться",
                onClick = { viewModel.onRegisterClicked() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Уже есть аккаунт? Войти",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .clickable { viewModel.onBackClicked() }
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun GlassRoleSelector(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoleButton(
            text = "Я ученик",
            isSelected = selectedRole == UserRole.STUDENT,
            onClick = { onRoleSelected(UserRole.STUDENT) },
            modifier = Modifier.weight(1f)
        )

        RoleButton(
            text = "Я преподаватель",
            isSelected = selectedRole == UserRole.TEACHER,
            onClick = { onRoleSelected(UserRole.TEACHER) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RoleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) BrandGold.copy(alpha = 0.2f) else Color.Transparent,
        label = "RoleColorAnimation"
    )

    val textColor = if (isSelected) BrandGold else Color.White.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}