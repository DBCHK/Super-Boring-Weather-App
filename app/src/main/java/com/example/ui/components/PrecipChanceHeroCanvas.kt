package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.cos
import kotlin.math.sin

/**
 * Unique PRECIP hero: a glass “chance orb” that fills with liquid to the
 * precipitation probability (0–100%), with a surface wave and falling droplets.
 */
@Composable
fun PrecipChanceHeroCanvas(
    chancePercent: Int,
    rateInches: Float,
    modifier: Modifier = Modifier
) {
    val chance = chancePercent.coerceIn(0, 100)
    val fillTarget = chance / 100f

    val fill by animateFloatAsState(
        targetValue = fillTarget,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "precipFill"
    )

    val infinite = rememberInfiniteTransition(label = "precipOrb")
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )
    val dropPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dropPhase"
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val interaction = rememberInteractive3DState(
        initialPitch = 8f,
        autoSpinDegPerSec = 8f,
        maxPitch = 25f,
        maxYaw = 40f,
        autoSpinOscillate = true
    )

    Box(
        modifier = modifier.interactive3D(interaction, enablePitch = false),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f + 6f
            val radius = size.minDimension * 0.36f

            // Soft outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF007AFF).copy(alpha = 0.22f * glowPulse),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = radius * 1.55f
                ),
                radius = radius * 1.55f,
                center = Offset(cx, cy)
            )

            val orbPath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        cx - radius,
                        cy - radius,
                        cx + radius,
                        cy + radius
                    )
                )
            }

            // Glass body
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F4FF).copy(alpha = 0.95f),
                        Color(0xFFD6ECFF).copy(alpha = 0.75f),
                        Color(0xFFB8D9F5).copy(alpha = 0.55f)
                    ),
                    startY = cy - radius,
                    endY = cy + radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )

            // Liquid fill (clipped to orb)
            clipPath(orbPath, clipOp = ClipOp.Intersect) {
                val liquidTop = cy + radius - (2f * radius * fill)
                val waveAmp = 6f + 4f * fill

                val liquidPath = Path().apply {
                    moveTo(cx - radius - 4f, cy + radius + 4f)
                    lineTo(cx - radius - 4f, liquidTop)
                    val steps = 28
                    for (i in 0..steps) {
                        val t = i / steps.toFloat()
                        val x = cx - radius + t * (2f * radius)
                        val y = liquidTop +
                            sin(wavePhase + t * 4.2f + interaction.renderYaw * 0.02f) * waveAmp +
                            cos(wavePhase * 0.7f + t * 2.1f) * (waveAmp * 0.35f)
                        lineTo(x, y)
                    }
                    lineTo(cx + radius + 4f, cy + radius + 4f)
                    close()
                }

                val liquidColor = when {
                    chance >= 70 -> Color(0xFF0051D5)
                    chance >= 40 -> Color(0xFF007AFF)
                    chance >= 15 -> Color(0xFF30B0C7)
                    else -> Color(0xFF64D2FF)
                }

                drawPath(
                    path = liquidPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            liquidColor.copy(alpha = 0.95f),
                            liquidColor.copy(alpha = 0.55f),
                            Color(0xFF00C7BE).copy(alpha = 0.35f)
                        ),
                        startY = liquidTop - 20f,
                        endY = cy + radius
                    )
                )

                // Surface highlight line
                if (fill > 0.04f) {
                    val highlight = Path()
                    val steps = 24
                    for (i in 0..steps) {
                        val t = i / steps.toFloat()
                        val x = cx - radius * 0.92f + t * (radius * 1.84f)
                        val y = liquidTop +
                            sin(wavePhase + t * 4.2f) * waveAmp * 0.85f
                        if (i == 0) highlight.moveTo(x, y) else highlight.lineTo(x, y)
                    }
                    drawPath(
                        path = highlight,
                        color = Color.White.copy(alpha = 0.45f),
                        style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                    )
                }

                // Bubbles inside liquid
                if (fill > 0.12f) {
                    val bubbleCount = 4 + (chance / 20)
                    for (i in 0 until bubbleCount) {
                        val bx = cx + cos(i * 1.7 + wavePhase) * radius * 0.35f
                        val by = liquidTop + 18f +
                            ((i * 37 + dropPhase * 40) % (radius * fill * 1.4f + 1f))
                        if (by < cy + radius - 8f) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.35f),
                                radius = 3f + (i % 3),
                                center = Offset(bx.toFloat(), by)
                            )
                        }
                    }
                }
            }

            // Glass rim
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 3.5f)
            )
            drawCircle(
                color = Color(0xFF007AFF).copy(alpha = 0.25f),
                radius = radius + 3f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )

            // Specular shine
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    center = Offset(cx - radius * 0.35f, cy - radius * 0.4f),
                    radius = radius * 0.55f
                ),
                radius = radius * 0.55f,
                center = Offset(cx - radius * 0.35f, cy - radius * 0.4f)
            )

            // Falling droplets outside when chance is meaningful
            if (chance >= 20) {
                val drops = 3 + chance / 15
                for (i in 0 until drops) {
                    val seed = i * 17.3f
                    val dx = cx + ((seed * 13f) % (radius * 1.8f)) - radius * 0.9f
                    val travel = ((dropPhase + seed * 0.07f) % 1f)
                    val dy = cy - radius * 1.15f + travel * (radius * 2.4f)
                    val alpha = (0.55f * (1f - travel)).coerceIn(0.05f, 0.55f)
                    drawLine(
                        color = Color(0xFF007AFF).copy(alpha = alpha),
                        start = Offset(dx, dy),
                        end = Offset(dx - 2f, dy + 12f + rateInches * 8f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Center chance label
            drawContext.canvas.nativeCanvas.apply {
                val pctPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = radius * 0.42f
                    color = android.graphics.Color.parseColor("#1C1C1E")
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.SANS_SERIF,
                        android.graphics.Typeface.BOLD
                    )
                }
                val subPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = radius * 0.14f
                    color = android.graphics.Color.parseColor("#8E8E93")
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.MONOSPACE,
                        android.graphics.Typeface.BOLD
                    )
                    letterSpacing = 0.08f
                }
                drawText("$chance%", cx, cy + radius * 0.08f, pctPaint)
                drawText("CHANCE", cx, cy + radius * 0.28f, subPaint)
            }
        }
    }
}
