package com.example.ui.components

import android.view.HapticFeedbackConstants
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import com.example.util.PianoSoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ThreeDIslandCanvas(
    precipRateInches: Float,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val context = view.context.applicationContext
    val soundManager = remember { PianoSoundManager(context) }
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "3DIslandAnimation")

    // Floating animation for the 3D terrain island
    val floatY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "islandFloatY"
    )

    // Raindrop falling progress (0f..1f)
    val rainProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainProgress"
    )

    var rotX by remember { mutableFloatStateOf(10f) }
    var rotY by remember { mutableFloatStateOf(0f) }
    var velX by remember { mutableFloatStateOf(0f) }
    var velY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

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
                            var currentVy = velY
                            while (!isDragging && (kotlin.math.abs(currentVx) > 0.05f || kotlin.math.abs(currentVy) > 0.05f)) {
                                rotY += currentVx
                                rotX -= currentVy
                                currentVx *= 0.92f
                                currentVy *= 0.92f
                                delay(16)
                            }
                        }
                    },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        velX = dragAmount.x * 0.45f
                        velY = dragAmount.y * 0.45f
                        rotY += velX
                        rotX = (rotX - velY).coerceIn(-45f, 45f)
                    }
                )
            }
            .graphicsLayer {
                rotationX = rotX
                rotationY = rotY
                cameraDistance = 16f * density
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.55f + floatY)
            val islandWidth = size.width * 0.72f
            val islandDepth = islandWidth * 0.38f
            val islandHeight = islandWidth * 0.32f

            // 1. Drop shadow beneath floating island
            val shadowWidth = islandWidth * 0.95f
            val shadowHeight = islandDepth * 0.8f
            val shadowCenter = Offset(center.x, center.y + islandHeight * 0.95f)

            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.30f),
                        Color.Black.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = shadowCenter,
                    radius = shadowWidth / 2f
                ),
                topLeft = Offset(shadowCenter.x - shadowWidth / 2f, shadowCenter.y - shadowHeight / 2f),
                size = Size(shadowWidth, shadowHeight)
            )

            // 2. 3D Low-Poly Dark Terrain Bottom Body (Sculpted polygonal rocks)
            val rockPath = Path().apply {
                val leftX = center.x - islandWidth / 2f
                val rightX = center.x + islandWidth / 2f
                val topY = center.y
                val bottomY = center.y + islandHeight

                moveTo(leftX, topY)
                lineTo(leftX + islandWidth * 0.2f, bottomY * 0.92f)
                lineTo(center.x - islandWidth * 0.1f, bottomY)
                lineTo(center.x + islandWidth * 0.15f, bottomY * 0.96f)
                lineTo(rightX - islandWidth * 0.15f, bottomY * 0.88f)
                lineTo(rightX, topY)
                close()
            }

            // Draw dark clay terrain mesh
            drawPath(
                path = rockPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2C2C2E),
                        Color(0xFF1C1C1E),
                        Color(0xFF0F0F10)
                    ),
                    start = Offset(center.x, center.y),
                    end = Offset(center.x, center.y + islandHeight)
                )
            )

            // Poly facet wirelines for 3D geometric feel
            drawPath(
                path = rockPath,
                color = Color.White.copy(alpha = 0.08f),
                style = Stroke(width = 2f)
            )

            // 3. Top Water Reservoir Plateau (Blue pool surface)
            val poolTopY = center.y - islandDepth / 2f
            val poolRect = Size(islandWidth, islandDepth)

            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF007AFF),
                        Color(0xFF30B0C7),
                        Color(0xFF004080)
                    ),
                    start = Offset(center.x - islandWidth / 2f, poolTopY),
                    end = Offset(center.x + islandWidth / 2f, poolTopY + islandDepth)
                ),
                topLeft = Offset(center.x - islandWidth / 2f, poolTopY),
                size = poolRect
            )

            // Water edge stroke highlight
            drawOval(
                color = Color(0xFF64D2FF).copy(alpha = 0.6f),
                topLeft = Offset(center.x - islandWidth / 2f, poolTopY),
                size = poolRect,
                style = Stroke(width = 3f)
            )

            // 4. Falling Raindrops onto the Island Surface
            val dropCount = 12
            val rainStartTop = center.y - islandHeight * 1.8f
            val rainEndBottom = poolTopY + islandDepth * 0.5f

            for (i in 0 until dropCount) {
                val xPos = center.x + ((i * 31) % 180 - 90) * (islandWidth / 200f)
                val phase = (rainProgress + (i * 0.11f)) % 1f
                val currentY = rainStartTop + (phase * (rainEndBottom - rainStartTop))

                if (currentY < rainEndBottom) {
                    val dropLength = 24f
                    drawLine(
                        color = Color(0xFF64D2FF).copy(alpha = (0.3f + phase * 0.7f).coerceIn(0.2f, 0.95f)),
                        start = Offset(xPos, currentY),
                        end = Offset(xPos, currentY + dropLength),
                        strokeWidth = 6f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                } else {
                    // Water ripple impact ring on island
                    val rippleR = (1f - (currentY - rainEndBottom) / 20f) * 16f
                    if (rippleR > 0) {
                        drawOval(
                            color = Color(0xFF64D2FF).copy(alpha = 0.5f),
                            topLeft = Offset(xPos - rippleR, rainEndBottom - rippleR * 0.4f),
                            size = Size(rippleR * 2f, rippleR * 0.8f),
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }
        }
    }
}
