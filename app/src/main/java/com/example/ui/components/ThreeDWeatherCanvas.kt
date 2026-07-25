package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.data.model.WeatherCondition
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ThreeDWeatherCanvas(
    condition: WeatherCondition,
    isDaytime: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Infinite animation transition for subtle 3D floating / bobbing / rotation
    val infiniteTransition = rememberInfiniteTransition(label = "3DFloating")

    // Vertical floating Y offset
    val floatY by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    // Sun ray / wind rotation angle
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    // Rain drop progress (0f to 1f loop)
    val particleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleProgress"
    )

    // Interactive drag tilt (user can swipe/tilt 3D cloud with fingers!)
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        dragX = 0f
                        dragY = 0f
                    },
                    onDragCancel = {
                        dragX = 0f
                        dragY = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    dragX = (dragX + dragAmount.x * 0.2f).coerceIn(-40f, 40f)
                    dragY = (dragY + dragAmount.y * 0.2f).coerceIn(-40f, 40f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f + dragX, size.height / 2f + floatY + dragY)
            val baseRadius = size.width.coerceAtMost(size.height) * 0.18f

            // 1. Draw Ground Blur Drop Shadow
            val shadowWidth = baseRadius * 2.8f
            val shadowHeight = baseRadius * 0.5f
            val shadowCenter = Offset(center.x, center.y + baseRadius * 1.8f)

            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.22f),
                        Color.Black.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = shadowCenter,
                    radius = shadowWidth / 2f
                ),
                topLeft = Offset(shadowCenter.x - shadowWidth / 2f, shadowCenter.y - shadowHeight / 2f),
                size = Size(shadowWidth, shadowHeight)
            )

            // 2. Draw 3D Weather Models based on Condition
            when (condition) {
                WeatherCondition.SUNNY, WeatherCondition.CLEAR -> {
                    draw3DSun(center, baseRadius, rotationAngle, isDaytime)
                }
                WeatherCondition.PARTLY_CLOUDY -> {
                    draw3DSun(Offset(center.x + baseRadius * 0.7f, center.y - baseRadius * 0.5f), baseRadius * 0.8f, rotationAngle, isDaytime)
                    draw3DCloud(center, baseRadius, isStorm = false)
                }
                WeatherCondition.CLOUDY, WeatherCondition.MOSTLY_CLOUDY -> {
                    draw3DCloud(center, baseRadius, isStorm = false)
                }
                WeatherCondition.RAINY, WeatherCondition.HEAVY_RAIN -> {
                    draw3DCloud(center, baseRadius, isStorm = false)
                    drawRainParticles(center, baseRadius, particleProgress)
                }
                WeatherCondition.THUNDERSTORM -> {
                    draw3DCloud(center, baseRadius, isStorm = true)
                    drawLightningBolt(center, baseRadius, particleProgress)
                    drawRainParticles(center, baseRadius, particleProgress)
                }
                WeatherCondition.SNOWY -> {
                    draw3DCloud(center, baseRadius, isStorm = false)
                    drawSnowParticles(center, baseRadius, particleProgress)
                }
                WeatherCondition.WINDY -> {
                    drawWindSwirls(center, baseRadius, rotationAngle)
                    draw3DCloud(center, baseRadius * 0.85f, isStorm = false)
                }
            }
        }
    }
}

// 3D Dark Clay Cloud with depth lobes and ambient shadows
private fun DrawScope.draw3DCloud(center: Offset, baseRadius: Float, isStorm: Boolean) {
    val darkBase = if (isStorm) Color(0xFF1E1E24) else Color(0xFF2C2C2E)
    val midTone = if (isStorm) Color(0xFF3A3A40) else Color(0xFF48484A)
    val highlightTone = if (isStorm) Color(0xFF5A5A62) else Color(0xFF636366)

    // Cloud Lobes (xRel, yRel, radiusFactor)
    val lobes = listOf(
        Triple(-0.8f, 0.2f, 0.65f),
        Triple(-0.4f, -0.1f, 0.85f),
        Triple(0.1f, -0.25f, 0.95f),
        Triple(0.6f, -0.05f, 0.80f),
        Triple(0.9f, 0.25f, 0.60f),
        Triple(0.0f, 0.35f, 0.75f)
    )

    // Base Shadow Layer
    for ((xFactor, yFactor, rFactor) in lobes) {
        val lobeCenter = Offset(center.x + xFactor * baseRadius, center.y + yFactor * baseRadius + 4f)
        drawCircle(
            color = Color.Black.copy(alpha = 0.3f),
            radius = baseRadius * rFactor + 2f,
            center = lobeCenter
        )
    }

    // Main 3D Spheres with Radial Shading for Clay Effect
    for ((xFactor, yFactor, rFactor) in lobes) {
        val lobeRadius = baseRadius * rFactor
        val lobeCenter = Offset(center.x + xFactor * baseRadius, center.y + yFactor * baseRadius)
        
        // Highlight offset for top-left light source
        val lightOffset = Offset(lobeCenter.x - lobeRadius * 0.28f, lobeCenter.y - lobeRadius * 0.28f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    highlightTone,
                    midTone,
                    darkBase
                ),
                center = lightOffset,
                radius = lobeRadius * 1.3f
            ),
            radius = lobeRadius,
            center = lobeCenter
        )
    }
}

