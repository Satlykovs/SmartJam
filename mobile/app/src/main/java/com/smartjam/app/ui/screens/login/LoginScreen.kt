package com.smartjam.app.ui.screens.login

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LoginScreen(

    viewModel: LoginViewModel = viewModel(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.NavigateToHome -> onNavigateToHome()
                is LoginEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
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
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SmartJam",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White, Color(0xFFE0E0E0))
                    )
                )
            )

            Text(
                text = "Почувствуй музыку",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 56.dp)
            )

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

            Spacer(modifier = Modifier.height(20.dp))

            AppleGlassTextField(
                value = state.passwordInput,
                onValueChange = { viewModel.onPasswordChanged(it) },
                hint = "Пароль",
                icon = Icons.Default.Lock,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.onLoginClicked() })
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = Color(0xFFFF5252),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            GoldenStringsButton(
                text = if (state.isLoading) "Загрузка..." else "Войти",
                onClick = { viewModel.onLoginClicked() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Создать аккаунт",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF00E5FF),
                modifier = Modifier
                    .clickable { onNavigateToRegister() }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "или продолжить через",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            AppleGlassButton(
                onClick = { /* TODO: Google Auth */ },
                text = "Google",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AppleLiquidBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing)), label = "p1"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().blur(120.dp)) {
            val width = size.width
            val height = size.height

            drawCircle(
                color = Color(0xFF4A00E0).copy(alpha = 0.4f),
                radius = width * 0.7f,
                center = Offset(
                    x = width * 0.5f + sin(Math.toRadians(phase1.toDouble())).toFloat() * 200f,
                    y = height * 0.2f
                )
            )

            drawCircle(
                color = Color(0xFF8E2DE2).copy(alpha = 0.3f),
                radius = width * 0.6f,
                center = Offset(
                    x = width * 0.8f,
                    y = height * 0.6f + sin(Math.toRadians(phase1.toDouble() + 90)).toFloat() * 300f
                )
            )

            drawCircle(
                color = Color(0xFF00C9FF).copy(alpha = 0.2f),
                radius = width * 0.5f,
                center = Offset(
                    x = width * 0.2f + sin(Math.toRadians(phase1.toDouble() + 180)).toFloat() * 150f,
                    y = height * 0.8f
                )
            )
        }
    }
}

@Composable
fun AppleGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        ),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        cursorBrush = SolidColor(Color.White),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
fun GoldenStringsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "strings")
    val masterProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "master_progress"
    )

    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.5f),
                        Color(0xFFFF007F).copy(alpha = 0.3f),
                        Color(0xFF00E5FF).copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))) {
            val width = size.width
            val height = size.height
            val numStrings = 4
            val spacing = height * 0.18f

            val startY = (height - (spacing * (numStrings - 1))) / 2f

            val stringColors = listOf(
                Color(0xFFFFD700),
                Color(0xFFFF8C00),
                Color(0xFFFF007F),
                Color(0xFF00E5FF)
            )

            for (i in 0 until numStrings) {
                val baseY = startY + (i * spacing)
                val path = Path()
                path.moveTo(0f, baseY)

                var localTime = masterProgress - (i * 0.15f)
                if (localTime < 0f) localTime += 1f

                val pullDuration = 0.15f
                val maxAmplitude = spacing * 0.7f
                val frequencies = 6f
                val direction = 1f

                var currentYOffset = 0f
                var energyAlpha = 0f

                if (localTime < pullDuration) {
                    val pullProgress = localTime / pullDuration
                    currentYOffset = maxAmplitude * (pullProgress * pullProgress)
                    energyAlpha = pullProgress
                } else {
                    val vibProgress = (localTime - pullDuration) / (1f - pullDuration)
                    val decay = (1f - vibProgress) * (1f - vibProgress) * (1f - vibProgress)
                    val oscillation = cos(vibProgress * PI * 2 * frequencies).toFloat()

                    currentYOffset = maxAmplitude * decay * oscillation
                    energyAlpha = decay
                }

                currentYOffset *= direction

                for (x in 0..width.toInt() step 4) {
                    val normalizedX = x / width
                    val spatialEnvelope = sin(normalizedX * PI).toFloat()
                    val y = baseY + currentYOffset * spatialEnvelope
                    path.lineTo(x.toFloat(), y)
                }

                val glowAlpha = 0.1f + (0.3f * energyAlpha)
                val coreAlpha = 0.3f + (0.7f * energyAlpha)

                val baseThickness = 4f - (i * 0.8f)

                drawPath(
                    path = path,
                    color = stringColors[i].copy(alpha = glowAlpha),
                    style = Stroke(width = baseThickness * 3f * (1f + energyAlpha))
                )

                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            stringColors[i].copy(alpha = coreAlpha * 0.1f),
                            stringColors[i].copy(alpha = coreAlpha),
                            stringColors[i].copy(alpha = coreAlpha * 0.1f)
                        )
                    ),
                    style = Stroke(width = baseThickness)
                )
            }
        }

        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            style = TextStyle(
                shadow = Shadow(
                    color = Color(0xFF000000).copy(alpha = 0.7f),
                    offset = Offset(0f, 2f),
                    blurRadius = 8f
                )
            )
        )
    }
}

@Composable
fun AppleGlassButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenModernPreview() {
    LoginScreen()
}