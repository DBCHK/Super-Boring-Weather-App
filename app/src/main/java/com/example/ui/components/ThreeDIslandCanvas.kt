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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

private data class IslandPoint3D(val x: Double, val y: Double, val z: Double)

private data class IslandProjected(
    val x: Float,
    val y: Float,
    val z: Float,
    val scale: Float
)

private fun projectIsland(
    p: IslandPoint3D,
    rotXDeg: Float,
    rotYDeg: Float,
    centerX: Float,
    centerY: Float,
    perspective: Float = 520f
): IslandProjected {
    val radX = Math.toRadians(rotXDeg.toDouble())
    val radY = Math.toRadians(rotYDeg.toDouble())

    // Pitch
    val y1 = p.y * cos(radX) - p.z * sin(radX)
    val z1 = p.y * sin(radX) + p.z * cos(radX)
    val x1 = p.x

    // Yaw
    val x2 = x1 * cos(radY) + z1 * sin(radY)
    val z2 = -x1 * sin(radY) + z1 * cos(radY)
    val y2 = y1

    val depth = (perspective + z2).toFloat().coerceAtLeast(90f)
    val scale = (perspective / depth).coerceIn(0.35f, 2.2f)
    return IslandProjected(
        x = centerX + (x2 * scale).toFloat(),
        y = centerY + (y2 * scale).toFloat(),
        z = z2.toFloat(),
        scale = scale
    )
}

