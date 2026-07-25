package com.example.util

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.floor

/**
 * Accurate-enough lunar phase math for UI (synodic month).
 * Phase fraction: 0 = new moon, 0.5 = full moon, 1 = next new.
 */
object MoonPhaseCalculator {

    private const val SYNODIC_MONTH = 29.530588853

    /** Known new moon reference (UTC): 2000-01-06 18:14 */
    private const val KNOWN_NEW_MOON_JD = 2451550.1

    data class MoonSnapshot(
        /** 0..1 through the cycle (0 = new, 0.5 = full). */
        val phaseFraction: Double,
        /** 0..1 illuminated fraction of the disc. */
        val illumination: Float,
        val phaseName: String,
        val daysToFull: Int,
        val daysToNew: Int,
        val isWaxing: Boolean
    )

    fun forDate(calendar: Calendar = Calendar.getInstance()): MoonSnapshot {
        val jd = toJulianDate(calendar)
        val daysSinceNew = jd - KNOWN_NEW_MOON_JD
        val age = ((daysSinceNew % SYNODIC_MONTH) + SYNODIC_MONTH) % SYNODIC_MONTH
        val phaseFraction = age / SYNODIC_MONTH
        // Illumination from phase angle (0° new → 180° full)
        val illum = ((1.0 - cos(phaseFraction * 2.0 * Math.PI)) / 2.0)
            .toFloat()
            .coerceIn(0f, 1f)
        val isWaxing = phaseFraction < 0.5
        val daysToFull = ((0.5 - phaseFraction + 1.0) % 1.0 * SYNODIC_MONTH).toInt()
            .coerceIn(0, 29)
        val daysToNew = ((1.0 - phaseFraction) % 1.0 * SYNODIC_MONTH).toInt()
            .coerceIn(0, 29)

        return MoonSnapshot(
            phaseFraction = phaseFraction,
            illumination = illum,
            phaseName = phaseName(phaseFraction, (illum * 100).toInt()),
            daysToFull = daysToFull,
            daysToNew = daysToNew,
            isWaxing = isWaxing
        )
    }

    fun illuminationForOffsetDays(offsetDays: Int): Float {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offsetDays) }
        return forDate(cal).illumination
    }

    fun phaseNameForIllumination(illuminationPercent: Int, isWaxing: Boolean = true): String {
        return when {
            illuminationPercent < 3 -> "NEW MOON"
            illuminationPercent < 40 -> if (isWaxing) "WAXING CRESCENT" else "WANING CRESCENT"
            illuminationPercent < 55 -> if (isWaxing) "FIRST QUARTER" else "LAST QUARTER"
            illuminationPercent < 97 -> if (isWaxing) "WAXING GIBBOUS" else "WANING GIBBOUS"
            else -> "FULL MOON"
        }
    }

    private fun phaseName(phaseFraction: Double, illumPct: Int): String {
        val isWaxing = phaseFraction < 0.5
        return when {
            phaseFraction < 0.03 || phaseFraction > 0.97 -> "NEW MOON"
            phaseFraction < 0.22 -> "WAXING CRESCENT"
            phaseFraction < 0.28 -> "FIRST QUARTER"
            phaseFraction < 0.47 -> "WAXING GIBBOUS"
            phaseFraction < 0.53 -> "FULL MOON"
            phaseFraction < 0.72 -> "WANING GIBBOUS"
            phaseFraction < 0.78 -> "LAST QUARTER"
            else -> "WANING CRESCENT"
        }.ifBlank { phaseNameForIllumination(illumPct, isWaxing) }
    }

    /** Julian Date from calendar (UTC approximation). */
    private fun toJulianDate(calendar: Calendar): Double {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = calendar.timeInMillis
        }
        var y = utc.get(Calendar.YEAR)
        var m = utc.get(Calendar.MONTH) + 1
        val d = utc.get(Calendar.DAY_OF_MONTH) +
            (utc.get(Calendar.HOUR_OF_DAY) +
                utc.get(Calendar.MINUTE) / 60.0 +
                utc.get(Calendar.SECOND) / 3600.0) / 24.0

        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) +
            floor(30.6001 * (m + 1)) +
            d + b - 1524.5
    }

    /** Approximate moonrise/set labels for display (not ephemeris-grade). */
    fun approximateRiseSetLabels(calendar: Calendar = Calendar.getInstance()): Pair<String, String> {
        val snap = forDate(calendar)
        // Rough offset: moon rises ~50 min later each day through the cycle
        val riseHour = ((6 + (snap.phaseFraction * 24).toInt()) % 24)
        val setHour = (riseHour + 12) % 24
        return formatHourLabel(riseHour) to formatHourLabel(setHour)
    }

    private fun formatHourLabel(hour24: Int): String {
        val h12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val suffix = if (hour24 < 12) "A" else "P"
        return "$h12:00$suffix"
    }
}
