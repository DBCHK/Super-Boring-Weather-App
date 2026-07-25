package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ThreeDMoonCanvas(
    illuminationPercent: Int = 13,
    phaseName: String = "WANING CRESCENT",
    modifier: Modifier = Modifier
) {
    val interaction = rememberInteractive3DState(
        initialPitch = 0f,
        autoSpinDegPerSec = 14f,
        maxPitch = 25f
    )

    val rotationAngle = interaction.yaw

    Box(
        modifier = modifier.interactive3D(interaction, enablePitch = false),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val moonRadius = size.width.coerceAtMost(size.height) * 0.38f

            // Soft ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = moonRadius * 1.35f
                ),
                radius = moonRadius * 1.35f,
                center = Offset(centerX, centerY)
            )

            // Base dark moon sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3A3A3C),
                        Color(0xFF1C1C1E),
                        Color(0xFF0A0A0B)
                    ),
                    center = Offset(centerX - moonRadius * 0.25f, centerY - moonRadius * 0.25f),
                    radius = moonRadius * 1.15f
                ),
                radius = moonRadius,
                center = Offset(centerX, centerY)
            )

            // Craters that rotate with yaw for a real 3D feel
            val craterOffsets = listOf(
                Offset(-0.30f, -0.20f) to 0.12f,
                Offset(0.20f, -0.40f) to 0.18f,
                Offset(-0.10f, 0.30f) to 0.15f,
                Offset(0.35f, 0.20f) to 0.10f,
                Offset(-0.40f, 0.10f) to 0.08f,
                Offset(0.10f, 0.10f) to 0.09f,
                Offset(-0.15f, -0.45f) to 0.07f,
                Offset(0.28f, 0.38f) to 0.11f
            )

            val rad = Math.toRadians(rotationAngle.toDouble())
            craterOffsets.forEach { (relOffset, relSize) ->
                // Sphere-ish rotation: x/z plane, hide far-side craters
                val rx = relOffset.x * cos(rad)
                val rz = relOffset.x * sin(rad)
                val ry = relOffset.y

                // Only draw craters on the front hemisphere
                if (rz > -0.15) {
                    val cX = centerX + rx.toFloat() * moonRadius
                    val cY = centerY + ry * moonRadius
                    val depthScale = (0.75f + 0.25f * ((rz + 1.0) / 2.0).toFloat()).coerceIn(0.55f, 1f)
                    val cR = relSize * moonRadius * depthScale
                    val alpha = (0.55f + 0.45f * depthScale).coerceIn(0.4f, 1f)

                    drawCircle(
                        color = Color(0xFF0F0F10).copy(alpha = alpha),
                        radius = cR,
                        center = Offset(cX, cY)
                    )
                    drawCircle(
                        color = Color(0xFF4A4A4C).copy(alpha = 0.35f * alpha),
                        radius = cR * 0.75f,
                        center = Offset(cX - cR * 0.18f, cY - cR * 0.18f)
                    )
                }
            }

            // Illuminated phase shading
            val illumFactor = (illuminationPercent / 100f).coerceIn(0f, 1f)
            val shadowOffset = (1f - illumFactor * 2f) * moonRadius

            // Lit surface gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFE5E5EA),
                        Color(0xFFD1D1D6),
                        Color.Transparent
                    ),
                    center = Offset(
                        centerX - moonRadius * 0.55f + shadowOffset * 0.35f,
                        centerY - moonRadius * 0.2f
                    ),
                    radius = moonRadius * 1.15f
                ),
                radius = moonRadius,
                center = Offset(centerX, centerY)
            )

            // Crescent shadow mask
            if (illumFactor < 0.98f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1C1C1E).copy(alpha = 0.92f),
                            Color(0xFF000000).copy(alpha = 0.97f)
                        ),
                        center = Offset(centerX + shadowOffset, centerY),
                        radius = moonRadius * 1.05f
                    ),
                    radius = moonRadius,
                    center = Offset(centerX + shadowOffset * 0.75f, centerY)
                )
            }

            // Rim highlight that follows rotation slightly
            val rimShift = (sin(rad) * moonRadius * 0.08).toFloat()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(centerX + rimShift, centerY - moonRadius * 0.1f),
                    radius = moonRadius * 1.12f
                ),
                radius = moonRadius * 1.12f,
                center = Offset(centerX, centerY)
            )
        }
    }
}
