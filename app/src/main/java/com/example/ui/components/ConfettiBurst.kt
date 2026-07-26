package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Lightweight confetti overlay for celebrations (double-tap hero, etc.). */
@Composable
fun ConfettiBurst(
    active: Boolean,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {}
) {
    if (!active) return
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1400, easing = LinearEasing),
        finishedListener = { onFinished() },
        label = "confetti"
    )
    val particles = remember {
        List(48) {
            ConfettiParticle(
                angle = Random.nextFloat() * 360f,
                speed = 0.35f + Random.nextFloat() * 0.85f,
                size = 4f + Random.nextFloat() * 8f,
                color = listOf(
                    Color(0xFFFFB300),
                    Color(0xFF007AFF),
                    Color(0xFF34C759),
                    Color(0xFFFF3B30),
                    Color(0xFFAF52DE),
                    Color.White
                )[it % 6],
                spin = Random.nextFloat() * 360f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f
        val dist = size.minDimension * 0.55f * progress
        particles.forEach { p ->
            val rad = Math.toRadians(p.angle.toDouble())
            val x = cx + cos(rad).toFloat() * dist * p.speed
            val y = cy + sin(rad).toFloat() * dist * p.speed + progress * progress * 120f
            val alpha = (1f - progress).coerceIn(0f, 1f)
            rotate(p.spin + progress * 180f, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - p.size / 2f, y - p.size / 2f),
                    size = Size(p.size, p.size * 0.55f)
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val spin: Float
)
