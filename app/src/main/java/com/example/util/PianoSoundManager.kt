package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Generates subtle, soothing piano note sound effects dynamically using synthesized audio.
 * Pure native Android AudioTrack implementation — zero external media assets required!
 */
class PianoSoundManager(private val context: Context) {

    private val audioScope = CoroutineScope(Dispatchers.Default)

    /**
     * Plays a pleasant, low-latency, warm piano key note (E5 = 659.25 Hz).
     */
    fun playSubtlePianoNote(frequency: Double = 659.25) {
        audioScope.launch {
            try {
                val sampleRate = 44100
                val durationMs = 180
                val numSamples = (durationMs * sampleRate) / 1000
                val samples = ShortArray(numSamples)

                // Synthesize warm acoustic piano tone with gentle exponential decay envelope
                for (i in 0 until numSamples) {
                    val time = i.toDouble() / sampleRate
                    // Fundamental + subtle 2nd & 3rd harmonics for rich piano timbre
                    val wave = sin(2.0 * Math.PI * frequency * time) +
                            0.35 * sin(2.0 * Math.PI * (frequency * 2.0) * time) +
                            0.15 * sin(2.0 * Math.PI * (frequency * 3.0) * time)

                    // Exponential decay curve
                    val decay = Math.exp(-12.0 * time)
                    val sampleValue = (wave * decay * 8000.0).toInt().coerceIn(-32768, 32767)
                    samples[i] = sampleValue.toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()

                // Release track after playback finishes
                kotlinx.coroutines.delay(250)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun rememberPianoFeedback(): Pair<() -> Unit, View> {
    val view = LocalView.current
    val context = view.context.applicationContext
    val soundManager = remember { PianoSoundManager(context) }

    val onClickWithFeedback = remember {
        {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            soundManager.playSubtlePianoNote()
        }
    }
    return Pair(onClickWithFeedback, view)
}
