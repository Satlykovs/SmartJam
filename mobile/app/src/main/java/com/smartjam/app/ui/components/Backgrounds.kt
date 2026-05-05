package com.smartjam.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartjam.app.ui.theme.BlurCyan
import com.smartjam.app.ui.theme.BlurPurpleDark
import com.smartjam.app.ui.theme.BlurPurpleLight
import kotlin.math.sin

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
                color = BlurPurpleDark.copy(alpha = 0.4f),
                radius = width * 0.7f,
                center = Offset(
                    x = width * 0.5f + sin(Math.toRadians(phase1.toDouble())).toFloat() * 200f,
                    y = height * 0.2f
                )
            )

            drawCircle(
                color = BlurPurpleLight.copy(alpha = 0.3f),
                radius = width * 0.6f,
                center = Offset(
                    x = width * 0.8f,
                    y = height * 0.6f + sin(Math.toRadians(phase1.toDouble() + 90)).toFloat() * 300f
                )
            )

            drawCircle(
                color = BlurCyan.copy(alpha = 0.2f),
                radius = width * 0.5f,
                center = Offset(
                    x = width * 0.2f + sin(Math.toRadians(phase1.toDouble() + 180)).toFloat() * 150f,
                    y = height * 0.8f
                )
            )
        }
    }
}
