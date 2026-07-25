package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import com.example.util.PianoSoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ThreeDMoonCanvas(
    illuminationPercent: Int = 13,
    phaseName: String = "WANING CRESCENT",
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val context = view.context.applicationContext
    val soundManager = remember { PianoSoundManager(context) }
    val scope = rememberCoroutineScope()

    var touchRotY by remember { mutableFloatStateOf(0f) }
    var velX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Auto-rotation effect when not dragging
    LaunchedEffect(isDragging) {
        if (!isDragging) {
            while (true) {
                touchRotY += 0.3f
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
                        scope.launch {
                            var currentVx = velX
                            while (!isDragging && kotlin.math.abs(currentVx) > 0.05f) {
                                touchRotY += currentVx
                                currentVx *= 0.94f
                                delay(16)
                            }
                        }
                    },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        velX = dragAmount.x * 0.4f
                        touchRotY += velX
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val moonRadius = (size.width.coerceAtMost(size.height) * 0.38f)

            val rotationAngle = touchRotY

            // 1. Base Dark Moon Sphere (Shadow side)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2C2C2E),
                        Color(0xFF1C1C1E),
                        Color(0xFF0F0F10)
                    ),
                    center = Offset(centerX, centerY),
                    radius = moonRadius
                ),
                radius = moonRadius,
                center = Offset(centerX, centerY)
            )

            // 2. Draw Moon Craters on dark surface
            val craterOffsets = listOf(
                Offset(-0.3f, -0.2f) to 0.12f,
                Offset(0.2f, -0.4f) to 0.18f,
                Offset(-0.1f, 0.3f) to 0.15f,
                Offset(0.35f, 0.2f) to 0.1f,
                Offset(-0.4f, 0.1f) to 0.08f,
                Offset(0.1f, 0.1f) to 0.09f
            )

            craterOffsets.forEach { (relOffset, relSize) ->
                val rad = Math.toRadians(rotationAngle.toDouble())
                val rx = relOffset.x * cos(rad) - relOffset.y * sin(rad)
                val ry = relOffset.x * sin(rad) + relOffset.y * cos(rad)

                val cX = centerX + rx.toFloat() * moonRadius
                val cY = centerY + ry.toFloat() * moonRadius
                val cR = relSize * moonRadius

                if (rx * rx + ry * ry < 0.85) {
                    drawCircle(
                        color = Color(0xFF151516),
                        radius = cR,
                        center = Offset(cX, cY)
                    )
                    drawCircle(
                        color = Color(0xFF3A3A3C).copy(alpha = 0.3f),
                        radius = cR * 0.8f,
                        center = Offset(cX - cR * 0.2f, cY - cR * 0.2f)
                    )
                }
            }

            // 3. Draw Dynamic Crescent / Illuminated Phase Shading
            // Illumination angle: 0% = New Moon, 50% = Quarter, 100% = Full Moon
            val illumFactor = (illuminationPercent / 100f).coerceIn(0f, 1f)
            val shadowOffset = (1f - illumFactor * 2f) * moonRadius

            // Lit Crescent Arc Overlay
            val crescentPath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        centerX - moonRadius,
                        centerY - moonRadius,
                        centerX + moonRadius,
                        centerY + moonRadius
                    )
                )
            }

            // Draw Illuminated Silver/White Gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFE5E5EA),
                        Color(0xFFD1D1D6),
                        Color.Transparent
                    ),
                    center = Offset(centerX - moonRadius * 0.6f + shadowOffset, centerY - moonRadius * 0.2f),
                    radius = moonRadius * 1.2f
                ),
                radius = moonRadius,
                center = Offset(centerX, centerY)
            )

            // Re-apply dark shadow mask for exact crescent phase geometry
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1C1C1E).copy(alpha = 0.95f),
                        Color(0xFF000000).copy(alpha = 0.98f)
                    ),
                    center = Offset(centerX + shadowOffset, centerY),
                    radius = moonRadius * 1.1f
                ),
                radius = moonRadius,
                center = Offset(centerX + shadowOffset * 0.8f, centerY)
            )

            // Outer Soft Moon Rim Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = moonRadius * 1.15f
                ),
                radius = moonRadius * 1.15f,
                center = Offset(centerX, centerY)
            )
        }
    }
}
