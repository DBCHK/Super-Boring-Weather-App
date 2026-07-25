package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WeatherCondition(val label: String) {
    CLEAR("CLEAR"),
    SUNNY("SUNNY"),
    CLOUDY("CLOUDY"),
    MOSTLY_CLOUDY("MOSTLY CLOUDY"),
    PARTLY_CLOUDY("PARTLY CLOUDY"),
    RAINY("RAINY"),
    HEAVY_RAIN("HEAVY RAIN"),
    SNOWY("SNOWY"),
    THUNDERSTORM("THUNDERSTORM"),
    HAZE("HAZE / MIST"),
    WINDY("WINDY");

    companion object {
        fun fromWmoCode(code: Int, isDay: Boolean = true): WeatherCondition {
            return when (code) {
                0 -> if (isDay) SUNNY else CLEAR
                1, 2 -> PARTLY_CLOUDY
                3 -> CLOUDY
                45, 48 -> HAZE
                51, 53, 55, 56, 57 -> RAINY
                61, 63, 65, 66, 67, 80, 81, 82 -> RAINY
                71, 73, 75, 77, 85, 86 -> SNOWY
                95, 96, 99 -> THUNDERSTORM
                else -> CLOUDY
            }
        }

        fun fromOpenWeatherId(id: Int, iconStr: String = "d"): WeatherCondition {
            val isDay = iconStr.contains("d")
            return when (id) {
                in 200..232 -> THUNDERSTORM
                in 300..321, in 500..531 -> RAINY
                in 600..622 -> SNOWY
                in 701..781 -> HAZE
                800 -> if (isDay) SUNNY else CLEAR
                801, 802 -> PARTLY_CLOUDY
                803, 804 -> CLOUDY
                else -> if (isDay) SUNNY else CLEAR
            }
        }
    }
}

data class HourlyForecast(
    val timeLabel: String, // e.g. "NOW", "12A", "3A", "6A", "9A", "12P", "3P", "6P", "9P"
    val fullTime: String, // e.g. "2026-07-25T14:00"
    val hourOfDay: Int,
    val tempC: Float,
    val tempF: Float,
    val condition: WeatherCondition,
    val precipChancePercent: Int,
    val precipRateInches: Float, // in/hr
    val windSpeedMph: Float,
    val windDirectionDegrees: Int,
    val humidityPercent: Int,
    val uvIndex: Float,
    val isDaytime: Boolean
)

data class DailyForecast(
    val dayName: String, // e.g. "MON", "TUE", "WED"
    val dateLabel: String, // e.g. "JUL 26"
    val condition: WeatherCondition,
    val maxTempC: Float,
    val maxTempF: Float,
    val minTempC: Float,
    val minTempF: Float,
    val precipChancePercent: Int,
    val precipAmountInches: Float
)

data class WeatherForecastData(
    val cityName: String,
    val country: String,
    val currentTempC: Float,
    val currentTempF: Float,
    val highTempC: Float,
    val highTempF: Float,
    val lowTempC: Float,
    val lowTempF: Float,
    val condition: WeatherCondition,
    val humidityPercent: Int,
    val windSpeedMph: Float,
    val windDirectionDegrees: Int,
    val precipChancePercent: Int,
    val precipRateInches: Float,
    val uvIndex: Float,
    val airQualityIndex: Int, // AQI 1-500
    val hourlyList: List<HourlyForecast>,
    val dailyList: List<DailyForecast>
)

@Entity(tableName = "saved_cities")
data class CityEntity(
    @PrimaryKey val id: String, // e.g. "lat_long" or "london_uk"
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val isDefault: Boolean = false
)
