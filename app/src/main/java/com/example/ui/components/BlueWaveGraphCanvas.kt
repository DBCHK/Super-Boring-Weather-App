package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.HourlyForecast
import kotlin.math.roundToInt

@Composable
fun BlueWaveGraphCanvas(
    hourlyList: List<HourlyForecast>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (hourlyList.isEmpty()) return

    Box(
        modifier = modifier.pointerInput(hourlyList) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                val w = size.width.toFloat()
                if (w > 0) {
                    val fraction = (change.position.x / w).coerceIn(0f, 1f)
                    val idx = (fraction * (hourlyList.size - 1)).roundToInt()
                    if (idx != selectedIndex) {
                        onIndexSelected(idx)
                    }
                }
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paddingBottom = 20f
            val graphHeight = height - paddingBottom

            // Collect precipitation rates / chances
            val rates = hourlyList.map { it.precipRateInches.coerceAtLeast(0.01f) }
            val maxRate = (rates.maxOrNull() ?: 0.4f).coerceAtLeast(0.4f)

            // Construct Smooth Wave Path
            val path = Path()
            val fillPath = Path()

            val stepX = width / (hourlyList.size - 1).coerceAtLeast(1)

            for (i in hourlyList.indices) {
                val x = i * stepX
                val rate = rates[i]
                val normalizedY = 1f - (rate / maxRate).coerceIn(0f, 1f)
                val y = (normalizedY * (graphHeight * 0.75f)) + (graphHeight * 0.15f)

                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, graphHeight)
                    fillPath.lineTo(x, y)
                } else {
                    val prevX = (i - 1) * stepX
                    val prevRate = rates[i - 1]
                    val prevNormY = 1f - (prevRate / maxRate).coerceIn(0f, 1f)
                    val prevY = (prevNormY * (graphHeight * 0.75f)) + (graphHeight * 0.15f)

                    val cx1 = prevX + stepX / 2f
                    val cy1 = prevY
                    val cx2 = prevX + stepX / 2f
                    val cy2 = y

                    path.cubicTo(cx1, cy1, cx2, cy2, x, y)
                    fillPath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                }
            }

            fillPath.lineTo(width, graphHeight)
            fillPath.lineTo(0f, graphHeight)
            fillPath.close()

            // 1. Draw Electric Blue Wave Gradient Fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF007AFF).copy(alpha = 0.85f),
                        Color(0xFF00C7BE).copy(alpha = 0.40f),
                        Color(0xFF007AFF).copy(alpha = 0.05f)
                    ),
                    startY = 0f,
                    endY = graphHeight
                )
            )

            // 2. Draw Bright Blue Top Edge Line
            drawPath(
                path = path,
                color = Color(0xFF007AFF),
                style = Stroke(width = 5f)
            )

            // 3. Draw Selected Hour Vertical Indicator Line and Scrub Knob
            val selX = selectedIndex * stepX
            val selRate = rates.getOrNull(selectedIndex) ?: 0.1f
            val selNormY = 1f - (selRate / maxRate).coerceIn(0f, 1f)
            val selY = (selNormY * (graphHeight * 0.75f)) + (graphHeight * 0.15f)

            // Vertical indicator
            drawLine(
                color = Color(0xFF1C1C1E).copy(alpha = 0.6f),
                start = Offset(selX, 0f),
                end = Offset(selX, graphHeight),
                strokeWidth = 3f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )

            // Scrub Indicator Point
            drawCircle(
                color = Color.White,
                radius = 12f,
                center = Offset(selX, selY)
            )
            drawCircle(
                color = Color(0xFF007AFF),
                radius = 7f,
                center = Offset(selX, selY)
            )
        }
    }
}
