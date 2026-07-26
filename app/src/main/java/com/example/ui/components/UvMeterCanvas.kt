package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.util.rememberDropletPlayers
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun UvMeterCanvas(
    uvIndex: Float,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onUvChange: ((Float) -> Unit)? = null
) {
    val feedback = rememberDropletPlayers()
    var dragUv by remember(uvIndex) { mutableFloatStateOf(uvIndex) }
    var isDragging by remember { mutableStateOf(false) }
    val target = if (isDragging) dragUv else uvIndex

    val animatedUv by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "uvIndex"
    )

    val pulse by rememberInfiniteTransition(label = "uvPulse").animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "uvPulseVal"
    )

    val knobBoost by animateFloatAsState(
        targetValue = if (isDragging) 1.35f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "uvKnob"
    )

    var lastBucket by remember { mutableIntStateOf(-1) }

    Box(
        modifier = modifier.then(
            if (interactive) {
                Modifier.pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            onUvChange?.invoke(dragUv)
                        },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, _ ->
                            change.consume()
                            val cx = size.width / 2f
                            val cy = size.height * 0.65f
                            val dx = change.position.x - cx
                            val dy = change.position.y - cy
                            // Map angle on semicircle (left 180° → right 0°) to UV 0–12
                            var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            // atan2: right=0, down=90, left=±180, up=-90
                            // We want top semicircle from left (180) to right (0)
                            if (deg < 0f) deg += 360f
                            // Clamp to upper half-ish: 0..180
                            val clamped = deg.coerceIn(0f, 180f)
                            val frac = 1f - (clamped / 180f)
                            val next = (frac * 12f).coerceIn(0f, 12f)
                            dragUv = next
                            val bucket = next.roundToInt()
                            if (bucket != lastBucket) {
                                lastBucket = bucket
                                feedback.scrubTick(bucket)
                            }
                            onUvChange?.invoke(next)
                        }
                    )
                }
            } else Modifier
        ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.65f)
            val radius = size.width.coerceAtMost(size.height) * 0.42f

            val startAngle = 180f
            val sweepAngle = 180f

            val arcRect = Size(radius * 2f, radius * 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)

            drawArc(
                color = Color(0xFFE5E5EA),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcRect,
                style = Stroke(width = 24f, cap = StrokeCap.Round)
            )

            val fraction = (animatedUv / 12f).coerceIn(0f, 1f)
            val activeSweep = sweepAngle * fraction

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF34C759),
                        Color(0xFFFFCC00),
                        Color(0xFFFF9500),
                        Color(0xFFFF3B30),
                        Color(0xFFAF52DE)
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

            val knobAngle = startAngle + activeSweep
            val knobRad = Math.toRadians(knobAngle.toDouble())
            val knobPos = Offset(
                center.x + radius * cos(knobRad).toFloat(),
                center.y + radius * sin(knobRad).toFloat()
            )

            val glowR = 22f * knobBoost * if (isDragging) pulse else 1f
            drawCircle(Color.White.copy(alpha = 0.35f), radius = glowR, center = knobPos)
            drawCircle(Color.White, radius = 16f * knobBoost, center = knobPos)
            drawCircle(Color(0xFF1C1C1E), radius = 10f * knobBoost, center = knobPos)
        }
    }
}