// 3D Sculpted Sun Sphere with Geometric Rays
private fun DrawScope.draw3DSun(center: Offset, radius: Float, rotationAngle: Float, isDaytime: Boolean) {
    val coreColor = if (isDaytime) Color(0xFFFF9500) else Color(0xFF5E5CE6)
    val glowColor = if (isDaytime) Color(0xFFFFCC00) else Color(0xFFBF5AF2)
    val shadowColor = if (isDaytime) Color(0xFFC75000) else Color(0xFF3634A3)

    // Rotating Rays Ring
    rotate(rotationAngle, center) {
        val rayCount = 12
        val rayLen = radius * 0.45f
        for (i in 0 until rayCount) {
            val angleDeg = i * (360f / rayCount)
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val innerPoint = Offset(
                center.x + (radius * 1.22f) * cos(angleRad).toFloat(),
                center.y + (radius * 1.22f) * sin(angleRad).toFloat()
            )
            val outerPoint = Offset(
                center.x + (radius * 1.22f + rayLen) * cos(angleRad).toFloat(),
                center.y + (radius * 1.22f + rayLen) * sin(angleRad).toFloat()
            )

            drawLine(
                color = glowColor.copy(alpha = 0.85f),
                start = innerPoint,
                end = outerPoint,
                strokeWidth = radius * 0.12f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }

    // 3D Sun Sphere
    val lightOffset = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glowColor,
                coreColor,
                shadowColor
            ),
            center = lightOffset,
            radius = radius * 1.25f
        ),
        radius = radius,
        center = center
    )
}

// Rain Drops Animation
private fun DrawScope.drawRainParticles(center: Offset, baseRadius: Float, progress: Float) {
    val dropCount = 14
    val rainColor = Color(0xFF007AFF)

    for (i in 0 until dropCount) {
        val xOffset = ((i * 37) % 180 - 90) * (baseRadius / 90f)
        val phase = (progress + (i * 0.13f)) % 1f
        val startY = center.y + baseRadius * 0.5f + (phase * baseRadius * 1.4f)
        val dropLen = baseRadius * 0.28f

        drawLine(
            color = rainColor.copy(alpha = (1f - phase * 0.8f).coerceIn(0.2f, 0.9f)),
            start = Offset(center.x + xOffset, startY),
            end = Offset(center.x + xOffset, startY + dropLen),
            strokeWidth = 6f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

// Snow Particle Animation
private fun DrawScope.drawSnowParticles(center: Offset, baseRadius: Float, progress: Float) {
    val flakeCount = 16
    val snowColor = Color.White

    for (i in 0 until flakeCount) {
        val xBase = ((i * 43) % 200 - 100) * (baseRadius / 100f)
        val phase = (progress + (i * 0.08f)) % 1f
        val sway = sin((phase * 4 * Math.PI) + i).toFloat() * 15f
        val startY = center.y + baseRadius * 0.4f + (phase * baseRadius * 1.5f)

        drawCircle(
            color = snowColor.copy(alpha = (1f - phase * 0.7f).coerceIn(0.3f, 1f)),
            radius = 7f,
            center = Offset(center.x + xBase + sway, startY)
        )
    }
}

// Lightning Bolt Flash
private fun DrawScope.drawLightningBolt(center: Offset, baseRadius: Float, progress: Float) {
    if (progress in 0.2f..0.35f || progress in 0.7f..0.8f) {
        val boltPath = Path().apply {
            moveTo(center.x - 5f, center.y + baseRadius * 0.4f)
            lineTo(center.x - 25f, center.y + baseRadius * 0.9f)
            lineTo(center.x - 5f, center.y + baseRadius * 0.9f)
            lineTo(center.x - 20f, center.y + baseRadius * 1.4f)
            lineTo(center.x + 15f, center.y + baseRadius * 0.85f)
            lineTo(center.x - 5f, center.y + baseRadius * 0.85f)
            close()
        }
        drawPath(
            path = boltPath,
            color = Color(0xFFFFCC00)
        )
    }
}

// Wind Swirl Lines
private fun DrawScope.drawWindSwirls(center: Offset, baseRadius: Float, rotationAngle: Float) {
    val windColor = Color(0xFF8E8E93).copy(alpha = 0.5f)
    for (i in 0..2) {
        val yOff = (i - 1) * baseRadius * 0.5f
        val path = Path().apply {
            moveTo(center.x - baseRadius * 1.2f, center.y + yOff)
            quadraticTo(
                center.x, center.y + yOff - 30f,
                center.x + baseRadius * 1.2f, center.y + yOff
            )
        }
        drawPath(
            path = path,
            color = windColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 8f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
    }
}
