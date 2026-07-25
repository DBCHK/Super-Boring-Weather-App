package com.example.data.api

import com.squareup.moshi.Json

data class OpenWeatherCurrentResponse(
    val coord: OpenWeatherCoord?,
    val weather: List<OpenWeatherWeatherItem>?,
    val main: OpenWeatherMainData?,
    val wind: OpenWeatherWindData?,
    val clouds: OpenWeatherCloudsData?,
    val dt: Long?,
    val sys: OpenWeatherSysData?,
    val name: String?
)

data class OpenWeatherForecastResponse(
    val list: List<OpenWeatherForecastItem>?,
    val city: OpenWeatherCityData?
)

data class OpenWeatherForecastItem(
    val dt: Long?,
    val main: OpenWeatherMainData?,
    val weather: List<OpenWeatherWeatherItem>?,
    val clouds: OpenWeatherCloudsData?,
    val wind: OpenWeatherWindData?,
    val pop: Float?,
    @field:Json(name = "dt_txt") val dtTxt: String?
)

data class OpenWeatherCoord(
    val lon: Double?,
    val lat: Double?
)

data class OpenWeatherWeatherItem(
    val id: Int?,
    val main: String?,
    val description: String?,
    val icon: String?
)

data class OpenWeatherMainData(
    val temp: Float?,
    @field:Json(name = "feels_like") val feelsLike: Float?,
    @field:Json(name = "temp_min") val tempMin: Float?,
    @field:Json(name = "temp_max") val tempMax: Float?,
    val pressure: Int?,
    val humidity: Int?
)

data class OpenWeatherWindData(
    val speed: Float?,
    val deg: Int?
)

data class OpenWeatherCloudsData(
    val all: Int?
)

data class OpenWeatherSysData(
    val country: String?,
    val sunrise: Long?,
    val sunset: Long?
)

data class OpenWeatherCityData(
    val id: Long?,
    val name: String?,
    val country: String?
)
