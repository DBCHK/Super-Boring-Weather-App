package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import com.example.data.model.WeatherCondition
import com.example.util.PianoSoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

private data class Lobe3D(
    val pos: Point3D,
    val radius: Float,
    val colorPrimary: Color,
    val colorShadow: Color,
    val isVolumetric: Boolean = true
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

    val scale = (perspective / (perspective + z3)).coerceIn(0.1, 4.0).toFloat()
    val screenX = centerX + (x3 * scale).toFloat()
    val screenY = centerY + (y3 * scale).toFloat()

    return ProjectedPoint(screenX, screenY, z3.toFloat(), scale, intensity)
}

@Composable
fun ThreeDWeatherCanvas(
    condition: WeatherCondition,
    isDaytime: Boolean = true,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val context = view.context.applicationContext
    val soundManager = remember { PianoSoundManager(context) }
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "3DAnimation")

    val bobbingY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobbingY"
    )

    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    // Smooth interactive rotation angles
    var rotX by remember { mutableFloatStateOf(15f) }
    var rotY by remember { mutableFloatStateOf(0f) }

    var velX by remember { mutableFloatStateOf(0f) }
    var velY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { androidx.compose.runtime.mutableStateOf(false) }

    // Auto-rotation effect when not dragging
    LaunchedEffect(isDragging) {
        if (!isDragging) {
            while (true) {
                rotY += 0.4f
                delay(16)
            }
        }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        soundManager.playWaterDropletSound()
                    },
                    onDragEnd = {
                        isDragging = false
                        // Momentum
                        scope.launch {
                            var currentVx = velX
                            var currentVy = velY
                            while (!isDragging && (kotlin.math.abs(currentVx) > 0.05f || kotlin.math.abs(currentVy) > 0.05f)) {
                                rotY += currentVx
                                rotX -= currentVy
                                currentVx *= 0.94f
                                currentVy *= 0.94f
                                delay(16)
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        velX = dragAmount.x * 0.4f
                        velY = dragAmount.y * 0.4f
                        rotY += velX
                        rotX = (rotX - velY).coerceIn(-85f, 85f)
                    }
                )
            }
            .graphicsLayer {
                cameraDistance = 12f * density
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f + bobbingY
            val baseScale = (size.width.coerceAtMost(size.height) / 300f).coerceAtLeast(0.8f)

            val currentRotX = rotX
            val currentRotY = rotY

            when (condition) {
                WeatherCondition.SUNNY, WeatherCondition.CLEAR -> {
                    // --- HIGH-FIDELITY 3D SUN ---
                    val sunRadius = 78f * baseScale
                    val sunCenter = project3D(Point3D(0.0, 0.0, 0.0), currentRotX, currentRotY, 0f, centerX, centerY)

                    // 1. Solar Corona Rings (Orbits in Z-space)
                    val rings = listOf(1.3, 1.6)
                    rings.forEachIndexed { ringIdx, ringMult ->
                        val numSegments = 16
                        val ringRadius = sunRadius * ringMult
                        for (i in 0 until numSegments) {
                            val angle = (i * 360.0 / numSegments) + (pulseProgress * 360.0 * (ringIdx + 1) * 0.1)
                            val rad = Math.toRadians(angle)
                            val z = sin(rad * 3) * 20.0
                            val p = project3D(Point3D(cos(rad) * ringRadius, sin(rad) * ringRadius, z), currentRotX, currentRotY, 0f, centerX, centerY)
                            
                            val alpha = (0.2f + 0.4f * p.lightIntensity).coerceIn(0.1f, 0.6f)
                            drawCircle(
                                color = if (ringIdx == 0) Color(0xFFFFD700).copy(alpha = alpha) else Color(0xFFFF8C00).copy(alpha = alpha),
                                radius = 8f * p.scale,
                                center = Offset(p.screenX, p.screenY)
                            )
                        }
                    }

                    // 2. Volumetric Sun Body
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFFDE7),
                                Color(0xFFFFD54F),
                                Color(0xFFF57C00),
                                Color(0xFFE65100)
                            ),
                            center = Offset(sunCenter.screenX - 20f * sunCenter.scale, sunCenter.screenY - 20f * sunCenter.scale),
                            radius = sunRadius * 1.4f * sunCenter.scale
                        ),
                        radius = sunRadius * sunCenter.scale,
                        center = Offset(sunCenter.screenX, sunCenter.screenY)
                    )
                }

                WeatherCondition.THUNDERSTORM -> {
                    // --- 3D STORM CELL ---
                    val lobes = listOf(
                        Lobe3D(Point3D(0.0, 0.0, 30.0), 70f * baseScale, Color(0xFF374151), Color(0xFF111827)),
                        Lobe3D(Point3D(-45.0, 20.0, 0.0), 55f * baseScale, Color(0xFF4B5563), Color(0xFF1E293B)),
                        Lobe3D(Point3D(45.0, -15.0, -20.0), 50f * baseScale, Color(0xFF374151), Color(0xFF0F172A)),
                        Lobe3D(Point3D(0.0, -35.0, 10.0), 45f * baseScale, Color(0xFF1F2937), Color(0xFF020617))
                    )

                    val projected = lobes.map { it to project3D(it.pos, currentRotX, currentRotY, 0f, centerX, centerY) }
                        .sortedBy { it.second.zDepth }

                    projected.forEach { (lobe, p) ->
                        val r = lobe.radius * p.scale
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    lobe.colorPrimary.copy(alpha = 0.9f + 0.1f * p.lightIntensity),
                                    lobe.colorShadow
                                ),
                                center = Offset(p.screenX - r * 0.3f, p.screenY - r * 0.3f),
                                radius = r * 1.3f
                            ),
                            radius = r,
                            center = Offset(p.screenX, p.screenY)
                        )
                    }

                    // 3D Arcing Lightning Bolt
                    if (pulseProgress > 0.85f) {
                        val boltNodes = listOf(
                            Point3D(0.0, 0.0, 30.0),
                            Point3D(20.0, 40.0, 40.0),
                            Point3D(-10.0, 80.0, 20.0),
                            Point3D(10.0, 120.0, 0.0)
                        )
                        val projectedNodes = boltNodes.map { project3D(it, currentRotX, currentRotY, 0f, centerX, centerY) }
                        val path = Path().apply {
                            moveTo(projectedNodes[0].screenX, projectedNodes[0].screenY)
                            projectedNodes.drop(1).forEach { lineTo(it.screenX, it.screenY) }
                        }
                        drawPath(path, Color(0xFF7DD3FC), style = Stroke(width = 6f, cap = StrokeCap.Round))
                        drawPath(path, Color.White, style = Stroke(width = 2f, cap = StrokeCap.Round))
                    }
                }

                WeatherCondition.SNOWY -> {
                    // --- 3D FRACTAL SNOWFLAKE ---
                    val center = project3D(Point3D(0.0, 0.0, 0.0), currentRotX, currentRotY, 0f, centerX, centerY)
                    
                    for (i in 0 until 6) {
                        val angle = i * 60.0
                        val rad = Math.toRadians(angle)
                        val tip = project3D(Point3D(cos(rad) * 90.0 * baseScale, sin(rad) * 90.0 * baseScale, 0.0), currentRotX, currentRotY, 0f, centerX, centerY)
                        
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
                            
                            val t1 = project3D(Point3D(subStart.x + cos(subAngle1) * subLen, subStart.y + sin(subAngle1) * subLen, 10.0), currentRotX, currentRotY, 0f, centerX, centerY)
                            val t2 = project3D(Point3D(subStart.x + cos(subAngle2) * subLen, subStart.y + sin(subAngle2) * subLen, -10.0), currentRotX, currentRotY, 0f, centerX, centerY)
                            
                            drawLine(Color(0xFFBAE6FD), Offset(subStartP.screenX, subStartP.screenY), Offset(t1.screenX, t1.screenY), 4f * t1.scale, StrokeCap.Round)
                            drawLine(Color(0xFFBAE6FD), Offset(subStartP.screenX, subStartP.screenY), Offset(t2.screenX, t2.screenY), 4f * t2.scale, StrokeCap.Round)
                        }
                    }
                }

                else -> {
                    // --- VOLUMETRIC CUMULUS CLOUD ---
                    val isRainy = condition == WeatherCondition.RAINY || condition == WeatherCondition.HEAVY_RAIN
                    val topColor = if (isRainy) Color(0xFF64748B) else Color.White
                    val bottomColor = if (isRainy) Color(0xFF334155) else Color(0xFFCBD5E1)

                    val cloudLobes = listOf(
                        Lobe3D(Point3D(0.0, 0.0, 20.0), 75f * baseScale, topColor, bottomColor),
                        Lobe3D(Point3D(-50.0, 10.0, -10.0), 60f * baseScale, topColor, bottomColor),
                        Lobe3D(Point3D(50.0, -15.0, 0.0), 55f * baseScale, topColor, bottomColor),
                        Lobe3D(Point3D(-20.0, -30.0, 30.0), 50f * baseScale, topColor, bottomColor),
                        Lobe3D(Point3D(20.0, 25.0, -25.0), 45f * baseScale, topColor, bottomColor)
                    )

                    val projected = cloudLobes.map { it to project3D(it.pos, currentRotX, currentRotY, 0f, centerX, centerY) }
                        .sortedBy { it.second.zDepth }

                    projected.forEach { (lobe, p) ->
                        val r = lobe.radius * p.scale
                        val color = lerpColor(lobe.colorShadow, lobe.colorPrimary, p.lightIntensity)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(color, lobe.colorShadow),
                                center = Offset(p.screenX - r * 0.25f, p.screenY - r * 0.25f),
                                radius = r * 1.2f
                            ),
                            radius = r,
                            center = Offset(p.screenX, p.screenY)
                        )
                    }
                    
                    if (isRainy) {
                        for (i in 0 until 12) {
                            val x = (i - 6) * 20.0
                            val dropP = project3D(Point3D(x, 40.0 + (pulseProgress * 60.0), (i % 3) * 20.0 - 20.0), currentRotX, currentRotY, 0f, centerX, centerY)
                            drawLine(
                                color = Color(0xFF38BDF8).copy(alpha = 0.6f * dropP.scale),
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

private fun lerpColor(start: Color, end: Color, t: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * t,
        green = start.green + (end.green - start.green) * t,
        blue = start.blue + (end.blue - start.blue) * t,
        alpha = start.alpha + (end.alpha - start.alpha) * t
    )
}
