package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WindCompassCanvas(
    degrees: Int,
    speedMph: Float,
    modifier: Modifier = Modifier
) {
    val animatedDegrees by animateFloatAsState(
        targetValue = degrees.toFloat(),
        animationSpec = tween(600),
        label = "compassDegrees"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width.coerceAtMost(size.height) * 0.40f

            // Outer dial ring
            drawCircle(
                color = Color(0xFFE5E5EA),
                radius = radius,
                center = center,
                style = Stroke(width = 6f)
            )

            // Direction Ticks (N, E, S, W)
            for (i in 0 until 12) {
                val angleRad = Math.toRadians(i * 30.0)
                val isMajor = i % 3 == 0
                val tickLen = if (isMajor) 16f else 8f
                val start = Offset(
                    center.x + (radius - tickLen) * cos(angleRad).toFloat(),
                    center.y + (radius - tickLen) * sin(angleRad).toFloat()
                )
                val end = Offset(
                    center.x + radius * cos(angleRad).toFloat(),
                    center.y + radius * sin(angleRad).toFloat()
                )

                drawLine(
                    color = if (isMajor) Color(0xFF1C1C1E) else Color(0xFF8E8E93),
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 4f else 2f
                )
            }

            // 3D Animated Needle
            rotate(animatedDegrees, center) {
                // North Red Arrow
                val northPath = Path().apply {
                    moveTo(center.x, center.y - radius * 0.75f)
                    lineTo(center.x - 12f, center.y)
                    lineTo(center.x, center.y)
                    close()
                }
                drawPath(northPath, Color(0xFFFF3B30))

                val northPathRight = Path().apply {
                    moveTo(center.x, center.y - radius * 0.75f)
                    lineTo(center.x + 12f, center.y)
                    lineTo(center.x, center.y)
                    close()
                }
                drawPath(northPathRight, Color(0xFFFF6961))

                // South Black Arrow
                val southPath = Path().apply {
                    moveTo(center.x, center.y + radius * 0.75f)
                    lineTo(center.x - 12f, center.y)
                    lineTo(center.x, center.y)
                    close()
                }
                drawPath(southPath, Color(0xFF1C1C1E))

                val southPathRight = Path().apply {
                    moveTo(center.x, center.y + radius * 0.75f)
                    lineTo(center.x + 12f, center.y)
                    lineTo(center.x, center.y)
                    close()
                }
                drawPath(southPathRight, Color(0xFF48484A))
            }

            // Center Pin
            drawCircle(Color.White, radius = 14f, center = center)
            drawCircle(Color(0xFF1C1C1E), radius = 8f, center = center)
        }
    }
}
