package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.WeatherCondition
import kotlin.math.cos
import kotlin.math.sin

private data class Point3D(val x: Double, val y: Double, val z: Double)

private data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val zDepth: Float,
    val scale: Float,
    val lightIntensity: Float = 1.0f
)

private fun project3D(
    p: Point3D,
    rotXDeg: Float,
    rotYDeg: Float,
    rotZDeg: Float,
    centerX: Float,
    centerY: Float,
    perspective: Float = 600f
): ProjectedPoint {
    val radX = Math.toRadians(rotXDeg.toDouble())
    val radY = Math.toRadians(rotYDeg.toDouble())
    val radZ = Math.toRadians(rotZDeg.toDouble())

    // 1. Z-axis rotation
    val x1 = p.x * cos(radZ) - p.y * sin(radZ)
    val y1 = p.x * sin(radZ) + p.y * cos(radZ)
    val z1 = p.z

    // 2. X-axis rotation (Pitch)
    val y2 = y1 * cos(radX) - z1 * sin(radX)
    val z2 = y1 * sin(radX) + z1 * cos(radX)
    val x2 = x1

    // 3. Y-axis rotation (Yaw)
    val x3 = x2 * cos(radY) + z2 * sin(radY)
    val z3 = -x2 * sin(radY) + z2 * cos(radY)
    val y3 = y2

    // Simple Light source at (-1, -1, -1) in 3D space
    val lightDirX = -0.577
    val lightDirY = -0.577
    val lightDirZ = -0.577

    val mag = Math.sqrt(x3 * x3 + y3 * y3 + z3 * z3)
    val nx = if (mag > 0) x3 / mag else 0.0
    val ny = if (mag > 0) y3 / mag else 0.0
    val nz = if (mag > 0) z3 / mag else 1.0

    val dot = nx * lightDirX + ny * lightDirY + nz * lightDirZ
    val intensity = (0.3 + 0.7 * Math.max(0.0, -dot)).toFloat()

    // Prevent divide-by-near-zero / extreme scale blow-ups behind camera
    val depth = (perspective + z3).toFloat().coerceAtLeast(80f)
    val scale = (perspective / depth).coerceIn(0.25f, 2.8f)
    val screenX = centerX + (x3 * scale).toFloat()
    val screenY = centerY + (y3 * scale).toFloat()

    return ProjectedPoint(screenX, screenY, z3.toFloat(), scale, intensity)
}

