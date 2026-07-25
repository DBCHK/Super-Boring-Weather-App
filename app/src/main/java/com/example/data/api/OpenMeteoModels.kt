package com.example.data.api

import com.squareup.moshi.Json

data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String?,
    @field:Json(name = "current") val current: CurrentWeatherDto?,
    @field:Json(name = "hourly") val hourly: HourlyWeatherDto?,
    @field:Json(name = "daily") val daily: DailyWeatherDto?
)

data class CurrentWeatherDto(
    val time: String,
    @field:Json(name = "temperature_2m") val temperature2m: Float?,
    @field:Json(name = "relative_humidity_2m") val relativeHumidity2m: Int?,
    @field:Json(name = "is_day") val isDay: Int?,
    val precipitation: Float?,
    val rain: Float?,
    @field:Json(name = "weather_code") val weatherCode: Int?,
    @field:Json(name = "cloud_cover") val cloudCover: Int?,
    @field:Json(name = "wind_speed_10m") val windSpeed10m: Float?,
    @field:Json(name = "wind_direction_10m") val windDirection10m: Int?
)

data class HourlyWeatherDto(
    val time: List<String>?,
    @field:Json(name = "temperature_2m") val temperature2m: List<Float>?,
    @field:Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Int>?,
    @field:Json(name = "precipitation_probability") val precipitationProbability: List<Int>?,
    val precipitation: List<Float>?,
    @field:Json(name = "weather_code") val weatherCode: List<Int>?,
    @field:Json(name = "cloud_cover") val cloudCover: List<Int>?,
    @field:Json(name = "wind_speed_10m") val windSpeed10m: List<Float>?,
    @field:Json(name = "wind_direction_10m") val windDirection10m: List<Int>?,
    @field:Json(name = "uv_index") val uvIndex: List<Float>?,
    @field:Json(name = "is_day") val isDay: List<Int>?
)

data class DailyWeatherDto(
    val time: List<String>?,
    @field:Json(name = "weather_code") val weatherCode: List<Int>?,
    @field:Json(name = "temperature_2m_max") val temperature2mMax: List<Float>?,
    @field:Json(name = "temperature_2m_min") val temperature2mMin: List<Float>?,
    @field:Json(name = "precipitation_sum") val precipitationSum: List<Float>?,
    @field:Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>?
)

data class GeocodingResponse(
    val results: List<GeocodingResultDto>?
)

data class GeocodingResultDto(
    val id: Long?,
    val name: String,
    val country: String?,
    @field:Json(name = "country_code") val countryCode: String?,
    @field:Json(name = "admin1") val admin1: String?,
    val latitude: Double,
    val longitude: Double
)
