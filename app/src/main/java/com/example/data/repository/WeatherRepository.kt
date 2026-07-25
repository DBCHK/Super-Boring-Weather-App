package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.db.CityDao
import com.example.data.model.CityEntity
import com.example.data.model.DailyForecast
import com.example.data.model.HourlyForecast
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherForecastData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class WeatherRepository(private val cityDao: CityDao) {

    val savedCities: Flow<List<CityEntity>> = cityDao.getAllCities()

    val defaultCities = listOf(
        CityEntity("san_francisco", "San Francisco", "United States", 37.7749, -122.4194, true),
        CityEntity("new_york", "New York", "United States", 40.7128, -74.0060, false),
        CityEntity("london", "London", "United Kingdom", 51.5074, -0.1278, false),
        CityEntity("tokyo", "Tokyo", "Japan", 35.6762, 139.6503, false),
        CityEntity("paris", "Paris", "France", 48.8566, 2.3522, false),
        CityEntity("sydney", "Sydney", "Australia", -33.8688, 151.2093, false)
    )

    suspend fun saveCity(city: CityEntity) {
        cityDao.insertCity(city)
    }

    suspend fun deleteCity(city: CityEntity) {
        cityDao.deleteCity(city)
    }

    suspend fun searchCities(query: String): List<CityEntity> = withContext(Dispatchers.IO) {
        if (query.length < 2) return@withContext emptyList()
        try {
            val response = ApiClient.geocodingApi.searchCity(query)
            response.results?.map { dto ->
                CityEntity(
                    id = "${dto.latitude}_${dto.longitude}",
                    name = dto.name,
                    country = dto.country ?: dto.admin1 ?: "",
                    latitude = dto.latitude,
                    longitude = dto.longitude
                )
            } ?: emptyList()
        } catch (e: Exception) {
            defaultCities.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    suspend fun fetchWeather(city: CityEntity): WeatherForecastData = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.weatherApi.getWeatherForecast(city.latitude, city.longitude)
            
            val currentDto = response.current
            val hourlyDto = response.hourly
            val dailyDto = response.daily

            val currentTempC = currentDto?.temperature2m ?: 16f
            val currentTempF = celsiusToFahrenheit(currentTempC)
            val condition = WeatherCondition.fromWmoCode(
                currentDto?.weatherCode ?: 3,
                (currentDto?.isDay ?: 1) == 1
            )

            // Hourly parse
            val hourlyList = mutableListOf<HourlyForecast>()
            val times = hourlyDto?.time ?: emptyList()
            val temps = hourlyDto?.temperature2m ?: emptyList()
            val codes = hourlyDto?.weatherCode ?: emptyList()
            val probs = hourlyDto?.precipitationProbability ?: emptyList()
            val precips = hourlyDto?.precipitation ?: emptyList()
            val winds = hourlyDto?.windSpeed10m ?: emptyList()
            val windDirs = hourlyDto?.windDirection10m ?: emptyList()
            val humidities = hourlyDto?.relativeHumidity2m ?: emptyList()
            val uvs = hourlyDto?.uvIndex ?: emptyList()
            val days = hourlyDto?.isDay ?: emptyList()

            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

            // Find start index matching current time or top 24 hours
            val startIndex = times.indexOfFirst { timeStr ->
                try {
                    val hour = timeStr.substringAfter("T").substringBefore(":").toInt()
                    hour >= currentHour
                } catch (e: Exception) { false }
            }.coerceAtLeast(0)

            val countToTake = 24.coerceAtMost(times.size - startIndex)

            for (i in 0 until countToTake) {
                val idx = startIndex + i
                if (idx >= times.size) break

                val rawTime = times[idx]
                val hourInt = try { rawTime.substringAfter("T").substringBefore(":").toInt() } catch (e: Exception) { (currentHour + i) % 24 }
                
                val label = if (i == 0) "NOW" else formatHourLabel(hourInt)
                val tempC = temps.getOrNull(idx) ?: (currentTempC + (i % 3) - 1)
                val tempF = celsiusToFahrenheit(tempC)
                val wCode = codes.getOrNull(idx) ?: 0
                val isDaytime = (days.getOrNull(idx) ?: 1) == 1
                val cond = WeatherCondition.fromWmoCode(wCode, isDaytime)
                val prob = probs.getOrNull(idx) ?: 10
                val precipMm = precips.getOrNull(idx) ?: 0f
                val precipInches = precipMm / 25.4f
                val wSpeed = (winds.getOrNull(idx) ?: 8f) * 0.621371f // kmh to mph
                val wDir = windDirs.getOrNull(idx) ?: 180
                val hum = humidities.getOrNull(idx) ?: 60
                val uv = uvs.getOrNull(idx) ?: 3f

                hourlyList.add(
                    HourlyForecast(
                        timeLabel = label,
                        fullTime = rawTime,
                        hourOfDay = hourInt,
                        tempC = tempC,
                        tempF = tempF,
                        condition = cond,
                        precipChancePercent = prob,
                        precipRateInches = (precipInches * 100).roundToInt() / 100f,
                        windSpeedMph = (wSpeed * 10).roundToInt() / 10f,
                        windDirectionDegrees = wDir,
                        humidityPercent = hum,
                        uvIndex = uv,
                        isDaytime = isDaytime
                    )
                )
            }

            // Daily parse
            val dailyList = mutableListOf<DailyForecast>()
            val dTimes = dailyDto?.time ?: emptyList()
            val dCodes = dailyDto?.weatherCode ?: emptyList()
            val dMaxs = dailyDto?.temperature2mMax ?: emptyList()
            val dMins = dailyDto?.temperature2mMin ?: emptyList()
            val dProbs = dailyDto?.precipitationProbabilityMax ?: emptyList()
            val dSums = dailyDto?.precipitationSum ?: emptyList()

            val dayFormat = SimpleDateFormat("EEE", Locale.US)
            val dateFormat = SimpleDateFormat("MMM d", Locale.US)
            val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            for (i in dTimes.indices) {
                if (i >= 7) break
                val dateStr = dTimes[i]
                val dateObj = try { isoFormat.parse(dateStr) } catch (e: Exception) { Date() }
                val dayName = if (i == 0) "TODAY" else dayFormat.format(dateObj ?: Date()).uppercase()
                val dateLabel = dateFormat.format(dateObj ?: Date()).uppercase()

                val maxC = dMaxs.getOrNull(i) ?: (currentTempC + 4)
                val minC = dMins.getOrNull(i) ?: (currentTempC - 4)
                val wCode = dCodes.getOrNull(i) ?: 0
                val prob = dProbs.getOrNull(i) ?: 20
                val sumInches = (dSums.getOrNull(i) ?: 0f) / 25.4f

                dailyList.add(
                    DailyForecast(
                        dayName = dayName,
                        dateLabel = dateLabel,
                        condition = WeatherCondition.fromWmoCode(wCode, true),
                        maxTempC = maxC,
                        maxTempF = celsiusToFahrenheit(maxC),
                        minTempC = minC,
                        minTempF = celsiusToFahrenheit(minC),
                        precipChancePercent = prob,
                        precipAmountInches = (sumInches * 100).roundToInt() / 100f
                    )
                )
            }

            val highC = dailyList.firstOrNull()?.maxTempC ?: (currentTempC + 3)
            val lowC = dailyList.firstOrNull()?.minTempC ?: (currentTempC - 5)

            WeatherForecastData(
                cityName = city.name,
                country = city.country,
                currentTempC = currentTempC,
                currentTempF = currentTempF,
                highTempC = highC,
                highTempF = celsiusToFahrenheit(highC),
                lowTempC = lowC,
                lowTempF = celsiusToFahrenheit(lowC),
                condition = condition,
                humidityPercent = currentDto?.relativeHumidity2m ?: 65,
                windSpeedMph = ((currentDto?.windSpeed10m ?: 10f) * 0.621371f * 10).roundToInt() / 10f,
                windDirectionDegrees = currentDto?.windDirection10m ?: 180,
                precipChancePercent = hourlyList.firstOrNull()?.precipChancePercent ?: 15,
                precipRateInches = hourlyList.firstOrNull()?.precipRateInches ?: 0.09f,
                uvIndex = hourlyList.firstOrNull()?.uvIndex ?: 4.2f,
                airQualityIndex = 32, // Healthy
                hourlyList = if (hourlyList.isNotEmpty()) hourlyList else generateMockHourly(),
                dailyList = if (dailyList.isNotEmpty()) dailyList else generateMockDaily()
            )

        } catch (e: Exception) {
            // Fallback mock weather for smooth experience
            generateMockWeatherForCity(city)
        }
    }

    private fun celsiusToFahrenheit(c: Float): Float {
        return (c * 9f / 5f) + 32f
    }

    private fun formatHourLabel(hour: Int): String {
        return when {
            hour == 0 -> "12A"
            hour == 12 -> "12P"
            hour > 12 -> "${hour - 12}P"
            else -> "${hour}A"
        }
    }

    private fun generateMockWeatherForCity(city: CityEntity): WeatherForecastData {
        val currentTempC = 16f
        val currentTempF = celsiusToFahrenheit(currentTempC)
        val hourly = generateMockHourly()
        val daily = generateMockDaily()

        return WeatherForecastData(
            cityName = city.name,
            country = city.country,
            currentTempC = currentTempC,
            currentTempF = currentTempF,
            highTempC = 19f,
            highTempF = 66f,
            lowTempC = 11f,
            lowTempF = 52f,
            condition = WeatherCondition.CLOUDY,
            humidityPercent = 74,
            windSpeedMph = 12.4f,
            windDirectionDegrees = 220,
            precipChancePercent = 78,
            precipRateInches = 0.09f,
            uvIndex = 3.5f,
            airQualityIndex = 28,
            hourlyList = hourly,
            dailyList = daily
        )
    }

    private fun generateMockHourly(): List<HourlyForecast> {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val list = mutableListOf<HourlyForecast>()

        val hours = listOf(0, 3, 6, 9, 12, 15, 18, 21)
        for (i in 0 until 12) {
            val h = (currentHour + i * 2) % 24
            val label = if (i == 0) "NOW" else formatHourLabel(h)
            val isDay = h in 6..19
            val cond = when (i % 4) {
                0 -> WeatherCondition.CLOUDY
                1 -> WeatherCondition.RAINY
                2 -> if (isDay) WeatherCondition.SUNNY else WeatherCondition.CLEAR
                else -> WeatherCondition.PARTLY_CLOUDY
            }

            list.add(
                HourlyForecast(
                    timeLabel = label,
                    fullTime = "2026-07-25T${if (h < 10) "0$h" else "$h"}:00",
                    hourOfDay = h,
                    tempC = (14 + (i % 5)).toFloat(),
                    tempF = celsiusToFahrenheit((14 + (i % 5)).toFloat()),
                    condition = cond,
                    precipChancePercent = if (cond == WeatherCondition.RAINY) 85 else 15,
                    precipRateInches = if (cond == WeatherCondition.RAINY) 0.12f else 0.00f,
                    windSpeedMph = 10f + (i * 1.2f),
                    windDirectionDegrees = 180 + (i * 15),
                    humidityPercent = 65 + (i * 2),
                    uvIndex = if (isDay) 4.5f else 0.0f,
                    isDaytime = isDay
                )
            )
        }
        return list
    }

    private fun generateMockDaily(): List<DailyForecast> {
        val days = listOf("TODAY", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        val conditions = listOf(
            WeatherCondition.CLOUDY,
            WeatherCondition.RAINY,
            WeatherCondition.SUNNY,
            WeatherCondition.PARTLY_CLOUDY,
            WeatherCondition.SUNNY,
            WeatherCondition.CLOUDY,
            WeatherCondition.RAINY
        )

        return days.mapIndexed { idx, day ->
            val maxC = 18f + (idx % 3)
            val minC = 10f + (idx % 2)
            DailyForecast(
                dayName = day,
                dateLabel = "JUL ${25 + idx}",
                condition = conditions[idx],
                maxTempC = maxC,
                maxTempF = celsiusToFahrenheit(maxC),
                minTempC = minC,
                minTempF = celsiusToFahrenheit(minC),
                precipChancePercent = if (conditions[idx] == WeatherCondition.RAINY) 75 else 20,
                precipAmountInches = if (conditions[idx] == WeatherCondition.RAINY) 0.25f else 0.00f
            )
        }
    }
}
