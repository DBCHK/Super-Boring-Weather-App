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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.DailyForecast
import kotlin.math.roundToInt

/**
 * Weekly precipitation bar chart for the DAY/WEEK toggle week mode.
 */
@Composable
fun WeeklyPrecipGraphCanvas(
    dailyList: List<DailyForecast>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dailyList.isEmpty()) return

    Box(
        modifier = modifier.pointerInput(dailyList) {
            detectTapGestures { offset ->
                val w = size.width.toFloat()
                if (w > 0 && dailyList.isNotEmpty()) {
                    val slot = w / dailyList.size
                    val idx = (offset.x / slot).toInt().coerceIn(0, dailyList.lastIndex)
                    onIndexSelected(idx)
                }
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val labelH = 28f
            val graphH = height - labelH
            val count = dailyList.size
            val gap = 10f
            val barW = ((width - gap * (count + 1)) / count).coerceAtLeast(12f)
            val maxAmount = dailyList.maxOf { it.precipAmountInches.coerceAtLeast(0.01f) }
                .coerceAtLeast(0.15f)
            val maxChance = dailyList.maxOf { it.precipChancePercent }.coerceAtLeast(10)

            dailyList.forEachIndexed { i, day ->
                val x = gap + i * (barW + gap)
                val amountNorm = (day.precipAmountInches / maxAmount).coerceIn(0.05f, 1f)
                val chanceNorm = (day.precipChancePercent / maxChance.toFloat()).coerceIn(0.08f, 1f)
                // Blend amount + chance for bar height
                val barNorm = (amountNorm * 0.55f + chanceNorm * 0.45f).coerceIn(0.08f, 1f)
                val barH = graphH * 0.78f * barNorm
                val top = graphH - barH
                val isSelected = i == selectedIndex

                // Track
                drawRoundRect(
                    color = Color(0xFFE5E5EA),
                    topLeft = Offset(x, graphH * 0.12f),
                    size = Size(barW, graphH * 0.78f),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                // Active bar
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = if (isSelected) {
                            listOf(Color(0xFF00C7BE), Color(0xFF007AFF))
                        } else {
                            listOf(
                                Color(0xFF007AFF).copy(alpha = 0.75f),
                                Color(0xFF007AFF).copy(alpha = 0.35f)
                            )
                        },
                        startY = top,
                        endY = graphH
                    ),
                    topLeft = Offset(x, top),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                // Selection glow ring
                if (isSelected) {
                    drawRoundRect(
                        color = Color(0xFF1C1C1E).copy(alpha = 0.9f),
                        topLeft = Offset(x - 2f, top - 2f),
                        size = Size(barW + 4f, barH + 4f),
                        cornerRadius = CornerRadius(12f, 12f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                    )
                }

                // Day label
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 28f
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
                        height - 6f,
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
