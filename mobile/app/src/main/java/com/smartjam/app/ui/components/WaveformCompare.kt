package com.smartjam.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.smartjam.app.model.FeedbackEvent
import com.smartjam.app.model.FeedbackType
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
    onErrorGroupClick: (List<FeedbackEvent>) -> Unit,
    modifier: Modifier = Modifier,
    onScrubbing: (Long) -> Unit = {},
    onScrubbingFinished: (Long) -> Unit = {},
) {
    val barsCount = 100
    val maxAmp =
        remember(teacherRms, studentRms) {
            val m = maxOf(teacherRms.maxOrNull() ?: 0.1f, studentRms.maxOrNull() ?: 0.1f)
            if (m < 0.01f) 0.1f else m
        }

    val teacherSampled = remember(teacherRms) { downsample(teacherRms, barsCount) }
    val studentSampled = remember(studentRms) { downsample(studentRms, barsCount) }

    val totalDurationSec = if (durationMs > 0) durationMs / 1000f else 1.0f

    val barErrorStates =
        remember(feedback, totalDurationSec) {
            List(barsCount) { i ->
                val start = (i.toFloat() / barsCount) * totalDurationSec
                val end = ((i + 1).toFloat() / barsCount) * totalDurationSec
                feedback.filter { start < it.studentEndTime && end > it.studentStartTime }
            }
        }

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(feedback, durationMs) {
                    detectTapGestures { offset ->
                        if (durationMs > 0) {
                            val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                            val clickedBarIdx =
                                (ratio * barsCount).toInt().coerceIn(0, barsCount - 1)

                            if (barErrorStates[clickedBarIdx].isNotEmpty()) {
                                var startIdx = clickedBarIdx
                                while (startIdx > 0 && barErrorStates[startIdx - 1].isNotEmpty()) {
                                    startIdx--
                                }
                                var endIdx = clickedBarIdx
                                while (
                                    endIdx < barsCount - 1 &&
                                        barErrorStates[endIdx + 1].isNotEmpty()
                                ) {
                                    endIdx++
                                }

                                val timeStart = (startIdx.toFloat() / barsCount) * totalDurationSec
                                val timeEnd =
                                    ((endIdx + 1).toFloat() / barsCount) * totalDurationSec

                                val groupedErrors =
                                    feedback
                                        .filter {
                                            it.studentStartTime < timeEnd &&
                                                it.studentEndTime > timeStart
                                        }
                                        .distinctBy { it.studentStartTime }
                                onErrorGroupClick(groupedErrors)
                            } else {
                                onErrorGroupClick(emptyList())
                            }
                        }
                    }
                }
                .pointerInput(durationMs) {
                    var lastTargetPos = currentPositionMs
                    detectDragGestures(
                        onDragStart = {},
                        onDragEnd = { onScrubbingFinished(lastTargetPos) },
                        onDragCancel = { onScrubbingFinished(lastTargetPos) },
                        onDrag = { change, _ ->
                            change.consume()
                            if (durationMs > 0) {
                                val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                                lastTargetPos = (ratio * durationMs).toLong()
                                onScrubbing(lastTargetPos)
                            }
                        },
                    )
                }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val stepX = width / barsCount
        val barWidth = stepX * 0.5f

        teacherSampled.forEachIndexed { i, amp ->
            val barH = (amp / maxAmp) * (centerY * 0.85f)
            val x = i * stepX + (stepX / 2f)
            drawLine(
                BrandGold.copy(0.4f),
                Offset(x, centerY - 2f),
                Offset(x, centerY - barH.coerceAtLeast(4f)),
                barWidth,
                StrokeCap.Round,
            )
        }

        studentSampled.forEachIndexed { i, amp ->
            val x = i * stepX + (stepX / 2f)
            val barH = (amp / maxAmp) * (centerY * 0.85f)
            val errors = barErrorStates[i]
            val hasNote = errors.any { it.type == FeedbackType.WRONG_NOTE }
            val hasRhythm = errors.any { it.type == FeedbackType.WRONG_RHYTHM }

            val brush =
                when {
                    hasNote && hasRhythm ->
                        Brush.verticalGradient(listOf(Color(0xFFFF5252), Color(0xFFFFD166)))
                    hasNote -> SolidColor(Color(0xFFFF5252))
                    hasRhythm -> SolidColor(Color(0xFFFFD166))
                    else -> SolidColor(BrandCyan.copy(alpha = 0.8f))
                }
            drawLine(
                brush,
                Offset(x, centerY + 2f),
                Offset(x, centerY + barH.coerceAtLeast(4f)),
                barWidth,
                StrokeCap.Round,
            )
        }

        if (durationMs > 0) {
            val cursorX = (currentPositionMs.toFloat() / durationMs) * width
            drawLine(Color.White, Offset(cursorX, 0f), Offset(cursorX, height), 2.dp.toPx())
            drawCircle(Color.White, 6.dp.toPx(), Offset(cursorX, 0f))
        }

        drawLine(Color.White.copy(0.1f), Offset(0f, centerY), Offset(width, centerY), 1.dp.toPx())
    }
}

private fun downsample(data: List<Float>, targetSize: Int): List<Float> {
    if (data.isEmpty()) return List(targetSize) { 0f }
    val result = mutableListOf<Float>()
    val chunkSize = data.size.toFloat() / targetSize
    for (i in 0 until targetSize) {
        val start = (i * chunkSize).toInt()
        val end = ((i + 1) * chunkSize).toInt().coerceAtMost(data.size)
        result.add(if (start < end) data.subList(start, end).maxOrNull() ?: 0f else 0f)
    }
    return result
}
