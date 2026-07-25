package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import com.example.util.PianoSoundManager
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp

/**
 * Shared interactive 3D rotation state used by all weather 3D canvases.
 * Provides fluid drag, momentum ease-out, and a gentle ease-in back to auto-spin.
 */
@Stable
class Interactive3DState(
    initialPitch: Float = 12f,
    initialYaw: Float = 0f,
    val maxPitch: Float = 70f,
    val autoSpinDegPerSec: Float = 18f
) {
    var pitch by mutableFloatStateOf(initialPitch)
        internal set
    var yaw by mutableFloatStateOf(initialYaw)
        internal set

    val pressScale = Animatable(1f)

    var isDragging by mutableStateOf(false)
        internal set

    /** Latest drag deltas used as residual velocity after release. */
    var velYaw by mutableFloatStateOf(0f)
        internal set
    var velPitch by mutableFloatStateOf(0f)
        internal set
}

@Composable
fun rememberInteractive3DState(
    initialPitch: Float = 12f,
    initialYaw: Float = 0f,
    maxPitch: Float = 70f,
    autoSpinDegPerSec: Float = 18f
): Interactive3DState {
    return remember {
        Interactive3DState(
            initialPitch = initialPitch,
            initialYaw = initialYaw,
            maxPitch = maxPitch,
            autoSpinDegPerSec = autoSpinDegPerSec
        )
    }
}

/**
 * Modifier that:
 *  - rotates the composable on drag (fluid, 1:1 feel)
 *  - eases out momentum after release
 *  - eases in to a gentle continuous auto-spin
 *  - soft press scale bounce on touch
 */
fun Modifier.interactive3D(
    state: Interactive3DState,
    enablePitch: Boolean = true,
    playSound: Boolean = true
): Modifier = composed {
    val view = LocalView.current
    val context = view.context.applicationContext
    val soundManager = remember { PianoSoundManager(context) }
    val scope = rememberCoroutineScope()

    // Continuous auto-spin + residual inertia when not dragging
    LaunchedEffect(state.isDragging) {
        if (state.isDragging) return@LaunchedEffect

        var lastFrame = 0L
        // Convert approx per-frame deltas → deg/sec for frame-rate independence
        var velocityYaw = state.velYaw * 55f
        var velocityPitch = state.velPitch * 55f
        var autoSpinBlend = 0f // 0..1 ease-in of auto-spin after release

        while (true) {
            withFrameMillis { frameTime ->
                if (lastFrame == 0L) {
                    lastFrame = frameTime
                    return@withFrameMillis
                }
                val dt = ((frameTime - lastFrame) / 1000f).coerceIn(0.001f, 0.05f)
                lastFrame = frameTime

                // Exponential friction (ease-out momentum)
                val friction = exp(-3.4f * dt)
                velocityYaw *= friction
                velocityPitch *= friction

                // Ease-in auto-spin over ~650ms after release
                autoSpinBlend = (autoSpinBlend + dt / 0.65f).coerceIn(0f, 1f)
                val easedAuto = FastOutSlowInEasing.transform(autoSpinBlend)
                val autoSpin = state.autoSpinDegPerSec * easedAuto

                // Soften auto-spin while residual momentum is still high
                val momentumMag = abs(velocityYaw)
                val autoContribution = if (momentumMag > 35f) {
                    autoSpin * (1f - ((momentumMag - 35f) / 100f).coerceIn(0f, 1f))
                } else {
                    autoSpin
                }

                state.yaw += (velocityYaw + autoContribution) * dt
                if (enablePitch) {
                    state.pitch = (state.pitch - velocityPitch * dt)
                        .coerceIn(-state.maxPitch, state.maxPitch)
                }
            }
        }
    }

    // Read Animatable in composition so press-scale changes recompose
    val pressScale = state.pressScale.value

    this
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
            cameraDistance = 14f * density
        }
        .pointerInput(state, enablePitch) {
            detectTapGestures(
                onPress = {
                    scope.launch {
                        state.pressScale.animateTo(
                            0.94f,
                            animationSpec = tween(110, easing = FastOutSlowInEasing)
                        )
                    }
                    if (playSound) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        soundManager.playWaterDropletSound()
                    }
                    tryAwaitRelease()
                    // Ease-in spring bounce on release
                    scope.launch {
                        state.pressScale.animateTo(
                            1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            )
        }
        .pointerInput(state, enablePitch) {
            detectDragGestures(
                onDragStart = {
                    state.isDragging = true
                    state.velYaw = 0f
                    state.velPitch = 0f
                    scope.launch {
                        state.pressScale.animateTo(
                            0.96f,
                            animationSpec = tween(90, easing = FastOutSlowInEasing)
                        )
                    }
                    if (playSound) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        soundManager.playWaterDropletSound()
                    }
                },
                onDragEnd = {
                    state.isDragging = false
                    scope.launch {
                        state.pressScale.animateTo(
                            1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                },
                onDragCancel = {
                    state.isDragging = false
                    scope.launch {
                        state.pressScale.animateTo(
                            1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val yawDelta = dragAmount.x * 0.35f
                    val pitchDelta = dragAmount.y * 0.28f
                    state.velYaw = yawDelta
                    state.velPitch = pitchDelta
                    state.yaw += yawDelta
                    if (enablePitch) {
                        state.pitch = (state.pitch - pitchDelta)
                            .coerceIn(-state.maxPitch, state.maxPitch)
                    }
                }
            )
        }
}