@Composable
fun ThreeDIslandCanvas(
    precipRateInches: Float,
    modifier: Modifier = Modifier
) {
    val interaction = rememberInteractive3DState(
        initialPitch = 22f,
        autoSpinDegPerSec = 12f,
        maxPitch = 55f
    )

    val infiniteTransition = rememberInfiniteTransition(label = "3DIslandAnimation")

    val floatY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "islandFloatY"
    )

    val rainProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainProgress"
    )

    val rotX = interaction.renderPitch
    val rotY = interaction.renderYaw
    // Intensity of rain based on precip rate
    val dropCount = (6 + (precipRateInches * 40f).toInt()).coerceIn(6, 22)

    Box(
        modifier = modifier.interactive3D(interaction, enablePitch = true),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height * 0.52f + floatY
            val base = size.width.coerceAtMost(size.height) * 0.42f

            // Drop shadow (projected under island)
            val shadow = projectIsland(
                IslandPoint3D(0.0, base * 0.55, 0.0),
                rotX, rotY, centerX, centerY
            )
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.28f),
                        Color.Black.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(shadow.x, shadow.y),
                    radius = base * 0.85f * shadow.scale
                ),
                topLeft = Offset(
                    shadow.x - base * 0.85f * shadow.scale,
                    shadow.y - base * 0.28f * shadow.scale
                ),
                size = Size(base * 1.7f * shadow.scale, base * 0.56f * shadow.scale)
            )

            // Terrain mesh points (low-poly island) sorted by depth
            data class Face(
                val points: List<IslandPoint3D>,
                val color: Color
            )

            val halfW = base * 0.95
            val halfD = base * 0.55
            val height = base * 0.42

            val faces = listOf(
                // Top pool surface (slightly raised)
                Face(
                    listOf(
                        IslandPoint3D(-halfW * 0.75, -height * 0.15, -halfD * 0.7),
                        IslandPoint3D(halfW * 0.75, -height * 0.15, -halfD * 0.7),
                        IslandPoint3D(halfW * 0.7, -height * 0.15, halfD * 0.55),
                        IslandPoint3D(-halfW * 0.7, -height * 0.15, halfD * 0.55)
                    ),
                    Color(0xFF0A84FF)
                ),
                // Front cliff
                Face(
                    listOf(
                        IslandPoint3D(-halfW * 0.7, -height * 0.15, halfD * 0.55),
                        IslandPoint3D(halfW * 0.7, -height * 0.15, halfD * 0.55),
                        IslandPoint3D(halfW * 0.55, height * 0.85, halfD * 0.35),
                        IslandPoint3D(-halfW * 0.55, height * 0.85, halfD * 0.35)
                    ),
                    Color(0xFF2C2C2E)
                ),
                // Left cliff
                Face(
                    listOf(
                        IslandPoint3D(-halfW * 0.75, -height * 0.15, -halfD * 0.7),
                        IslandPoint3D(-halfW * 0.7, -height * 0.15, halfD * 0.55),
                        IslandPoint3D(-halfW * 0.55, height * 0.85, halfD * 0.35),
                        IslandPoint3D(-halfW * 0.6, height * 0.85, -halfD * 0.45)
                    ),
                    Color(0xFF1C1C1E)
                ),
                // Right cliff
                Face(
                    listOf(
                        IslandPoint3D(halfW * 0.75, -height * 0.15, -halfD * 0.7),
                        IslandPoint3D(halfW * 0.7, -height * 0.15, halfD * 0.55),
                        IslandPoint3D(halfW * 0.55, height * 0.85, halfD * 0.35),
                        IslandPoint3D(halfW * 0.6, height * 0.85, -halfD * 0.45)
                    ),
                    Color(0xFF3A3A3C)
                ),
                // Back cliff
                Face(
                    listOf(
                        IslandPoint3D(-halfW * 0.75, -height * 0.15, -halfD * 0.7),
                        IslandPoint3D(halfW * 0.75, -height * 0.15, -halfD * 0.7),
                        IslandPoint3D(halfW * 0.6, height * 0.85, -halfD * 0.45),
                        IslandPoint3D(-halfW * 0.6, height * 0.85, -halfD * 0.45)
                    ),
                    Color(0xFF48484A)
                )
            )

            val projectedFaces = faces.map { face ->
                val projected = face.points.map { projectIsland(it, rotX, rotY, centerX, centerY) }
                val avgZ = projected.map { it.z }.average().toFloat()
                Triple(face, projected, avgZ)
            }.sortedByDescending { it.third } // far faces first

            projectedFaces.forEach { (face, projected, _) ->
                val path = Path().apply {
                    moveTo(projected[0].x, projected[0].y)
                    projected.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
                val avgScale = projected.map { it.scale }.average().toFloat()
                // Lighting based on face depth / scale
                val light = (0.55f + 0.45f * avgScale.coerceIn(0.5f, 1.2f)).coerceIn(0.4f, 1f)
                val lit = Color(
                    red = (face.color.red * light).coerceIn(0f, 1f),
                    green = (face.color.green * light).coerceIn(0f, 1f),
                    blue = (face.color.blue * light).coerceIn(0f, 1f),
                    alpha = 1f
                )
                drawPath(path, lit)
                drawPath(path, Color.White.copy(alpha = 0.08f), style = Stroke(width = 1.5f))
            }

            // Water surface highlight oval projected on top
            val poolCorners = listOf(
                IslandPoint3D(-halfW * 0.55, -height * 0.22, -halfD * 0.45),
                IslandPoint3D(halfW * 0.55, -height * 0.22, -halfD * 0.45),
                IslandPoint3D(halfW * 0.5, -height * 0.22, halfD * 0.35),
                IslandPoint3D(-halfW * 0.5, -height * 0.22, halfD * 0.35)
            ).map { projectIsland(it, rotX, rotY, centerX, centerY) }

            val poolPath = Path().apply {
                moveTo(poolCorners[0].x, poolCorners[0].y)
                poolCorners.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(
                path = poolPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF64D2FF),
                        Color(0xFF007AFF),
                        Color(0xFF0040A0)
                    ),
                    start = Offset(poolCorners[0].x, poolCorners[0].y),
                    end = Offset(poolCorners[2].x, poolCorners[2].y)
                )
            )
            drawPath(
                path = poolPath,
                color = Color(0xFF64D2FF).copy(alpha = 0.55f),
                style = Stroke(width = 2.5f)
            )

            // Raindrops in 3D space falling onto the island
            for (i in 0 until dropCount) {
                val x = ((i * 37) % 160 - 80) / 100.0 * halfW
                val z = ((i * 53) % 120 - 60) / 100.0 * halfD
                val phase = (rainProgress + i * 0.09f) % 1f
                val yStart = -height * 1.6
                val yEnd = -height * 0.05
                val y = yStart + (yEnd - yStart) * phase

                val dropTop = projectIsland(IslandPoint3D(x, y, z), rotX, rotY, centerX, centerY)
                val dropBot = projectIsland(
                    IslandPoint3D(x, y + height * 0.18, z),
                    rotX, rotY, centerX, centerY
                )

                if (phase < 0.92f) {
                    drawLine(
                        color = Color(0xFF64D2FF).copy(alpha = (0.25f + phase * 0.7f).coerceIn(0.2f, 0.9f)),
                        start = Offset(dropTop.x, dropTop.y),
                        end = Offset(dropBot.x, dropBot.y),
                        strokeWidth = 4.5f * dropTop.scale,
                        cap = StrokeCap.Round
                    )
                } else {
                    // Ripple on impact
                    val ripple = ((phase - 0.92f) / 0.08f).coerceIn(0f, 1f)
                    val r = 10f * ripple * dropTop.scale
                    drawOval(
                        color = Color(0xFF64D2FF).copy(alpha = (1f - ripple) * 0.55f),
                        topLeft = Offset(dropBot.x - r, dropBot.y - r * 0.4f),
                        size = Size(r * 2f, r * 0.8f),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
    }
}
