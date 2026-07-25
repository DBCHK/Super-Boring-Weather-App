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
import kotlin.random.Random

/**
 * Synthesized water-droplet / drip SFX — several variants for richer UI feedback.
 */
class PianoSoundManager(private val context: Context) {

    private val audioScope = CoroutineScope(Dispatchers.Default)
    private val random = Random(System.nanoTime())

    enum class DropletKind {
        /** Classic short bloop (default taps) */
        DROP,
        /** Softer, lower drip */
        DRIP,
        /** Bright multi-bubble plink */
        PLINK,
        /** Heavier splash for important actions */
        SPLASH,
        /** Tiny tick for tab switches / scrub */
        TICK
    }

    fun playWaterDropletSound() = play(DropletKind.DROP)

    fun play(kind: DropletKind = DropletKind.DROP) {
        audioScope.launch {
            try {
                val sampleRate = 44100
                val (durationMs, startFreq, endFreq, volume, harmonics) = when (kind) {
                    DropletKind.DROP -> Params(110, 420.0, 1150.0, 12000.0, 0.22)
                    DropletKind.DRIP -> Params(160, 280.0, 720.0, 9000.0, 0.12)
                    DropletKind.PLINK -> Params(90, 620.0, 1480.0, 10000.0, 0.35)
                    DropletKind.SPLASH -> Params(200, 180.0, 900.0, 14000.0, 0.45)
                    DropletKind.TICK -> Params(55, 900.0, 1400.0, 7000.0, 0.08)
                }
                // Slight random detune so repeated taps don't sound identical
                val detune = 1.0 + (random.nextDouble() - 0.5) * 0.08
                val numSamples = (durationMs * sampleRate) / 1000
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val time = i.toDouble() / sampleRate
                    val progress = i.toDouble() / numSamples
                    val currentFreq =
                        (startFreq + (endFreq - startFreq) * Math.pow(progress, 0.5)) * detune

                    var wave = sin(2.0 * Math.PI * currentFreq * time)
                    wave += harmonics * sin(2.0 * Math.PI * (currentFreq * 2.0) * time)
                    if (kind == DropletKind.SPLASH) {
                        wave += 0.15 * sin(2.0 * Math.PI * (currentFreq * 0.5) * time)
                        // Noise burst at attack
                        if (progress < 0.12) {
                            wave += (random.nextDouble() - 0.5) * 0.35 * (1.0 - progress / 0.12)
                        }
                    }
                    if (kind == DropletKind.PLINK) {
                        wave += 0.18 * sin(2.0 * Math.PI * (currentFreq * 3.1) * time)
                    }

                    val envelope = when (kind) {
                        DropletKind.DRIP ->
                            Math.exp(-8.0 * progress) * sin(Math.PI * progress.coerceIn(0.0, 1.0))
                        DropletKind.TICK ->
                            Math.exp(-22.0 * progress)
                        DropletKind.SPLASH ->
                            Math.exp(-7.0 * progress) * (0.4 + 0.6 * sin(Math.PI * progress.coerceIn(0.0, 1.0)))
                        else ->
                            Math.exp(-12.0 * progress) * sin(Math.PI * progress.coerceIn(0.0, 1.0))
                    }
                    val sampleValue = (wave * envelope * volume).toInt().coerceIn(-32768, 32767)
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
                kotlinx.coroutines.delay((durationMs + 80).toLong())
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Random drop/drip/plink — great for ambient UI taps. */
    fun playRandomDroplet() {
        val kinds = listOf(
            DropletKind.DROP,
            DropletKind.DRIP,
            DropletKind.PLINK,
            DropletKind.DROP
        )
        play(kinds[random.nextInt(kinds.size)])
    }

    fun playSubtlePianoNote(frequency: Double = 659.25) {
        play(DropletKind.PLINK)
    }

    private data class Params(
        val durationMs: Int,
        val startFreq: Double,
        val endFreq: Double,
        val volume: Double,
        val harmonics: Double
    )
}

@Composable
fun rememberPianoFeedback(): Pair<() -> Unit, View> = rememberDropletFeedback()

/**
 * Returns multi-variant droplet feedback:
 * - first: random droplet (default taps)
 * - second: View for haptics
 * - also exposes named players via [DropletFeedback]
 */
@Composable
fun rememberDropletFeedback(): Pair<() -> Unit, View> {
    val feedback = rememberDropletPlayers()
    return Pair(feedback.tap, feedback.view)
}

data class DropletFeedback(
    val view: View,
    val tap: () -> Unit,
    val drop: () -> Unit,
    val drip: () -> Unit,
    val plink: () -> Unit,
    val splash: () -> Unit,
    val tick: () -> Unit,
    val random: () -> Unit
)

@Composable
fun rememberDropletPlayers(): DropletFeedback {
    val view = LocalView.current
    val context = view.context.applicationContext
    val soundManager = remember { PianoSoundManager(context) }

    return remember {
        fun haptic() {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        DropletFeedback(
            view = view,
            tap = {
                haptic()
                soundManager.playRandomDroplet()
            },
            drop = {
                haptic()
                soundManager.play(PianoSoundManager.DropletKind.DROP)
            },
            drip = {
                haptic()
                soundManager.play(PianoSoundManager.DropletKind.DRIP)
            },
            plink = {
                haptic()
                soundManager.play(PianoSoundManager.DropletKind.PLINK)
            },
            splash = {
                haptic()
                soundManager.play(PianoSoundManager.DropletKind.SPLASH)
            },
            tick = {
                haptic()
                soundManager.play(PianoSoundManager.DropletKind.TICK)
            },
            random = {
                haptic()
                soundManager.playRandomDroplet()
            }
        )
    }
}
