package com.example.widget

import android.content.Context
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherForecastData
import com.example.util.MoonPhaseCalculator
import kotlin.math.roundToInt

/**
 * Lightweight weather snapshot persisted for home-screen widgets.
 */
data class WidgetSnapshot(
    val cityName: String = "—",
    val tempC: Int = 0,
    val tempF: Int = 0,
    val highC: Int = 0,
    val highF: Int = 0,
    val lowC: Int = 0,
    val lowF: Int = 0,
    val conditionLabel: String = "CLEAR",
    val conditionKey: String = WeatherCondition.CLEAR.name,
    val aqi: Int = 30,
    val aqiLabel: String = "GOOD",
    val moonPhase: String = "WANING CRESCENT",
    val moonIllum: Int = 25,
    val day0Name: String = "TODAY",
    val day0High: Int = 0,
    val day0Low: Int = 0,
    val day0Icon: String = "sun",
    val day1Name: String = "—",
    val day1High: Int = 0,
    val day1Low: Int = 0,
    val day1Icon: String = "cloud",
    val day2Name: String = "—",
    val day2High: Int = 0,
    val day2Low: Int = 0,
    val day2Icon: String = "cloud",
    val updatedAtMs: Long = 0L
) {
    fun displayTemp(useCelsius: Boolean = true): Int = if (useCelsius) tempC else tempF
    fun displayHigh(useCelsius: Boolean = true): Int = if (useCelsius) highC else highF
    fun displayLow(useCelsius: Boolean = true): Int = if (useCelsius) lowC else lowF

    companion object {
        fun from(data: WeatherForecastData): WidgetSnapshot {
            val moon = MoonPhaseCalculator.forDate()
            val days = data.dailyList
            fun dayName(i: Int) = days.getOrNull(i)?.dayName?.take(5)?.uppercase() ?: "—"
            fun dayHigh(i: Int) = days.getOrNull(i)?.maxTempC?.roundToInt() ?: data.highTempC.roundToInt()
            fun dayLow(i: Int) = days.getOrNull(i)?.minTempC?.roundToInt() ?: data.lowTempC.roundToInt()
            fun dayIcon(i: Int): String {
                val c = days.getOrNull(i)?.condition ?: data.condition
                return when (c) {
                    WeatherCondition.SUNNY, WeatherCondition.CLEAR -> "sun"
                    WeatherCondition.RAINY, WeatherCondition.HEAVY_RAIN, WeatherCondition.THUNDERSTORM -> "rain"
                    WeatherCondition.SNOWY -> "snow"
                    else -> "cloud"
                }
            }
            val aqi = data.airQualityIndex
            val aqiLabel = when {
                aqi <= 50 -> "GOOD"
                aqi <= 100 -> "MODERATE"
                aqi <= 150 -> "UNHEALTHY"
                else -> "POOR"
            }
            return WidgetSnapshot(
                cityName = data.cityName.uppercase(),
                tempC = data.currentTempC.roundToInt(),
                tempF = data.currentTempF.roundToInt(),
                highC = data.highTempC.roundToInt(),
                highF = data.highTempF.roundToInt(),
                lowC = data.lowTempC.roundToInt(),
                lowF = data.lowTempF.roundToInt(),
                conditionLabel = data.condition.label,
                conditionKey = data.condition.name,
                aqi = aqi,
                aqiLabel = aqiLabel,
                moonPhase = moon.phaseName,
                moonIllum = (moon.illumination * 100).roundToInt(),
                day0Name = dayName(0),
                day0High = dayHigh(0),
                day0Low = dayLow(0),
                day0Icon = dayIcon(0),
                day1Name = dayName(1),
                day1High = dayHigh(1),
                day1Low = dayLow(1),
                day1Icon = dayIcon(1),
                day2Name = dayName(2),
                day2High = dayHigh(2),
                day2Low = dayLow(2),
                day2Icon = dayIcon(2),
                updatedAtMs = System.currentTimeMillis()
            )
        }

        fun empty() = WidgetSnapshot(updatedAtMs = 0L)
    }
}

object WidgetSnapshotStore {
    private const val PREFS = "weather_widget_snapshot"
    private const val KEY = "snapshot_csv"

    fun save(context: Context, snap: WidgetSnapshot) {
        val csv = listOf(
            snap.cityName,
            snap.tempC, snap.tempF, snap.highC, snap.highF, snap.lowC, snap.lowF,
            snap.conditionLabel.replace(',', ' '),
            snap.conditionKey,
            snap.aqi, snap.aqiLabel,
            snap.moonPhase.replace(',', ' '), snap.moonIllum,
            snap.day0Name, snap.day0High, snap.day0Low, snap.day0Icon,
            snap.day1Name, snap.day1High, snap.day1Low, snap.day1Icon,
            snap.day2Name, snap.day2High, snap.day2Low, snap.day2Icon,
            snap.updatedAtMs
        ).joinToString("|")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, csv)
            .apply()
    }

    fun load(context: Context): WidgetSnapshot {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return WidgetSnapshot.empty()
        val p = raw.split("|")
        if (p.size < 26) return WidgetSnapshot.empty()
        return try {
            WidgetSnapshot(
                cityName = p[0],
                tempC = p[1].toInt(),
                tempF = p[2].toInt(),
                highC = p[3].toInt(),
                highF = p[4].toInt(),
                lowC = p[5].toInt(),
                lowF = p[6].toInt(),
                conditionLabel = p[7],
                conditionKey = p[8],
                aqi = p[9].toInt(),
                aqiLabel = p[10],
                moonPhase = p[11],
                moonIllum = p[12].toInt(),
                day0Name = p[13],
                day0High = p[14].toInt(),
                day0Low = p[15].toInt(),
                day0Icon = p[16],
                day1Name = p[17],
                day1High = p[18].toInt(),
                day1Low = p[19].toInt(),
                day1Icon = p[20],
                day2Name = p[21],
                day2High = p[22].toInt(),
                day2Low = p[23].toInt(),
                day2Icon = p[24],
                updatedAtMs = p[25].toLong()
            )
        } catch (_: Exception) {
            WidgetSnapshot.empty()
        }
    }
}
