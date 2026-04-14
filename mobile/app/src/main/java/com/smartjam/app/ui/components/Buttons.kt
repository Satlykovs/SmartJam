package com.smartjam.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartjam.app.ui.theme.BrandCyan
import com.smartjam.app.ui.theme.BrandGold
import com.smartjam.app.ui.theme.BrandOrange
import com.smartjam.app.ui.theme.BrandPink
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GoldenStringsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
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
            .background(Color.White.copy(alpha = if (enabled) 0.1f else 0.05f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        BrandGold.copy(alpha = 0.5f),
                        BrandPink.copy(alpha = 0.3f),
                        BrandCyan.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                enabled = enabled,
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

            val stringColors = listOf(BrandGold, BrandOrange, BrandPink, BrandCyan)

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
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
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
