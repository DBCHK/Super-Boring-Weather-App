package com.example.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
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
import com.example.data.model.WeatherCondition
import kotlin.math.cos
import kotlin.math.sin

private data class Point3D(val x: Double, val y: Double, val z: Double)

private data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val zDepth: Float,
    val scale: Float
)

private data class Lobe3D(
    val pos: Point3D,
    val radius: Float,
    val colorPrimary: Color,
    val colorShadow: Color
)

private fun project3D(
    p: Point3D,
    rotXDeg: Float,
    rotYDeg: Float,
    rotZDeg: Float,
    centerX: Float,
    centerY: Float,
    perspective: Float = 500f
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

    val scale = (perspective / (perspective + z3)).coerceIn(0.1, 4.0).toFloat()
    val screenX = centerX + (x3 * scale).toFloat()
    val screenY = centerY + (y3 * scale).toFloat()

    return ProjectedPoint(screenX, screenY, z3.toFloat(), scale)
}

@Composable
fun ThreeDWeatherCanvas(
    condition: WeatherCondition,
    isDaytime: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "3DAnimation")

    val autoRotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "autoRotate"
    )

    val bobbingY by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
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

    // Interactive 360-degree touch rotation angles
    var rotX by remember { mutableFloatStateOf(15f) }
    var rotY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Rotates 360 degrees smoothly in any direction
                    rotY = (rotY + dragAmount.x * 0.75f) % 360f
                    rotX = (rotX - dragAmount.y * 0.75f) % 360f
                }
            }
            .graphicsLayer {
                cameraDistance = 16f * density
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f + bobbingY
            val baseScale = (size.width.coerceAtMost(size.height) / 320f).coerceAtLeast(0.8f)

            val currentRotX = rotX
            val currentRotY = rotY + autoRotateAngle * 0.15f // subtle idle spin

            when (condition) {
                WeatherCondition.SUNNY, WeatherCondition.CLEAR -> {
                    // --- 3D SUN WITH VOLUMETRIC SPHERE & ORBITING 3D CORONA RAYS ---
                    val sunRadius = 70f * baseScale

                    // 1. Draw 3D Solar Corona Rays in Z depth ring
                    val numRays = 12
                    val rayInnerR = sunRadius * 1.15
                    val rayOuterR = sunRadius * 1.75

                    data class Ray3D(val start: Point3D, val end: Point3D)
                    val rayList = mutableListOf<Ray3D>()

                    for (i in 0 until numRays) {
                        val angle = (i * 360.0 / numRays) + (autoRotateAngle * 0.5)
                        val rad = Math.toRadians(angle)
                        val zOffset = sin(rad * 2) * 20.0
                        val startP = Point3D(cos(rad) * rayInnerR, sin(rad) * rayInnerR, zOffset)
                        val endP = Point3D(cos(rad) * rayOuterR, sin(rad) * rayOuterR, zOffset)
                        rayList.add(Ray3D(startP, endP))
                    }

                    // Project & sort rays by depth
                    val projectedRays = rayList.map { ray ->
                        val p1 = project3D(ray.start, currentRotX, currentRotY, 0f, centerX, centerY)
                        val p2 = project3D(ray.end, currentRotX, currentRotY, 0f, centerX, centerY)
                        val avgZ = (p1.zDepth + p2.zDepth) / 2f
                        Triple(p1, p2, avgZ)
                    }.sortedBy { it.third }

                    // Project Sun Sphere
                    val sunCenterP = project3D(Point3D(0.0, 0.0, 0.0), currentRotX, currentRotY, 0f, centerX, centerY)

                    // Draw back rays (Z < 0)
                    projectedRays.filter { it.third < 0 }.forEach { (p1, p2, _) ->
                        drawLine(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFD700), Color(0xFFFF8C00)),
                                center = Offset(p1.screenX, p1.screenY),
                                radius = 40f
                            ),
                            start = Offset(p1.screenX, p1.screenY),
                            end = Offset(p2.screenX, p2.screenY),
                            strokeWidth = 6f * p1.scale,
                            cap = StrokeCap.Round
                        )
                    }

                    // Draw 3D Sun Sphere with 3D Specular Highlight
                    val lightOffsetX = -sunRadius * 0.35f * sunCenterP.scale
                    val lightOffsetY = -sunRadius * 0.35f * sunCenterP.scale
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF9C4), // Bright specular core
                                Color(0xFFFFD54F),
                                Color(0xFFFF9800), // Deep orange rim
                                Color(0xFFF57C00)
                            ),
                            center = Offset(sunCenterP.screenX + lightOffsetX, sunCenterP.screenY + lightOffsetY),
                            radius = sunRadius * 1.2f * sunCenterP.scale
                        ),
                        radius = sunRadius * sunCenterP.scale,
                        center = Offset(sunCenterP.screenX, sunCenterP.screenY)
                    )

                    // Draw front rays (Z >= 0)
                    projectedRays.filter { it.third >= 0 }.forEach { (p1, p2, _) ->
                        drawLine(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFF59D), Color(0xFFFFB300)),
                                center = Offset(p1.screenX, p1.screenY),
                                radius = 40f
                            ),
                            start = Offset(p1.screenX, p1.screenY),
                            end = Offset(p2.screenX, p2.screenY),
                            strokeWidth = 7f * p1.scale,
                            cap = StrokeCap.Round
                        )
                    }
                }

                WeatherCondition.THUNDERSTORM -> {
                    // --- 3D VOLUMETRIC STORM CLOUD & ELECTRIC LIGHTNING BOLTS ---
                    val lobes = listOf(
                        Lobe3D(Point3D(0.0, -10.0, 25.0), 62f * baseScale, Color(0xFF374151), Color(0xFF1F2937)),
                        Lobe3D(Point3D(-55.0, 10.0, -10.0), 52f * baseScale, Color(0xFF4B5563), Color(0xFF111827)),
                        Lobe3D(Point3D(55.0, 15.0, 0.0), 48f * baseScale, Color(0xFF374151), Color(0xFF1F2937)),
                        Lobe3D(Point3D(-25.0, -35.0, 10.0), 44f * baseScale, Color(0xFF4B5563), Color(0xFF1E293B)),
                        Lobe3D(Point3D(25.0, -30.0, -15.0), 42f * baseScale, Color(0xFF374151), Color(0xFF0F172A)),
                        Lobe3D(Point3D(0.0, 30.0, -20.0), 38f * baseScale, Color(0xFF1F2937), Color(0xFF020617))
                    )

                    val projectedLobes = lobes.map { lobe ->
                        val proj = project3D(lobe.pos, currentRotX, currentRotY, 0f, centerX, centerY)
                        Pair(lobe, proj)
                    }.sortedBy { it.second.zDepth }

                    // Render sorted 3D cloud lobes
                    projectedLobes.forEach { (lobe, proj) ->
                        val r = lobe.radius * proj.scale
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(lobe.colorPrimary, lobe.colorShadow),
                                center = Offset(proj.screenX - r * 0.2f, proj.screenY - r * 0.2f),
                                radius = r * 1.1f
                            ),
                            radius = r,
                            center = Offset(proj.screenX, proj.screenY)
                        )
                    }

                    // 3D Arcing Lightning Bolt
                    val boltNodes = listOf(
                        Point3D(0.0, 20.0, 10.0),
                        Point3D(-20.0, 50.0, 15.0),
                        Point3D(10.0, 70.0, 5.0),
                        Point3D(-15.0, 110.0, -10.0)
                    )

                    val projNodes = boltNodes.map { project3D(it, currentRotX, currentRotY, 0f, centerX, centerY) }
                    val path = Path().apply {
                        moveTo(projNodes[0].screenX, projNodes[0].screenY)
                        for (i in 1 until projNodes.size) {
                            lineTo(projNodes[i].screenX, projNodes[i].screenY)
                        }
                    }

                    // Electric Glow
                    drawPath(
                        path = path,
                        color = Color(0xFF38BDF8).copy(alpha = 0.8f),
                        style = Stroke(width = 10f * projNodes[1].scale, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(width = 4f * projNodes[1].scale, cap = StrokeCap.Round)
                    )
                }

                WeatherCondition.SNOWY -> {
                    // --- 3D CRYSTALLINE SNOWFLAKE WITH BRANCHING ARMS ---
                    val armLength = 85.0 * baseScale
                    val numArms = 6
                    val projectedArms = mutableListOf<Pair<ProjectedPoint, ProjectedPoint>>()

                    for (i in 0 until numArms) {
                        val angle = i * (360.0 / numArms) + (autoRotateAngle * 0.3)
                        val rad = Math.toRadians(angle)
                        val pCenter = Point3D(0.0, 0.0, 0.0)
                        val pTip = Point3D(cos(rad) * armLength, sin(rad) * armLength, sin(rad * 3) * 15.0)

                        val projC = project3D(pCenter, currentRotX, currentRotY, 0f, centerX, centerY)
                        val projT = project3D(pTip, currentRotX, currentRotY, 0f, centerX, centerY)
                        projectedArms.add(Pair(projC, projT))
                    }

                    projectedArms.forEach { (pC, pT) ->
                        // Primary Branch
                        drawLine(
                            color = Color(0xFFE0F2FE),
                            start = Offset(pC.screenX, pC.screenY),
                            end = Offset(pT.screenX, pT.screenY),
                            strokeWidth = 5f * pT.scale,
                            cap = StrokeCap.Round
                        )

                        // Sub-branches
                        val midX = (pC.screenX + pT.screenX) / 2f
                        val midY = (pC.screenY + pT.screenY) / 2f
                        drawCircle(
                            color = Color.White,
                            radius = 6f * pT.scale,
                            center = Offset(midX, midY)
                        )
                        drawCircle(
                            color = Color(0xFFBAE6FD),
                            radius = 8f * pT.scale,
                            center = Offset(pT.screenX, pT.screenY)
                        )
                    }

                    // Center Snowflake Gem
                    val centerP = project3D(Point3D(0.0, 0.0, 0.0), currentRotX, currentRotY, 0f, centerX, centerY)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFF7DD3FC)),
                            center = Offset(centerP.screenX, centerP.screenY),
                            radius = 24f * centerP.scale
                        ),
                        radius = 20f * centerP.scale,
                        center = Offset(centerP.screenX, centerP.screenY)
                    )
                }

                WeatherCondition.HAZE -> {
                    // --- 3D HAZE / MIST PARTICLES WITH DEPTH PARALLAX ---
                    val mistDiscs = mutableListOf<Lobe3D>()
                    val random = java.util.Random(1234)
                    for (i in 0 until 18) {
                        val x = (random.nextDouble() - 0.5) * 220.0
                        val y = (random.nextDouble() - 0.5) * 140.0
                        val z = (random.nextDouble() - 0.5) * 160.0
                        val r = (35 + random.nextInt(30)).toFloat() * baseScale
                        mistDiscs.add(Lobe3D(Point3D(x, y, z), r, Color(0xFFE2E8F0), Color(0xFF94A3B8)))
                    }

                    val projDiscs = mistDiscs.map { disc ->
                        val proj = project3D(disc.pos, currentRotX, currentRotY, 0f, centerX, centerY)
                        Pair(disc, proj)
                    }.sortedBy { it.second.zDepth }

                    projDiscs.forEach { (disc, proj) ->
                        val r = disc.radius * proj.scale
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color(0xFFCBD5E1).copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                center = Offset(proj.screenX, proj.screenY),
                                radius = r * 1.2f
                            ),
                            radius = r,
                            center = Offset(proj.screenX, proj.screenY)
                        )
                    }
                }

                else -> {
                    // --- 3D VOLUMETRIC CUMULUS CLOUD (CLOUDY / RAINY / PARTLY CLOUDY) ---
                    val isRainy = condition == WeatherCondition.RAINY || condition == WeatherCondition.HEAVY_RAIN
                    val topColor = if (isRainy) Color(0xFF64748B) else Color.White
                    val bottomColor = if (isRainy) Color(0xFF334155) else Color(0xFFCBD5E1)

                    val cloudLobes = listOf(
                        Lobe3D(Point3D(0.0, -12.0, 25.0), 65f * baseScale, topColor, bottomColor),
                        Lobe3D(Point3D(-55.0, 12.0, -10.0), 55f * baseScale, topColor, bottomColor),
                        Lobe3D(Point3D(55.0, 15.0, 0.0), 50f * baseScale, topColor, bottomColor),
                        Lobe3D(Point3D(-25.0, -32.0, 10.0), 48f * baseScale, topColor, bottomColor),
                        Lobe3D(Point3D(25.0, -28.0, -15.0), 45f * baseScale, topColor, bottomColor),
                        Lobe3D(Point3D(0.0, 28.0, -20.0), 40f * baseScale, topColor, bottomColor),
                        Lobe3D(Point3D(-70.0, 25.0, 5.0), 35f * baseScale, topColor, bottomColor),
                        Lobe3D(Point3D(70.0, 20.0, -5.0), 35f * baseScale, topColor, bottomColor)
                    )

                    val projected = cloudLobes.map { lobe ->
                        val proj = project3D(lobe.pos, currentRotX, currentRotY, 0f, centerX, centerY)
                        Pair(lobe, proj)
                    }.sortedBy { it.second.zDepth }

                    projected.forEach { (lobe, proj) ->
                        val r = lobe.radius * proj.scale
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(lobe.colorPrimary, lobe.colorShadow),
                                center = Offset(proj.screenX - r * 0.25f, proj.screenY - r * 0.25f),
                                radius = r * 1.15f
                            ),
                            radius = r,
                            center = Offset(proj.screenX, proj.screenY)
                        )
                    }

                    // 3D Rain drops angled in 3D space if rainy
                    if (isRainy) {
                        for (i in 0 until 14) {
                            val xOffset = (i - 7) * 16.0
                            val startP = Point3D(xOffset, 45.0 + (pulseProgress * 30.0), (i % 3) * 15.0 - 15.0)
                            val endP = Point3D(xOffset - 8.0, 75.0 + (pulseProgress * 30.0), (i % 3) * 15.0 - 15.0)

                            val p1 = project3D(startP, currentRotX, currentRotY, 0f, centerX, centerY)
                            val p2 = project3D(endP, currentRotX, currentRotY, 0f, centerX, centerY)

                            drawLine(
                                color = Color(0xFF38BDF8).copy(alpha = 0.8f),
                                start = Offset(p1.screenX, p1.screenY),
                                end = Offset(p2.screenX, p2.screenY),
                                strokeWidth = 3.5f * p1.scale,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}
