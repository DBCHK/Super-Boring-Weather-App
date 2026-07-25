package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.example.util.PianoSoundManager
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Shared interactive 3D rotation state used by all weather 3D canvases.
 * Provides fluid drag, momentum ease-out, gentle auto-spin, and device-tilt hologram offsets.
 *
 * @param maxYaw when set, yaw is clamped so objects (e.g. digits) never flip reversed.
 * @param autoSpinOscillate when true with [maxYaw], auto-spin ping-pongs left/right instead of full 360°.
 */
@Stable
class Interactive3DState(
    initialPitch: Float = 12f,
    initialYaw: Float = 0f,
    val maxPitch: Float = 70f,
    val maxYaw: Float? = null,
    val autoSpinDegPerSec: Float = 18f,
    val autoSpinOscillate: Boolean = false
) {
    var pitch by mutableFloatStateOf(initialPitch)
        internal set
    var yaw by mutableFloatStateOf(initialYaw)
        internal set

    /** Soft device-tilt offsets for hologram parallax (degrees). */
    var devicePitchOffset by mutableFloatStateOf(0f)
        internal set
    var deviceYawOffset by mutableFloatStateOf(0f)
        internal set

    val pressScale = Animatable(1f)

    var isDragging by mutableStateOf(false)
        internal set

    /** Latest drag deltas used as residual velocity after release. */
    var velYaw by mutableFloatStateOf(0f)
        internal set
    var velPitch by mutableFloatStateOf(0f)
        internal set

    /** Direction for oscillating auto-spin (+1 or -1). */
    var autoSpinSign by mutableFloatStateOf(1f)
        internal set

    fun clampYaw(value: Float): Float {
        val limit = maxYaw ?: return value
        return value.coerceIn(-limit, limit)
    }

    /** Pitch used for rendering: drag/auto + gentle device tilt. */
    val renderPitch: Float
        get() = pitch.coerceIn(-maxPitch, maxPitch) + devicePitchOffset

    /** Yaw used for rendering: drag/auto + gentle device tilt (clamped when maxYaw set). */
    val renderYaw: Float
        get() = clampYaw(yaw + deviceYawOffset)
}

@Composable
fun rememberInteractive3DState(
    initialPitch: Float = 12f,
    initialYaw: Float = 0f,
    maxPitch: Float = 70f,
    maxYaw: Float? = null,
    autoSpinDegPerSec: Float = 18f,
    autoSpinOscillate: Boolean = false
): Interactive3DState {
    return remember {
        Interactive3DState(
            initialPitch = initialPitch,
            initialYaw = initialYaw,
            maxPitch = maxPitch,
            maxYaw = maxYaw,
            autoSpinDegPerSec = autoSpinDegPerSec,
            autoSpinOscillate = autoSpinOscillate
        )
    }
}

/**
 * Modifier that:
 *  - rotates the composable on drag (fluid, 1:1 feel)
 *  - eases out momentum after release
 *  - eases in to a gentle continuous auto-spin
 *  - soft press scale bounce on touch
 *  - subtle device-tilt hologram parallax (not aggressive)
 */
fun Modifier.interactive3D(
    state: Interactive3DState,
    enablePitch: Boolean = true,
    playSound: Boolean = true,
    enableDeviceTilt: Boolean = true
): Modifier = composed {
    val view = LocalView.current
    val context = view.context.applicationContext
    val soundManager = remember { PianoSoundManager(context) }
    val scope = rememberCoroutineScope()
    val sensorContext = LocalContext.current

    // Gentle hologram tilt from phone motion
    if (enableDeviceTilt) {
        DisposableEffect(state, enablePitch) {
            val sensorManager =
                sensorContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
                ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            if (sensor == null) {
                onDispose { }
            } else {
                // Soft low-pass + small angle caps so motion feels floaty, not twitchy
                var fx = 0f
                var fy = 9.81f
                var fz = 0f
                val smooth = 0.10f
                // More readable hologram range — still smooth, clearly visible on tilt
                val maxDeviceYaw = 22f
                val maxDevicePitch = if (enablePitch) 16f else 10f

                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]
                        fx = smooth * x + (1f - smooth) * fx
                        fy = smooth * y + (1f - smooth) * fy
                        fz = smooth * z + (1f - smooth) * fz

                        val horizontal = sqrt(fx * fx + fy * fy).coerceAtLeast(0.01f)
                        // Left/right phone tilt → yaw
                        val tiltLR = Math.toDegrees(atan2(fx.toDouble(), fy.toDouble())).toFloat()
                        // Forward/back phone tilt → pitch
                        val tiltFB =
                            Math.toDegrees(atan2((-fz).toDouble(), horizontal.toDouble())).toFloat()

                        state.deviceYawOffset =
                            (-tiltLR * 0.62f).coerceIn(-maxDeviceYaw, maxDeviceYaw)
                        state.devicePitchOffset =
                            (tiltFB * 0.50f).coerceIn(-maxDevicePitch, maxDevicePitch)
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }

                sensorManager.registerListener(
                    listener,
                    sensor,
                    SensorManager.SENSOR_DELAY_GAME
                )
                onDispose {
                    sensorManager.unregisterListener(listener)
                    state.devicePitchOffset = 0f
                    state.deviceYawOffset = 0f
                }
            }
        }
    }

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
                val autoSpinBase = state.autoSpinDegPerSec * easedAuto *
                    if (state.autoSpinOscillate) state.autoSpinSign else 1f

                // Soften auto-spin while residual momentum is still high
                val momentumMag = abs(velocityYaw)
                val autoContribution = if (momentumMag > 35f) {
                    autoSpinBase * (1f - ((momentumMag - 35f) / 100f).coerceIn(0f, 1f))
                } else {
                    autoSpinBase
                }

                var nextYaw = state.yaw + (velocityYaw + autoContribution) * dt
                val yawLimit = state.maxYaw
                if (yawLimit != null) {
                    if (nextYaw > yawLimit) {
                        nextYaw = yawLimit
                        velocityYaw = 0f
                        if (state.autoSpinOscillate) state.autoSpinSign = -1f
                    } else if (nextYaw < -yawLimit) {
                        nextYaw = -yawLimit
                        velocityYaw = 0f
                        if (state.autoSpinOscillate) state.autoSpinSign = 1f
                    }
                }
                state.yaw = nextYaw

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
                    // Horizontal drag → yaw (X / left-right). Vertical → pitch only if enabled.
                    val yawDelta = dragAmount.x * 0.35f
                    val pitchDelta = if (enablePitch) dragAmount.y * 0.28f else 0f
                    state.velYaw = yawDelta
                    state.velPitch = pitchDelta
                    state.yaw = state.clampYaw(state.yaw + yawDelta)
                    if (enablePitch) {
                        state.pitch = (state.pitch - pitchDelta)
                            .coerceIn(-state.maxPitch, state.maxPitch)
                    }
                }
            )
        }
}
