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
import androidx.compose.ui.graphics.TransformOrigin
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

    fun clampPitch(value: Float): Float = value.coerceIn(-maxPitch, maxPitch)

    /** Pitch used for rendering: drag + device tilt, hard-clamped so models never flip fully. */
    val renderPitch: Float
        get() = clampPitch(pitch + devicePitchOffset)

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
 *  - tracks drag / momentum / auto-spin / device tilt on [state]
 *  - soft press scale bounce on touch
 *  - optional [applyLayerRotation]: applies pitch/yaw as a single Compose 2.5D transform
 *    so children (e.g. weather on top, digits at bottom) move like ends of a rigid stick
 *    around [layerTransformOrigin]
 */
fun Modifier.interactive3D(
    state: Interactive3DState,
    enablePitch: Boolean = true,
    playSound: Boolean = true,
    enableDeviceTilt: Boolean = true,
    /**
     * When true, pitch/yaw drive this layer's rotationX/Y (shared pivot for linked children).
     * Child models should then disable their own interaction rotation to avoid double-spin.
     */
    applyLayerRotation: Boolean = false,
    /** Pivot for stick motion — typically mid-column between weather (top) and digits (bottom). */
    layerTransformOrigin: TransformOrigin = TransformOrigin.Center
): Modifier = composed {
    val view = LocalView.current
    val context = view.context.applicationContext
    val soundManager = remember { PianoSoundManager(context) }
    val scope = rememberCoroutineScope()
    val sensorContext = LocalContext.current

    // Gentle hologram tilt — warm-up + heavy smoothing prevents idle "snap" jumps
    if (enableDeviceTilt) {
        DisposableEffect(state, enablePitch) {
            val sensorManager =
                sensorContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
                ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            if (sensor == null) {
                onDispose { }
            } else {
                var fx = 0f
                var fy = 9.81f
                var fz = 0f
                var seeded = false
                var warmFrames = 0
                // Heavier smoothing = less idle noise / sudden shifts
                val smooth = 0.06f
                val outputSmooth = 0.12f
                val maxDeviceYaw = 18f
                // Device pitch stays within overall maxPitch so combined tilt can't go upside-down
                val maxDevicePitch = if (enablePitch) {
                    (state.maxPitch * 0.55f).coerceIn(10f, 28f)
                } else {
                    8f
                }
                var outYaw = 0f
                var outPitch = 0f

                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]
                        if (!seeded) {
                            fx = x; fy = y; fz = z
                            seeded = true
                            return
                        }
                        fx = smooth * x + (1f - smooth) * fx
                        fy = smooth * y + (1f - smooth) * fy
                        fz = smooth * z + (1f - smooth) * fz

                        val horizontal = sqrt(fx * fx + fy * fy).coerceAtLeast(0.01f)
                        val tiltLR = Math.toDegrees(atan2(fx.toDouble(), fy.toDouble())).toFloat()
                        val tiltFB =
                            Math.toDegrees(atan2((-fz).toDouble(), horizontal.toDouble())).toFloat()

                        val targetYaw = (-tiltLR * 0.48f).coerceIn(-maxDeviceYaw, maxDeviceYaw)
                        val targetPitch = (tiltFB * 0.45f).coerceIn(-maxDevicePitch, maxDevicePitch)

                        // Warm-up: ignore first ~12 samples so first real reading doesn't snap
                        warmFrames++
                        if (warmFrames < 12) {
                            outYaw = targetYaw
                            outPitch = targetPitch
                            return
                        }

                        outYaw += (targetYaw - outYaw) * outputSmooth
                        outPitch += (targetPitch - outPitch) * outputSmooth
                        state.deviceYawOffset = outYaw
                        // Keep device pitch inside absolute pitch budget
                        state.devicePitchOffset = outPitch.coerceIn(-state.maxPitch, state.maxPitch)
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }

                sensorManager.registerListener(
                    listener,
                    sensor,
                    SensorManager.SENSOR_DELAY_UI // calmer than GAME — less idle jitter
                )
                onDispose {
                    sensorManager.unregisterListener(listener)
                    // Don't hard-zero (causes visible jump); leave last value
                }
            }
        }
    }

    // Continuous auto-spin + residual inertia when not dragging
    LaunchedEffect(state.isDragging) {
        if (state.isDragging) return@LaunchedEffect

        var lastFrame = 0L
        var velocityYaw = state.velYaw * 55f
        var velocityPitch = state.velPitch * 55f
        // Longer ease-in so idle doesn't "kick" after ~1s
        var autoSpinBlend = 0f
        // Continuous phase for smooth oscillation (no hard reverse at maxYaw)
        var oscillatePhase = 0f

        while (true) {
            withFrameMillis { frameTime ->
                if (lastFrame == 0L) {
                    lastFrame = frameTime
                    return@withFrameMillis
                }
                val dt = ((frameTime - lastFrame) / 1000f).coerceIn(0.001f, 0.033f)
                lastFrame = frameTime

                val friction = exp(-3.8f * dt)
                velocityYaw *= friction
                velocityPitch *= friction

                // ~1.4s ease-in — avoids sudden mid-idle spin kick
                autoSpinBlend = (autoSpinBlend + dt / 1.4f).coerceIn(0f, 1f)
                val easedAuto = FastOutSlowInEasing.transform(autoSpinBlend)

                val yawLimit = state.maxYaw
                if (state.autoSpinOscillate && yawLimit != null && state.autoSpinDegPerSec != 0f) {
                    // Smooth sine oscillation between ±maxYaw (no hard bounce snap)
                    val phaseSpeed =
                        (state.autoSpinDegPerSec / yawLimit.coerceAtLeast(1f)) * 0.55f
                    oscillatePhase += phaseSpeed * dt * easedAuto
                    val target = kotlin.math.sin(oscillatePhase) * yawLimit
                    // Blend free yaw (from drag residual) toward sine target
                    val momentumMag = abs(velocityYaw)
                    if (momentumMag < 8f) {
                        state.yaw += (target - state.yaw) * (2.2f * dt).coerceIn(0f, 1f)
                    } else {
                        state.yaw = state.clampYaw(state.yaw + velocityYaw * dt)
                    }
                } else {
                    val autoSpinBase = state.autoSpinDegPerSec * easedAuto
                    val momentumMag = abs(velocityYaw)
                    val autoContribution = if (momentumMag > 35f) {
                        autoSpinBase * (1f - ((momentumMag - 35f) / 100f).coerceIn(0f, 1f))
                    } else {
                        autoSpinBase
                    }
                    var nextYaw = state.yaw + (velocityYaw + autoContribution) * dt
                    if (yawLimit != null) {
                        nextYaw = nextYaw.coerceIn(-yawLimit, yawLimit)
                        if (nextYaw == yawLimit || nextYaw == -yawLimit) velocityYaw = 0f
                    }
                    state.yaw = nextYaw
                }

                if (enablePitch) {
                    state.pitch = (state.pitch - velocityPitch * dt)
                        .coerceIn(-state.maxPitch, state.maxPitch)
                }
            }
        }
    }

    // Read state in composition so press-scale / stick rotation recompose
    val pressScale = state.pressScale.value
    // Subscribe pitch/yaw when drawing as a rigid stick layer
    val stickPitch = if (applyLayerRotation) state.renderPitch else 0f
    val stickYaw = if (applyLayerRotation) state.renderYaw else 0f

    this
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
            // Stick kinematics: one pivot for the whole hero column.
            // Pitch tips top (weather) and bottom (digits) in opposite screen arcs;
            // yaw swings the stick left/right as a single rigid body.
            if (applyLayerRotation) {
                rotationX = stickPitch
                rotationY = stickYaw
                transformOrigin = layerTransformOrigin
                // Deeper camera so the stick foreshortens instead of flattening
                cameraDistance = 22f * density
            } else {
                cameraDistance = 14f * density
            }
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
                        soundManager.playRandomDroplet()
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
                        soundManager.play(PianoSoundManager.DropletKind.DRIP)
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
