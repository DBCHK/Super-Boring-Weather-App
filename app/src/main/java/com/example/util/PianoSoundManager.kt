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
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Synthesized UI SFX — water droplets, soft piano-ish plinks, swooshes, and chimes
 * inspired by playful weather-app feedback (Not Boring–style).
 */
class PianoSoundManager(private val context: Context) {

    private val audioScope = CoroutineScope(Dispatchers.Default)
    private val random = Random(System.nanoTime())

    enum class SfxKind {
        DROP,
        DRIP,
        PLINK,
        SPLASH,
        TICK,
        /** Soft UI whoosh for sheet open / theme change */
        SWOOSH,
        /** Upward whoosh — swipe up / open details */
        WHOOSH_UP,
        /** Downward whoosh — dismiss sheet */
        WHOOSH_DOWN,
        /** Soft pop for toggles */
        POP,
        /** Success / location lock chime */
        CHIME,
        /** Bubble cluster for weather interactions */
        BUBBLE,
        /** Crisp snap for unit / chip select */
        SNAP
    }

    fun playWaterDropletSound() = play(SfxKind.DROP)

    fun play(kind: SfxKind = SfxKind.DROP) {
        audioScope.launch {
            try {
                val sampleRate = 44100
                val samples = when (kind) {
                    SfxKind.DROP -> synthDroplet(sampleRate, 110, 420.0, 1150.0, 12000.0, 0.22, splash = false)
                    SfxKind.DRIP -> synthDroplet(sampleRate, 160, 280.0, 720.0, 9000.0, 0.12, splash = false, dripEnv = true)
                    SfxKind.PLINK -> synthDroplet(sampleRate, 90, 620.0, 1480.0, 10000.0, 0.35, plink = true)
                    SfxKind.SPLASH -> synthDroplet(sampleRate, 200, 180.0, 900.0, 14000.0, 0.45, splash = true)
                    SfxKind.TICK -> synthDroplet(sampleRate, 50, 980.0, 1500.0, 6500.0, 0.06, tickEnv = true)
                    SfxKind.SWOOSH -> synthNoiseSweep(sampleRate, 180, 900.0, 220.0, 9000.0, up = false)
                    SfxKind.WHOOSH_UP -> synthNoiseSweep(sampleRate, 160, 180.0, 1100.0, 8500.0, up = true)
                    SfxKind.WHOOSH_DOWN -> synthNoiseSweep(sampleRate, 170, 900.0, 160.0, 8500.0, up = false)
                    SfxKind.POP -> synthPop(sampleRate)
                    SfxKind.CHIME -> synthChime(sampleRate)
                    SfxKind.BUBBLE -> synthBubble(sampleRate)
                    SfxKind.SNAP -> synthSnap(sampleRate)
                }

                val durationMs = (samples.size * 1000L) / sampleRate
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
                kotlinx.coroutines.delay(durationMs + 60)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playRandomDroplet() {
        val kinds = listOf(SfxKind.DROP, SfxKind.DRIP, SfxKind.PLINK, SfxKind.BUBBLE, SfxKind.DROP)
        play(kinds[random.nextInt(kinds.size)])
    }

    fun playSubtlePianoNote(frequency: Double = 659.25) {
        play(SfxKind.PLINK)
    }

    /** Scrub tick — tiny pitch steps so dragging the timeline feels musical. */
    fun playScrubTick(step: Int) {
        audioScope.launch {
            try {
                val sampleRate = 44100
                val base = 720.0 + (step % 12) * 38.0
                val samples = synthDroplet(
                    sampleRate, 40, base, base * 1.35, 5500.0, 0.05, tickEnv = true
                )
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
                kotlinx.coroutines.delay(70)
                audioTrack.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun synthDroplet(
        sampleRate: Int,
        durationMs: Int,
        startFreq: Double,
        endFreq: Double,
        volume: Double,
        harmonics: Double,
        splash: Boolean = false,
        plink: Boolean = false,
        dripEnv: Boolean = false,
        tickEnv: Boolean = false
    ): ShortArray {
        val detune = 1.0 + (random.nextDouble() - 0.5) * 0.08
        val numSamples = (durationMs * sampleRate) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val time = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val currentFreq =
                (startFreq + (endFreq - startFreq) * progress.pow(0.5)) * detune
            var wave = sin(2.0 * PI * currentFreq * time)
            wave += harmonics * sin(2.0 * PI * (currentFreq * 2.0) * time)
            if (splash) {
                wave += 0.15 * sin(2.0 * PI * (currentFreq * 0.5) * time)
                if (progress < 0.12) {
                    wave += (random.nextDouble() - 0.5) * 0.35 * (1.0 - progress / 0.12)
                }
            }
            if (plink) {
                wave += 0.18 * sin(2.0 * PI * (currentFreq * 3.1) * time)
            }
            val envelope = when {
                dripEnv -> exp(-8.0 * progress) * sin(PI * progress.coerceIn(0.0, 1.0))
                tickEnv -> exp(-24.0 * progress)
                splash -> exp(-7.0 * progress) * (0.4 + 0.6 * sin(PI * progress.coerceIn(0.0, 1.0)))
                else -> exp(-12.0 * progress) * sin(PI * progress.coerceIn(0.0, 1.0))
            }
            samples[i] = (wave * envelope * volume).toInt().coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    private fun synthNoiseSweep(
        sampleRate: Int,
        durationMs: Int,
        f0: Double,
        f1: Double,
        volume: Double,
        up: Boolean
    ): ShortArray {
        val n = (durationMs * sampleRate) / 1000
        val samples = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val p = i.toDouble() / n
            val freq = f0 + (f1 - f0) * if (up) p.pow(0.7) else p.pow(1.2)
            phase += 2.0 * PI * freq / sampleRate
            val tone = sin(phase) * 0.55
            val noise = (random.nextDouble() - 0.5) * 0.9
            val env = sin(PI * p.coerceIn(0.0, 1.0)).pow(0.85) * exp(-1.8 * p)
            val band = if (up) (0.4 + 0.6 * p) else (1.0 - 0.55 * p)
            val wave = (tone * 0.65 + noise * 0.35 * band) * env * volume
            samples[i] = wave.toInt().coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    private fun synthPop(sampleRate: Int): ShortArray {
        val n = (sampleRate * 0.09).toInt()
        val samples = ShortArray(n)
        val f = 280.0 + random.nextDouble() * 40.0
        for (i in 0 until n) {
            val p = i.toDouble() / n
            val t = i.toDouble() / sampleRate
            val wave = sin(2.0 * PI * f * (1.0 + 2.2 * (1.0 - p)) * t) +
                0.3 * sin(2.0 * PI * f * 2.4 * t)
            val env = exp(-18.0 * p)
            samples[i] = (wave * env * 11000.0).toInt().coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    private fun synthChime(sampleRate: Int): ShortArray {
        // Two partials like a soft notification bell
        val n = (sampleRate * 0.32).toInt()
        val samples = ShortArray(n)
        val f1 = 784.0 // G5
        val f2 = 1174.7 // D6
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val p = i.toDouble() / n
            val wave =
                0.55 * sin(2.0 * PI * f1 * t) * exp(-4.5 * p) +
                    0.35 * sin(2.0 * PI * f2 * t) * exp(-6.0 * p) +
                    0.12 * sin(2.0 * PI * f1 * 2.01 * t) * exp(-8.0 * p)
            samples[i] = (wave * 12000.0).toInt().coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    private fun synthBubble(sampleRate: Int): ShortArray {
        val n = (sampleRate * 0.16).toInt()
        val samples = ShortArray(n)
        val fA = 520.0 + random.nextDouble() * 80
        val fB = 780.0 + random.nextDouble() * 90
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val p = i.toDouble() / n
            val a = sin(2.0 * PI * fA * t) * exp(-14.0 * p)
            val b = if (p > 0.25) {
                sin(2.0 * PI * fB * (t - 0.04)) * exp(-16.0 * (p - 0.25))
            } else 0.0
            samples[i] = ((a * 0.7 + b * 0.5) * 10000.0).toInt().coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    private fun synthSnap(sampleRate: Int): ShortArray {
        val n = (sampleRate * 0.045).toInt()
        val samples = ShortArray(n)
        for (i in 0 until n) {
            val p = i.toDouble() / n
            val noise = (random.nextDouble() - 0.5) * 2.0
            val click = sin(2.0 * PI * 1600.0 * i / sampleRate) * exp(-40.0 * p)
            val env = exp(-30.0 * p)
            samples[i] = ((noise * 0.55 + click * 0.8) * env * 9000.0)
                .toInt().coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    // Backward-compatible alias
    enum class DropletKind {
        DROP, DRIP, PLINK, SPLASH, TICK
    }

    fun play(kind: DropletKind) {
        play(
            when (kind) {
                DropletKind.DROP -> SfxKind.DROP
                DropletKind.DRIP -> SfxKind.DRIP
                DropletKind.PLINK -> SfxKind.PLINK
                DropletKind.SPLASH -> SfxKind.SPLASH
                DropletKind.TICK -> SfxKind.TICK
            }
        )
    }
}

@Composable
fun rememberPianoFeedback(): Pair<() -> Unit, View> = rememberDropletFeedback()

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
    val random: () -> Unit,
    val swoosh: () -> Unit,
    val whooshUp: () -> Unit,
    val whooshDown: () -> Unit,
    val pop: () -> Unit,
    val chime: () -> Unit,
    val bubble: () -> Unit,
    val snap: () -> Unit,
    val scrubTick: (Int) -> Unit
)

@Composable
fun rememberDropletPlayers(): DropletFeedback {
    val view = LocalView.current
    val context = view.context.applicationContext
    val soundManager = remember { PianoSoundManager(context) }

    return remember {
        fun haptic(type: Int = HapticFeedbackConstants.KEYBOARD_TAP) {
            view.performHapticFeedback(type)
        }
        DropletFeedback(
            view = view,
            tap = {
                haptic()
                soundManager.playRandomDroplet()
            },
            drop = {
                haptic()
                soundManager.play(PianoSoundManager.SfxKind.DROP)
            },
            drip = {
                haptic()
                soundManager.play(PianoSoundManager.SfxKind.DRIP)
            },
            plink = {
                haptic()
                soundManager.play(PianoSoundManager.SfxKind.PLINK)
            },
            splash = {
                haptic(HapticFeedbackConstants.LONG_PRESS)
                soundManager.play(PianoSoundManager.SfxKind.SPLASH)
            },
            tick = {
                haptic(HapticFeedbackConstants.CLOCK_TICK)
                soundManager.play(PianoSoundManager.SfxKind.TICK)
            },
            random = {
                haptic()
                soundManager.playRandomDroplet()
            },
            swoosh = {
                haptic()
                soundManager.play(PianoSoundManager.SfxKind.SWOOSH)
            },
            whooshUp = {
                haptic(HapticFeedbackConstants.LONG_PRESS)
                soundManager.play(PianoSoundManager.SfxKind.WHOOSH_UP)
            },
            whooshDown = {
                haptic()
                soundManager.play(PianoSoundManager.SfxKind.WHOOSH_DOWN)
            },
            pop = {
                haptic()
                soundManager.play(PianoSoundManager.SfxKind.POP)
            },
            chime = {
                haptic(HapticFeedbackConstants.CONFIRM)
                soundManager.play(PianoSoundManager.SfxKind.CHIME)
            },
            bubble = {
                haptic()
                soundManager.play(PianoSoundManager.SfxKind.BUBBLE)
            },
            snap = {
                haptic(HapticFeedbackConstants.CLOCK_TICK)
                soundManager.play(PianoSoundManager.SfxKind.SNAP)
            },
            scrubTick = { step ->
                haptic(HapticFeedbackConstants.CLOCK_TICK)
                soundManager.playScrubTick(step)
            }
        )
    }
}
