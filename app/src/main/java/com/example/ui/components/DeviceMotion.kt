package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Shared phone-tilt state for hologram / parallax effects.
 * [offsetX]/[offsetY] are smoothed ~[-1, 1] values — easy to scale into pixels or degrees.
 */
@Stable
class DeviceMotionState {
    /** Left (−) / right (+) tilt, normalized roughly −1…1 */
    var offsetX by mutableFloatStateOf(0f)
        internal set

    /** Forward (−) / back (+) tilt, normalized roughly −1…1 */
    var offsetY by mutableFloatStateOf(0f)
        internal set

    var yawDeg by mutableFloatStateOf(0f)
        internal set

    var pitchDeg by mutableFloatStateOf(0f)
        internal set
}

@Composable
fun rememberDeviceMotionState(
    /** Higher = more obvious motion (1f = default background intensity). */
    intensity: Float = 1f
): DeviceMotionState {
    val state = remember { DeviceMotionState() }
    val context = LocalContext.current

    DisposableEffect(intensity) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensor == null) {
            onDispose { }
        } else {
            var fx = 0f
            var fy = 9.81f
            var fz = 0f
            val smooth = 0.14f

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    fx = smooth * x + (1f - smooth) * fx
                    fy = smooth * y + (1f - smooth) * fy
                    fz = smooth * z + (1f - smooth) * fz

                    val horizontal = sqrt(fx * fx + fy * fy).coerceAtLeast(0.01f)
                    val tiltLR = Math.toDegrees(atan2(fx.toDouble(), fy.toDouble())).toFloat()
                    val tiltFB =
                        Math.toDegrees(atan2((-fz).toDouble(), horizontal.toDouble())).toFloat()

                    // Normalize: ~±35° phone tilt → ±1
                    val nx = ((-tiltLR) / 35f).coerceIn(-1.2f, 1.2f) * intensity
                    val ny = (tiltFB / 40f).coerceIn(-1.2f, 1.2f) * intensity

                    state.offsetX = nx.coerceIn(-1.25f, 1.25f)
                    state.offsetY = ny.coerceIn(-1.25f, 1.25f)
                    state.yawDeg = (-tiltLR * 0.55f * intensity).coerceIn(-28f, 28f)
                    state.pitchDeg = (tiltFB * 0.45f * intensity).coerceIn(-22f, 22f)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            onDispose {
                sensorManager.unregisterListener(listener)
                state.offsetX = 0f
                state.offsetY = 0f
                state.yawDeg = 0f
                state.pitchDeg = 0f
            }
        }
    }

    return state
}
