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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.cos
import kotlin.math.sin

/**
 * Reference-style lunar disc (Not Boring): soft grey sphere with realistic phase terminator.
 * Illumination 0 = new, 50 = quarter, 100 = full. Waxing/waning via [isWaxing].
 */
@Composable
fun ThreeDMoonCanvas(
    illuminationPercent: Int = 13,
    phaseName: String = "WANING CRESCENT",
    isWaxing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interaction = rememberInteractive3DState(
        initialPitch = 6f,
        initialYaw = 0f,
        autoSpinDegPerSec = 6f,
        maxPitch = 18f,
        maxYaw = 28f,
        autoSpinOscillate = true
    )

    val rotationAngle = interaction.renderYaw
    val tiltPitch = interaction.renderPitch

    Box(
        modifier = modifier.interactive3D(interaction, enablePitch = true),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f + rotationAngle * 0.35f
            val centerY = size.height / 2f + tiltPitch * 0.4f
            val moonRadius = size.minDimension * 0.42f

            // Soft ambient glow behind disc
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFB0B0B8).copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = moonRadius * 1.55f
                ),
                radius = moonRadius * 1.55f,
                center = Offset(centerX, centerY)
            )

            val moonClip = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        centerX - moonRadius,
                        centerY - moonRadius,
                        centerX + moonRadius,
                        centerY + moonRadius
                    )
                )
            }

            // Base sphere — warm grey body like the reference
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFC8C8CE),
                        Color(0xFF8E8E93),
                        Color(0xFF3A3A3C),
                        Color(0xFF1C1C1E)
                    ),
                    center = Offset(centerX - moonRadius * 0.35f, centerY - moonRadius * 0.4f),
                    radius = moonRadius * 1.35f
                ),
                radius = moonRadius,
                center = Offset(centerX, centerY)
            )

            // Craters (rotate with yaw)
            val craterOffsets = listOf(
                Offset(-0.28f, -0.22f) to 0.13f,
                Offset(0.18f, -0.38f) to 0.17f,
                Offset(-0.08f, 0.28f) to 0.14f,
                Offset(0.32f, 0.18f) to 0.10f,
                Offset(-0.38f, 0.08f) to 0.09f,
                Offset(0.08f, 0.08f) to 0.08f,
                Offset(-0.12f, -0.42f) to 0.07f,
                Offset(0.26f, 0.36f) to 0.11f,
                Offset(0.05f, -0.15f) to 0.06f
            )
            val rad = Math.toRadians(rotationAngle.toDouble())
            craterOffsets.forEach { (relOffset, relSize) ->
                val rx = relOffset.x * cos(rad)
                val rz = relOffset.x * sin(rad)
                val ry = relOffset.y
                if (rz > -0.2) {
                    val cX = centerX + rx.toFloat() * moonRadius
                    val cY = centerY + ry * moonRadius
                    val depthScale = (0.7f + 0.3f * ((rz + 1.0) / 2.0).toFloat()).coerceIn(0.5f, 1f)
                    val cR = relSize * moonRadius * depthScale
                    drawCircle(
                        color = Color(0xFF2C2C2E).copy(alpha = 0.55f * depthScale),
                        radius = cR,
                        center = Offset(cX, cY)
                    )
                    drawCircle(
                        color = Color(0xFF5A5A5E).copy(alpha = 0.25f * depthScale),
                        radius = cR * 0.65f,
                        center = Offset(cX - cR * 0.2f, cY - cR * 0.2f)
                    )
                }
            }

            // Phase shadow — classic terminator (reference: dark right side for waning crescent)
            val illum = (illuminationPercent / 100f).coerceIn(0f, 1f)
            clipPath(moonClip) {
                if (illum < 0.995f) {
                    // For waxing, light is on the right; waning, light on the left (reference).
                    val lightOnLeft = !isWaxing
                    // Offset of the dark circle: at 0% fully covers, at 50% half, at 100% gone
                    val cover = 1f - illum
                    // Ellipse-ish shadow via large offset circle
                    val shadowShift = if (lightOnLeft) {
                        // Dark mass moves right as illum grows from left-lit crescent
                        moonRadius * (2f * cover - 1f) * 1.05f
                    } else {
                        -moonRadius * (2f * cover - 1f) * 1.05f
                    }

                    // Soft terminator
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0A0A0B).copy(alpha = 0.98f),
                                Color(0xFF121214).copy(alpha = 0.96f),
                                Color(0xFF000000).copy(alpha = 0.99f)
                            ),
                            center = Offset(centerX + shadowShift * 0.15f, centerY),
                            radius = moonRadius * 1.15f
                        ),
                        radius = moonRadius * 1.02f,
                        center = Offset(centerX + shadowShift, centerY)
                    )
                }
            }

            // Thin rim light on the lit edge
            val rimX = if (!isWaxing) centerX - moonRadius * 0.55f else centerX + moonRadius * 0.55f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(rimX, centerY - moonRadius * 0.1f),
                    radius = moonRadius * 0.85f
                ),
                radius = moonRadius * 0.85f,
                center = Offset(rimX, centerY - moonRadius * 0.1f)
            )
        }
    }
}