@Composable
fun ThreeDWeatherCanvas(
    condition: WeatherCondition,
    isDaytime: Boolean = true,
    modifier: Modifier = Modifier,
    modelScale: Float = 1.0f,
    /** Theme fill for cloud-like models; sun keeps warm gold when null-ish. */
    tintColor: Color? = null,
    shadeColor: Color? = null
) {
    val interaction = rememberInteractive3DState(
        initialPitch = 14f,
        autoSpinDegPerSec = 16f,
        maxPitch = 75f
    )

    val infiniteTransition = rememberInfiniteTransition(label = "3DAnimation")

    val bobbingY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobbingY"
    )

    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    // Read animated rotation each frame (includes gentle device-tilt hologram)
    val currentRotX = interaction.renderPitch
    val currentRotY = interaction.renderYaw

    val modelPath = when (condition) {
        WeatherCondition.SUNNY -> "models/sun.glb"
        WeatherCondition.CLEAR -> if (isDaytime) "models/sun.glb" else "models/moon.glb"
        WeatherCondition.RAINY, WeatherCondition.HEAVY_RAIN -> "models/rain_cloud.glb"
        WeatherCondition.SNOWY -> "models/snowflake.glb"
        WeatherCondition.THUNDERSTORM -> "models/lightning.glb"
        else -> "models/cloud.glb"
    }

    // Theme-aware tints: keep sun/lightning warm; clouds/snow follow theme for contrast
    val isSunOrBolt = condition == WeatherCondition.SUNNY ||
        (condition == WeatherCondition.CLEAR && isDaytime) ||
        condition == WeatherCondition.THUNDERSTORM
    val resolvedFill = when {
        isSunOrBolt -> Color(0xFFFFD54F)
        condition == WeatherCondition.SNOWY -> Color(0xFFF5F5F7)
        tintColor != null -> tintColor
        else -> Color(0xFFE5E5EA)
    }
    val resolvedShade = when {
        isSunOrBolt -> Color(0xFFFF8C00)
        shadeColor != null -> shadeColor
        else -> Color(0xFF8E8E93)
    }

    Box(
        modifier = modifier.interactive3D(interaction, enablePitch = true),
        contentAlignment = Alignment.Center
    ) {
        GlbModelRenderer(
            modelPath = modelPath,
            interactionState = interaction,
            offsetY = bobbingY / 30f, // Scaling pixels to SceneView units
            scaleToUnits = modelScale,
            tintColor = resolvedFill,
            shadeColor = resolvedShade,
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f + bobbingY
            val baseScale = (size.width.coerceAtMost(size.height) / 300f).coerceAtLeast(0.8f)

            when (condition) {
                WeatherCondition.SUNNY, WeatherCondition.CLEAR -> {
                    if (isDaytime) {
                        val sunRadius = 78f * baseScale
                        val sunCenter = project3D(
                            Point3D(0.0, 0.0, 0.0),
                            currentRotX, currentRotY, 0f, centerX, centerY
                        )

                        // Soft outer glow - overlays on the 3D sun
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD54F).copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                center = Offset(sunCenter.screenX, sunCenter.screenY),
                                radius = sunRadius * 1.9f * sunCenter.scale
                            ),
                            radius = sunRadius * 1.9f * sunCenter.scale,
                            center = Offset(sunCenter.screenX, sunCenter.screenY)
                        )

                        // Solar corona particles
                        val rings = listOf(1.3, 1.55)
                        rings.forEachIndexed { ringIdx, ringMult ->
                            val numSegments = 18
                            val ringRadius = sunRadius * ringMult
                            for (i in 0 until numSegments) {
                                val angle =
                                    (i * 360.0 / numSegments) + (pulseProgress * 360.0 * (ringIdx + 1) * 0.08)
                                val rad = Math.toRadians(angle)
                                val z = sin(rad * 3) * 18.0
                                val p = project3D(
                                    Point3D(cos(rad) * ringRadius, sin(rad) * ringRadius, z),
                                    currentRotX, currentRotY, 0f, centerX, centerY
                                )

                                val alpha = (0.18f + 0.4f * p.lightIntensity).coerceIn(0.1f, 0.55f)
                                drawCircle(
                                    color = if (ringIdx == 0) {
                                        Color(0xFFFFD700).copy(alpha = alpha)
                                    } else {
                                        Color(0xFFFF8C00).copy(alpha = alpha)
                                    },
                                    radius = 7f * p.scale,
                                    center = Offset(p.screenX, p.screenY)
                                )
                            }
                        }
                    }
                }

                WeatherCondition.THUNDERSTORM -> {
                    // Lightning flash
                    if (pulseProgress > 0.88f) {
                        val boltNodes = listOf(
                            Point3D(0.0, 0.0, 30.0),
                            Point3D(20.0, 40.0, 40.0),
                            Point3D(-10.0, 80.0, 20.0),
                            Point3D(10.0, 120.0, 0.0)
                        )
                        val projectedNodes = boltNodes.map {
                            project3D(it, currentRotX, currentRotY, 0f, centerX, centerY)
                        }
                        val path = Path().apply {
                            moveTo(projectedNodes[0].screenX, projectedNodes[0].screenY)
                            projectedNodes.drop(1).forEach { lineTo(it.screenX, it.screenY) }
                        }
                        drawPath(path, Color(0xFF7DD3FC), style = Stroke(width = 6f, cap = StrokeCap.Round))
                        drawPath(path, Color.White, style = Stroke(width = 2f, cap = StrokeCap.Round))
                    }
                }

                WeatherCondition.SNOWY -> {
                    val center = project3D(
                        Point3D(0.0, 0.0, 0.0),
                        currentRotX, currentRotY, 0f, centerX, centerY
                    )

                    for (i in 0 until 6) {
                        val angle = i * 60.0 + pulseProgress * 12.0
                        val rad = Math.toRadians(angle)
                        val tip = project3D(
                            Point3D(cos(rad) * 90.0 * baseScale, sin(rad) * 90.0 * baseScale, 0.0),
                            currentRotX, currentRotY, 0f, centerX, centerY
                        )

                        drawLine(
                            color = Color(0xFFE0F2FE),
                            start = Offset(center.screenX, center.screenY),
                            end = Offset(tip.screenX, tip.screenY),
                            strokeWidth = 6f * tip.scale,
                            cap = StrokeCap.Round
                        )

                        for (j in 1..2) {
                            val subDist = j * 30.0 * baseScale
                            val subStart = Point3D(cos(rad) * subDist, sin(rad) * subDist, 0.0)
                            val subStartP = project3D(subStart, currentRotX, currentRotY, 0f, centerX, centerY)

                            val subAngle1 = rad + Math.toRadians(45.0)
                            val subAngle2 = rad - Math.toRadians(45.0)
                            val subLen = 25.0 * baseScale

                            val t1 = project3D(
                                Point3D(
                                    subStart.x + cos(subAngle1) * subLen,
                                    subStart.y + sin(subAngle1) * subLen,
                                    10.0
                                ),
                                currentRotX, currentRotY, 0f, centerX, centerY
                            )
                            val t2 = project3D(
                                Point3D(
                                    subStart.x + cos(subAngle2) * subLen,
                                    subStart.y + sin(subAngle2) * subLen,
                                    -10.0
                                ),
                                currentRotX, currentRotY, 0f, centerX, centerY
                            )

                            drawLine(
                                Color(0xFFBAE6FD),
                                Offset(subStartP.screenX, subStartP.screenY),
                                Offset(t1.screenX, t1.screenY),
                                4f * t1.scale,
                                StrokeCap.Round
                            )
                            drawLine(
                                Color(0xFFBAE6FD),
                                Offset(subStartP.screenX, subStartP.screenY),
                                Offset(t2.screenX, t2.screenY),
                                4f * t2.scale,
                                StrokeCap.Round
                            )
                        }
                    }
                }

                else -> {
                    // Rain particles for rainy conditions
                    val isRainy = condition == WeatherCondition.RAINY || condition == WeatherCondition.HEAVY_RAIN
                    if (isRainy) {
                        for (i in 0 until 12) {
                            val x = (i - 6) * 20.0
                            val dropP = project3D(
                                Point3D(
                                    x,
                                    40.0 + (pulseProgress * 60.0),
                                    (i % 3) * 20.0 - 20.0
                                ),
                                currentRotX, currentRotY, 0f, centerX, centerY
                            )
                            drawLine(
                                color = Color(0xFF38BDF8).copy(alpha = 0.55f * dropP.scale.coerceIn(0.4f, 1f)),
                                start = Offset(dropP.screenX, dropP.screenY),
                                end = Offset(dropP.screenX - 5f, dropP.screenY + 15f * dropP.scale),
                                strokeWidth = 3f * dropP.scale,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}
