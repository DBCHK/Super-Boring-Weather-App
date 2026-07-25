package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    @Json(name = "current") val current: CurrentWeatherDto? = null,
    @Json(name = "hourly") val hourly: HourlyWeatherDto? = null,
    @Json(name = "daily") val daily: DailyWeatherDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    val time: String? = null,
    @Json(name = "temperature_2m") val temperature2m: Double? = null,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @Json(name = "is_day") val isDay: Int? = null,
    val precipitation: Double? = null,
    val rain: Double? = null,
    @Json(name = "weather_code") val weatherCode: Int? = null,
    @Json(name = "cloud_cover") val cloudCover: Int? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double? = null,
    @Json(name = "wind_direction_10m") val windDirection10m: Int? = null
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherDto(
    val time: List<String>? = null,
    @Json(name = "temperature_2m") val temperature2m: List<Double?>? = null,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Int?>? = null,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int?>? = null,
    val precipitation: List<Double?>? = null,
    @Json(name = "weather_code") val weatherCode: List<Int?>? = null,
    @Json(name = "cloud_cover") val cloudCover: List<Int?>? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double?>? = null,
    @Json(name = "wind_direction_10m") val windDirection10m: List<Int?>? = null,
    @Json(name = "uv_index") val uvIndex: List<Double?>? = null,
    @Json(name = "is_day") val isDay: List<Int?>? = null
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
    val time: List<String>? = null,
    @Json(name = "weather_code") val weatherCode: List<Int?>? = null,
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double?>? = null,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double?>? = null,
    @Json(name = "precipitation_sum") val precipitationSum: List<Double?>? = null,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int?>? = null,
    @Json(name = "uv_index_max") val uvIndexMax: List<Double?>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResultDto>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResultDto(
    val id: Long? = null,
    val name: String,
    val country: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    @Json(name = "admin1") val admin1: String? = null,
    val latitude: Double,
    val longitude: Double
)
