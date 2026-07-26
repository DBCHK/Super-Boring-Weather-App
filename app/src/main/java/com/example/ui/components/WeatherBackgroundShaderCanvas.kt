package com.example.ui.components

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
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val angle: Float,
    val alpha: Float,
    val phase: Float,
    /** 0..1 parallax depth — farther particles shift more with device tilt */
    val depth: Float
)

@Composable
fun WeatherBackgroundShaderCanvas(
    condition: WeatherCondition,
    modifier: Modifier = Modifier,
    /** Theme particle colors for contrast on light/dark/yellow backgrounds. */
    particlePrimary: Color = Color.White.copy(alpha = 0.22f),
    particleSecondary: Color = Color(0xFF007AFF).copy(alpha = 0.45f),
    isDarkTheme: Boolean = false,
    /** Meteorological wind from direction (degrees, 0 = N, 90 = E). Rain falls with the wind. */
    windDirectionDegrees: Int = 180,
    windSpeedMph: Float = 8f
) {
    // Obvious phone-motion parallax for rain / dots / flakes
    val motion = rememberDeviceMotionState(intensity = 1.35f)

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

    val particles = remember(condition) {
        val list = mutableListOf<Particle>()
        // Tuned down for GPU budget — still reads as lively weather, not a particle storm
        val count = when (condition) {
            WeatherCondition.SNOWY -> 42
            WeatherCondition.RAINY, WeatherCondition.HEAVY_RAIN -> 55
            WeatherCondition.THUNDERSTORM -> 48
            WeatherCondition.HAZE -> 28
            WeatherCondition.SUNNY, WeatherCondition.CLEAR -> 24
            else -> 22
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
                    phase = random.nextFloat() * 6.28f,
                    depth = 0.35f + random.nextFloat() * 0.65f
                )
            )
        }
        list
    }

    // Read motion so Canvas recomposes on tilt
    val tiltX = motion.offsetX
    val tiltY = motion.offsetY
    // Wind: meteorological "from" direction → rain moves toward opposite (downwind)
    // 0° = from N → rain drifts south; 90° = from E → rain drifts west
    val windRad = Math.toRadians((windDirectionDegrees + 180.0) % 360.0)
    val windStrength = (windSpeedMph / 18f).coerceIn(0.25f, 2.2f)
    // Horizontal unit of wind in screen space (x positive = right)
    val windDirX = sin(windRad).toFloat()
    val windDirY = cos(windRad).toFloat() // positive y = down on screen when wind from N… 

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        // Large pixel shift so movement is clearly visible when the phone tilts
        val maxShiftX = width * 0.14f
        val maxShiftY = height * 0.10f

        fun parallax(p: Particle): Offset {
            return Offset(
                x = tiltX * maxShiftX * p.depth,
                y = tiltY * maxShiftY * p.depth
            )
        }

        // Rain streak end offset: mostly downward + wind horizontal, mild wind vertical
        fun rainSlant(p: Particle, dropLength: Float, extraTilt: Float): Offset {
            val baseFall = dropLength
            val windX = windDirX * windStrength * (28f + p.size * 4f) * p.depth + extraTilt
            val windY = baseFall + windDirY.coerceAtLeast(0.15f) * windStrength * 10f * p.depth
            return Offset(windX, windY)
        }

        when (condition) {
            WeatherCondition.SNOWY -> {
                val flake = if (isDarkTheme) Color.White else Color(0xFF1C1C1E)
                val flakeCore = if (isDarkTheme) Color(0xFFF0F8FF) else Color(0xFF3A3A3C)
                particles.forEach { p ->
                    val shift = parallax(p)
                    val yPos = ((p.y + animProgress * p.speed * 2f) % 1f) * height + shift.y
                    val xSway = sin(animProgress * 12.5f + p.phase) * 24f * p.speed
                    val xPos = (p.x * width + xSway + shift.x + width) % width
                    val yWrapped = ((yPos % height) + height) % height
                    val radius = p.size * (0.8f + 0.4f * sin(p.phase + animProgress * 6f))

                    drawCircle(
                        color = flake.copy(alpha = p.alpha * 0.3f),
                        radius = radius * 2f,
                        center = Offset(xPos, yWrapped)
                    )
                    drawCircle(
                        color = flakeCore.copy(alpha = p.alpha * 0.9f),
                        radius = radius,
                        center = Offset(xPos, yWrapped)
                    )
                    if (radius > 5f) {
                        rotate(
                            degrees = (animProgress * 360f * p.speed + tiltX * 40f) % 360f,
                            pivot = Offset(xPos, yWrapped)
                        ) {
                            for (arm in 0 until 6) {
                                val angleRad = arm * (Math.PI / 3.0)
                                val endX = xPos + cos(angleRad).toFloat() * radius * 2.2f
                                val endY = yWrapped + sin(angleRad).toFloat() * radius * 2.2f
                                drawLine(
                                    color = Color.White.copy(alpha = p.alpha * 0.8f),
                                    start = Offset(xPos, yWrapped),
                                    end = Offset(endX, endY),
                                    strokeWidth = 1.8f
                                )
                            }
                        }
                    }
                }
            }

            WeatherCondition.RAINY, WeatherCondition.HEAVY_RAIN -> {
                val rainCore = if (isDarkTheme) Color(0xFF9CCBFF) else Color(0xFF1E90FF)
                val rainTip = if (isDarkTheme) Color.White else Color(0xFF70A1FF)
                // Drift entire field with wind so motion is obvious
                val windDriftX = windDirX * windStrength * animProgress * width * 0.35f
                particles.forEach { p ->
                    val shift = parallax(p)
                    val fallSpeed = p.speed * (3.2f + windStrength * 0.9f)
                    val yPos = ((p.y + animProgress * fallSpeed) % 1f) * height + shift.y
                    val xPos = (p.x * width + p.angle * 40f + windDriftX + shift.x + width * 3f) % width
                    val yWrapped = ((yPos % height) + height) % height
                    val dropLength = p.size * 5f + windStrength * 6f + kotlin.math.abs(tiltY) * 6f
                    val end = rainSlant(p, dropLength, tiltX * 40f * p.depth)

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                rainTip.copy(alpha = 0f),
                                rainCore.copy(alpha = p.alpha * 0.85f),
                                (if (isDarkTheme) Color.White else rainTip).copy(alpha = p.alpha)
                            ),
                            start = Offset(xPos, yWrapped),
                            end = Offset(xPos + end.x, yWrapped + end.y)
                        ),
                        start = Offset(xPos, yWrapped),
                        end = Offset(xPos + end.x, yWrapped + end.y),
                        strokeWidth = p.size * 0.35f,
                        cap = StrokeCap.Round
                    )
                }
            }

            WeatherCondition.THUNDERSTORM -> {
                val windDriftX = windDirX * windStrength * animProgress * width * 0.4f
                particles.forEach { p ->
                    val shift = parallax(p)
                    val fallSpeed = p.speed * (4f + windStrength)
                    val yPos = ((p.y + animProgress * fallSpeed) % 1f) * height + shift.y
                    val xPos = (p.x * width + windDriftX + shift.x + width * 2f) % width
                    val yWrapped = ((yPos % height) + height) % height
                    val dropLength = p.size * 6f + windStrength * 8f
                    val end = rainSlant(p, dropLength, tiltX * 36f * p.depth)

                    drawLine(
                        color = Color(0xFFA0C4FF).copy(alpha = p.alpha * 0.7f),
                        start = Offset(xPos, yWrapped),
                        end = Offset(xPos + end.x, yWrapped + end.y),
                        strokeWidth = p.size * 0.4f,
                        cap = StrokeCap.Round
                    )
                }

                val isFlashing = flashProgress in 0.82f..0.88f || flashProgress in 0.92f..0.96f
                if (isFlashing) {
                    drawRect(color = Color(0xFFE2E8F0).copy(alpha = 0.25f), size = size)
                    val boltPath = Path().apply {
                        val startX = width * 0.6f + tiltX * 40f
                        moveTo(startX, 0f)
                        lineTo(startX - 25f, height * 0.2f)
                        lineTo(startX + 15f, height * 0.22f)
                        lineTo(startX - 35f, height * 0.45f)
                        lineTo(startX + 10f, height * 0.47f)
                        lineTo(startX - 20f, height * 0.7f)
                    }
                    drawPath(boltPath, Color.White, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
                    drawPath(
                        boltPath,
                        Color(0xFF00D2FF),
                        style = Stroke(width = 9f, cap = StrokeCap.Round)
                    )
                }
            }

            WeatherCondition.HAZE -> {
                particles.forEach { p ->
                    val shift = parallax(p)
                    val xPos = ((p.x + animProgress * p.speed * 0.3f) % 1f) * width + shift.x
                    val yPos = p.y * height + sin(animProgress * 6.28f + p.phase) * 15f + shift.y
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
                particles.forEach { p ->
                    val shift = parallax(p)
                    val yPos =
                        ((p.y - animProgress * p.speed * 0.2f + 1f) % 1f) * height + shift.y
                    val xPos =
                        (p.x * width + sin(animProgress * 4f + p.phase) * 30f + shift.x + width) % width
                    val yWrapped = ((yPos % height) + height) % height
                    val alpha =
                        (sin(animProgress * 6.28f + p.phase) * 0.3f + 0.5f) * p.alpha
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = alpha * 0.5f),
                        radius = p.size * 1.5f,
                        center = Offset(xPos, yWrapped)
                    )
                }
            }

            else -> {
                // Floating ambient dots — theme-colored multi-layer parallax
                particles.forEach { p ->
                    val shift = parallax(p)
                    val xPos =
                        ((p.x + animProgress * p.speed * 0.15f) % 1f) * width + shift.x
                    val yPos = p.y * height + shift.y
                    val alpha = p.alpha * (if (isDarkTheme) 0.32f else 0.18f) * (0.5f + p.depth * 0.5f)
                    drawCircle(
                        color = particlePrimary.copy(alpha = alpha.coerceIn(0.04f, 0.45f)),
                        radius = p.size * (8f + p.depth * 8f),
                        center = Offset(xPos, yPos)
                    )
                    // Inner brighter core for depth
                    drawCircle(
                        color = particleSecondary.copy(alpha = alpha * 0.55f),
                        radius = p.size * (3f + p.depth * 3f),
                        center = Offset(xPos - 2f, yPos - 2f)
                    )
                }
            }
        }
    }
}
