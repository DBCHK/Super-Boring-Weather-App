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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun UvMeterCanvas(
    uvIndex: Float,
    modifier: Modifier = Modifier
) {
    val animatedUv by animateFloatAsState(
        targetValue = uvIndex,
        animationSpec = tween(700),
        label = "uvIndex"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.65f)
            val radius = size.width.coerceAtMost(size.height) * 0.42f

            val startAngle = 180f
            val sweepAngle = 180f

            val arcRect = Size(radius * 2f, radius * 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)

            // Background Track Arc
            drawArc(
                color = Color(0xFFE5E5EA),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcRect,
                style = Stroke(width = 24f, cap = StrokeCap.Round)
            )

            // UV Color Spectrum Arc (Low Green -> Moderate Yellow -> High Orange -> Very High Purple)
            val fraction = (animatedUv / 12f).coerceIn(0f, 1f)
            val activeSweep = sweepAngle * fraction

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF34C759), // Green (Low)
                        Color(0xFFFFCC00), // Yellow (Moderate)
                        Color(0xFFFF9500), // Orange (High)
                        Color(0xFFFF3B30), // Red (Very High)
                        Color(0xFFAF52DE)  // Purple (Extreme)
                    ),
                    center = center
                ),
                startAngle = startAngle,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcRect,
                style = Stroke(width = 24f, cap = StrokeCap.Round)
            )

            // Indicator Knob Point
            val knobAngle = startAngle + activeSweep
            val knobRad = Math.toRadians(knobAngle.toDouble())
            val knobPos = Offset(
                center.x + radius * cos(knobRad).toFloat(),
                center.y + radius * sin(knobRad).toFloat()
            )

            drawCircle(Color.White, radius = 16f, center = knobPos)
            drawCircle(Color(0xFF1C1C1E), radius = 10f, center = knobPos)
        }
    }
}
