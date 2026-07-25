package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.DailyForecast
import kotlin.math.roundToInt

/**
 * Weekly precipitation chance bars (0–100% Y-axis), day labels on X.
 */
@Composable
fun WeeklyPrecipGraphCanvas(
    dailyList: List<DailyForecast>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dailyList.isEmpty()) return

    val padLeft = 44f
    val padRight = 12f

    Box(
        modifier = modifier.pointerInput(dailyList) {
            detectTapGestures { offset ->
                val w = size.width.toFloat()
                val graphW = (w - padLeft - padRight).coerceAtLeast(1f)
                if (dailyList.isNotEmpty()) {
                    val slot = graphW / dailyList.size
                    val idx = ((offset.x - padLeft) / slot).toInt()
                        .coerceIn(0, dailyList.lastIndex)
                    onIndexSelected(idx)
                }
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val padTop = 14f
            val padBottom = 36f
            val graphLeft = padLeft
            val graphRight = width - padRight
            val graphTop = padTop
            val graphBottom = height - padBottom
            val graphW = (graphRight - graphLeft).coerceAtLeast(1f)
            val graphH = (graphBottom - graphTop).coerceAtLeast(1f)
            val count = dailyList.size
            val gap = 10f
            val barW = ((graphW - gap * (count + 1)) / count).coerceAtLeast(12f)

            fun chanceY(percent: Int): Float {
                val p = percent.coerceIn(0, 100) / 100f
                return graphBottom - (p * graphH)
            }

            // Y-axis grid: 0 / 50 / 100%
            val labelPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.RIGHT
                textSize = 26f
                color = android.graphics.Color.parseColor("#8E8E93")
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.MONOSPACE,
                    android.graphics.Typeface.BOLD
                )
            }
            listOf(0, 50, 100).forEach { tick ->
                val y = chanceY(tick)
                drawLine(
                    color = Color(0xFFD1D1D6).copy(alpha = if (tick == 0) 0.9f else 0.45f),
                    start = Offset(graphLeft, y),
                    end = Offset(graphRight, y),
                    strokeWidth = if (tick == 0) 2f else 1.2f,
                    pathEffect = if (tick == 0) null else PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "$tick%",
                    graphLeft - 8f,
                    y + 9f,
                    labelPaint
                )
            }

            dailyList.forEachIndexed { i, day ->
                val chance = day.precipChancePercent.coerceIn(0, 100)
                val x = graphLeft + gap + i * (barW + gap)
                val top = chanceY(chance)
                // Minimum visible bar for 0% still shows a flat baseline tick
                val barH = (graphBottom - top).coerceAtLeast(if (chance == 0) 3f else 0f)
                val isSelected = i == selectedIndex

                // Track (full 0–100 column)
                drawRoundRect(
                    color = Color(0xFFE5E5EA),
                    topLeft = Offset(x, graphTop),
                    size = Size(barW, graphH),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                // Active bar height = chance%
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = if (isSelected) {
                            listOf(Color(0xFF00C7BE), Color(0xFF007AFF))
                        } else {
                            listOf(
                                Color(0xFF007AFF).copy(alpha = 0.85f),
                                Color(0xFF007AFF).copy(alpha = 0.40f)
                            )
                        },
                        startY = top,
                        endY = graphBottom
                    ),
                    topLeft = Offset(x, top),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                if (isSelected) {
                    drawRoundRect(
                        color = Color(0xFF1C1C1E).copy(alpha = 0.9f),
                        topLeft = Offset(x - 2f, top - 2f),
                        size = Size(barW + 4f, barH + 4f),
                        cornerRadius = CornerRadius(12f, 12f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                    )
                    // % label above bar
                    val pctPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 24f
                        color = android.graphics.Color.parseColor("#1C1C1E")
                        typeface = android.graphics.Typeface.create(
                            android.graphics.Typeface.MONOSPACE,
                            android.graphics.Typeface.BOLD
                        )
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "$chance%",
                        x + barW / 2f,
                        (top - 8f).coerceAtLeast(graphTop + 4f),
                        pctPaint
                    )
                }

                // Day label on X
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 26f
                        color = if (isSelected) {
                            android.graphics.Color.parseColor("#1C1C1E")
                        } else {
                            android.graphics.Color.parseColor("#8E8E93")
                        }
                        typeface = android.graphics.Typeface.create(
                            android.graphics.Typeface.MONOSPACE,
                            android.graphics.Typeface.BOLD
                        )
                    }
                    drawText(
                        day.dayName.take(3).uppercase(),
                        x + barW / 2f,
                        height - 8f,
                        paint
                    )
                }
            }
        }
    }
}

/** Helper used by detailed forecast week metrics. */
fun formatPrecipInches(inches: Float): String {
    return if (inches < 0.01f) "0 IN" else "${((inches * 100).roundToInt() / 100f)} IN"
}
