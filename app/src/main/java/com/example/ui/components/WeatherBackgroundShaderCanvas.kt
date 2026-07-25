package com.example.ui.components

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.data.model.WeatherCondition
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val x: Float, // 0..1 relative width
    val y: Float, // 0..1 relative height
    val size: Float,
    val speed: Float,
    val angle: Float,
    val alpha: Float,
    val phase: Float
)

@Composable
fun WeatherBackgroundShaderCanvas(
    condition: WeatherCondition,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weatherShader")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shaderProgress"
    )

    val flashProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flashProgress"
    )

    // Generate static random seed particles
    val particles = remember(condition) {
        val list = mutableListOf<Particle>()
        val count = when (condition) {
            WeatherCondition.SNOWY -> 60
            WeatherCondition.RAINY, WeatherCondition.HEAVY_RAIN -> 80
            WeatherCondition.THUNDERSTORM -> 70
            WeatherCondition.HAZE -> 45
            WeatherCondition.SUNNY, WeatherCondition.CLEAR -> 35
            else -> 30
        }
        val random = Random(condition.name.hashCode())
        for (i in 0 until count) {
            list.add(
                Particle(
                    x = random.nextFloat(),
                    y = random.nextFloat(),
                    size = random.nextFloat() * 8f + 3f,
                    speed = random.nextFloat() * 0.8f + 0.2f,
                    angle = (random.nextFloat() - 0.5f) * 0.3f,
                    alpha = random.nextFloat() * 0.6f + 0.2f,
                    phase = random.nextFloat() * 6.28f
                )
            )
        }
        list
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        when (condition) {
            WeatherCondition.SNOWY -> {
                // Falling soft 3D snowflakes
                particles.forEach { p ->
                    val yPos = ((p.y + animProgress * p.speed * 2f) % 1f) * height
                    val xSway = sin(animProgress * 12.5f + p.phase) * 24f * p.speed
                    val xPos = (p.x * width + xSway) % width
                    val radius = p.size * (0.8f + 0.4f * sin(p.phase + animProgress * 6f))

                    // Draw outer soft glow
                    drawCircle(
                        color = Color.White.copy(alpha = p.alpha * 0.3f),
                        radius = radius * 2f,
                        center = Offset(xPos, yPos)
                    )
                    // Draw snowflake center
                    drawCircle(
                        color = Color(0xFFF0F8FF).copy(alpha = p.alpha * 0.9f),
                        radius = radius,
                        center = Offset(xPos, yPos)
                    )

                    // Draw 6-arm snowflake lines if particle is large enough
                    if (radius > 5f) {
                        rotate(degrees = (animProgress * 360f * p.speed) % 360f, pivot = Offset(xPos, yPos)) {
                            for (arm in 0 until 6) {
                                val angleRad = arm * (Math.PI / 3.0)
                                val endX = xPos + cos(angleRad).toFloat() * radius * 2.2f
                                val endY = yPos + sin(angleRad).toFloat() * radius * 2.2f
                                drawLine(
                                    color = Color.White.copy(alpha = p.alpha * 0.8f),
                                    start = Offset(xPos, yPos),
                                    end = Offset(endX, endY),
                                    strokeWidth = 1.8f
                                )
                            }
                        }
                    }
                }
            }

            WeatherCondition.RAINY, WeatherCondition.HEAVY_RAIN -> {
                // Animated falling rain droplets
                particles.forEach { p ->
                    val fallSpeed = p.speed * 4f
                    val yPos = ((p.y + animProgress * fallSpeed) % 1f) * height
                    val xPos = (p.x * width + p.angle * 100f) % width
                    val dropLength = p.size * 5f

                    // Rain Streak
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF70A1FF).copy(alpha = 0f),
                                Color(0xFF1E90FF).copy(alpha = p.alpha * 0.8f),
                                Color.White.copy(alpha = p.alpha)
                            ),
                            startY = yPos,
                            endY = yPos + dropLength
                        ),
                        start = Offset(xPos, yPos),
                        end = Offset(xPos + p.angle * 15f, yPos + dropLength),
                        strokeWidth = p.size * 0.35f,
                        cap = StrokeCap.Round
                    )
                }
            }

            WeatherCondition.THUNDERSTORM -> {
                // Rain + Lightning Flash
                particles.forEach { p ->
                    val fallSpeed = p.speed * 5f
                    val yPos = ((p.y + animProgress * fallSpeed) % 1f) * height
                    val xPos = (p.x * width) % width
                    val dropLength = p.size * 6f

                    drawLine(
                        color = Color(0xFFA0C4FF).copy(alpha = p.alpha * 0.7f),
                        start = Offset(xPos, yPos),
                        end = Offset(xPos - 5f, yPos + dropLength),
                        strokeWidth = p.size * 0.4f
                    )
                }

                // Periodic Lightning Flash
                val isFlashing = flashProgress in 0.82f..0.88f || flashProgress in 0.92f..0.96f
                if (isFlashing) {
                    drawRect(
                        color = Color(0xFFE2E8F0).copy(alpha = 0.25f),
                        size = size
                    )

                    // Draw jagged lightning bolt segment
                    val boltPath = Path().apply {
                        val startX = width * 0.6f
                        moveTo(startX, 0f)
                        lineTo(startX - 25f, height * 0.2f)
                        lineTo(startX + 15f, height * 0.22f)
                        lineTo(startX - 35f, height * 0.45f)
                        lineTo(startX + 10f, height * 0.47f)
                        lineTo(startX - 20f, height * 0.7f)
                    }

                    drawPath(
                        path = boltPath,
                        color = Color.White,
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = boltPath,
                        color = Color(0xFF00D2FF),
                        style = Stroke(width = 9f, cap = StrokeCap.Round)
                    )
                }
            }

            WeatherCondition.HAZE -> {
                // Floating misty haze particles and fog bands
                particles.forEach { p ->
                    val xPos = ((p.x + animProgress * p.speed * 0.3f) % 1f) * width
                    val yPos = (p.y * height) + sin(animProgress * 6.28f + p.phase) * 15f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFD1D5DB).copy(alpha = p.alpha * 0.35f),
                                Color(0xFF9CA3AF).copy(alpha = 0f)
                            ),
                            center = Offset(xPos, yPos),
                            radius = p.size * 18f
                        ),
                        radius = p.size * 18f,
                        center = Offset(xPos, yPos)
                    )
                }
            }

            WeatherCondition.SUNNY, WeatherCondition.CLEAR -> {
                // Warm sun flares and floating golden particles
                particles.forEach { p ->
                    val yPos = ((p.y - animProgress * p.speed * 0.2f + 1f) % 1f) * height
                    val xPos = (p.x * width + sin(animProgress * 4f + p.phase) * 30f) % width
                    val alpha = (sin(animProgress * 6.28f + p.phase) * 0.3f + 0.5f) * p.alpha

                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = alpha * 0.5f),
                        radius = p.size * 1.5f,
                        center = Offset(xPos, yPos)
                    )
                }
            }

            else -> {
                // Cloudy ambient floating particles
                particles.forEach { p ->
                    val xPos = ((p.x + animProgress * p.speed * 0.15f) % 1f) * width
                    val yPos = (p.y * height)

                    drawCircle(
                        color = Color.White.copy(alpha = p.alpha * 0.15f),
                        radius = p.size * 12f,
                        center = Offset(xPos, yPos)
                    )
                }
            }
        }
    }
}
