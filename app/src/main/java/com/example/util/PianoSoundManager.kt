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
 * Generates dynamic water droplet touch sound effects dynamically using synthesized audio.
 * Pure native Android AudioTrack implementation — zero external media assets required!
 */
class PianoSoundManager(private val context: Context) {

    private val audioScope = CoroutineScope(Dispatchers.Default)

    /**
     * Plays a pleasant, low-latency water droplet ("bloop") sound effect with ascending pitch sweep.
     */
    fun playWaterDropletSound() {
        audioScope.launch {
            try {
                val sampleRate = 44100
                val durationMs = 120
                val numSamples = (durationMs * sampleRate) / 1000
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val time = i.toDouble() / sampleRate
                    val progress = i.toDouble() / numSamples
                    
                    // Pitch sweeps rapidly upward from 420Hz to 1150Hz for realistic droplet bubble popping resonance
                    val startFreq = 420.0
                    val endFreq = 1150.0
                    val currentFreq = startFreq + (endFreq - startFreq) * Math.pow(progress, 0.5)

                    // Sine wave at sweeping frequency
                    val wave = sin(2.0 * Math.PI * currentFreq * time) + 
                            0.2 * sin(2.0 * Math.PI * (currentFreq * 2.0) * time)
                    
                    // Exponential attack and decay envelope
                    val envelope = Math.exp(-12.0 * progress) * sin(Math.PI * progress.coerceIn(0.0, 1.0))
                    val sampleValue = (wave * envelope * 12000.0).toInt().coerceIn(-32768, 32767)
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
                kotlinx.coroutines.delay(200)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playSubtlePianoNote(frequency: Double = 659.25) {
        playWaterDropletSound()
    }
}

@Composable
fun rememberPianoFeedback(): Pair<() -> Unit, View> {
    return rememberDropletFeedback()
}

@Composable
fun rememberDropletFeedback(): Pair<() -> Unit, View> {
    val view = LocalView.current
    val context = view.context.applicationContext
    val soundManager = remember { PianoSoundManager(context) }

    val onClickWithFeedback = remember {
        {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            soundManager.playWaterDropletSound()
        }
    }
    return Pair(onClickWithFeedback, view)
}

