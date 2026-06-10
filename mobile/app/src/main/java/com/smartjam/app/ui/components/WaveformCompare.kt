package com.smartjam.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.smartjam.app.model.FeedbackEvent
import com.smartjam.app.ui.theme.BrandCyan
import com.smartjam.app.ui.theme.BrandGold

@Composable
fun WaveformCompare(
    teacherRms: List<Float>,
    studentRms: List<Float>,
    feedback: List<FeedbackEvent>,
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val barsCount = 100 // Количество палочек на экране

    val maxAmp =
        remember(teacherRms, studentRms) {
            val m = maxOf(teacherRms.maxOrNull() ?: 0.1f, studentRms.maxOrNull() ?: 0.1f)
            if (m < 0.01f) 0.1f else m
        }

    val teacherSampled = remember(teacherRms) { downsample(teacherRms, barsCount) }
    val studentSampled = remember(studentRms) { downsample(studentRms, barsCount) }

    // Длительность для расчетов (из плеера или из данных, если плеер еще тупит)
    val totalDurationSec =
        if (durationMs > 0) durationMs / 1000f
        else (feedback.maxOfOrNull { it.studentEndTime } ?: 1.0).toFloat()

    Canvas(
        modifier =
            modifier.fillMaxWidth().height(160.dp).pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0) {
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((ratio * durationMs).toLong())
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val stepX = width / barsCount
        val barWidth = stepX * 0.5f // Тонкие палочки

        teacherSampled.forEachIndexed { i, amp ->
            val barH = (amp / maxAmp) * (centerY * 0.85f)
            val x = i * stepX + (stepX / 2f)
            drawLine(
                color = BrandGold.copy(alpha = 0.5f),
                start = Offset(x, centerY - 2f),
                end = Offset(x, centerY - barH.coerceAtLeast(4f)),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }

        studentSampled.forEachIndexed { i, amp ->
            val x = i * stepX + (stepX / 2f)
            val barH = (amp / maxAmp) * (centerY * 0.85f)

            // ТВОЯ ЛОГИКА: расчет через время
            val barStart = (i.toFloat() / barsCount) * totalDurationSec
            val barEnd = ((i + 1).toFloat() / barsCount) * totalDurationSec

            val hasError = feedback.any { ev ->
                val errStart = ev.studentStartTime.toFloat()
                val errEnd = ev.studentEndTime.toFloat()
                // Палочка красная, если её временной интервал пересекается с интервалом ошибки
                barStart < errEnd && barEnd > errStart
            }

            drawLine(
                color = if (hasError) Color(0xFFFF5252) else BrandCyan,
                start = Offset(x, centerY + 2f),
                end = Offset(x, centerY + barH.coerceAtLeast(4f)),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }

        // Курсор
        if (durationMs > 0) {
            val cursorX = (currentPositionMs.toFloat() / durationMs) * width
            drawLine(
                Color.White,
                Offset(cursorX, 0f),
                Offset(cursorX, height),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

private fun downsample(data: List<Float>, targetSize: Int): List<Float> {
    if (data.isEmpty()) return List(targetSize) { 0f }
    val result = mutableListOf<Float>()
    val chunkSize = data.size.toFloat() / targetSize
    for (i in 0 until targetSize) {
        val start = (i * chunkSize).toInt()
        val end = ((i + 1) * chunkSize).toInt().coerceAtMost(data.size)
        if (start < end) result.add(data.subList(start, end).maxOrNull() ?: 0f) else result.add(0f)
    }
    return result
}
