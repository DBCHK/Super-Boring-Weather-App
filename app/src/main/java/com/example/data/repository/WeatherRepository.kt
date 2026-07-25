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
        // Open-Meteo first (accurate, no API key). Do NOT swallow failures into mock —
        // that left every city stuck at ~15°C / 59°F offline placeholders.
        val openMeteoError: Exception? = try {
            return@withContext fetchFromOpenMeteo(city)
        } catch (e: Exception) {
            e.printStackTrace()
            e
        }

        val openWeatherError: Exception? = try {
            return@withContext fetchFromOpenWeather(city)
        } catch (e: Exception) {
            e.printStackTrace()
            e
        }

        // Last resort only — still city-dependent so location changes are visible offline
        generateMockWeatherForCity(city).also {
            println(
                "WeatherRepository: live APIs failed for ${city.name} " +
                    "(open-meteo=${openMeteoError?.message}, openweather=${openWeatherError?.message}); using mock"
            )
        }
    }

    private suspend fun fetchFromOpenWeather(city: CityEntity): WeatherForecastData {
        val currentResp = ApiClient.openWeatherApi.getCurrentWeather(city.latitude, city.longitude)
        val forecastResp = ApiClient.openWeatherApi.getWeatherForecast(city.latitude, city.longitude)

        val weatherItem = currentResp.weather?.firstOrNull()
        val weatherId = weatherItem?.id ?: 800
        val icon = weatherItem?.icon ?: "d"
        val condition = WeatherCondition.fromOpenWeatherId(weatherId, icon)

        val currentTempC = currentResp.main?.temp ?: 18f
        val currentTempF = celsiusToFahrenheit(currentTempC)
        val highC = currentResp.main?.tempMax ?: (currentTempC + 3f)
        val lowC = currentResp.main?.tempMin ?: (currentTempC - 4f)

        val humidity = currentResp.main?.humidity ?: 65
        val windSpeedMph = ((currentResp.wind?.speed ?: 4.5f) * 2.23694f * 10).roundToInt() / 10f
        val windDeg = currentResp.wind?.deg ?: 180

        val hourlyList = mutableListOf<HourlyForecast>()
        val dailyList = mutableListOf<DailyForecast>()

        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val dateFormat = SimpleDateFormat("MMM d", Locale.US)
        val hourFormat = SimpleDateFormat("ha", Locale.US)

        val items = forecastResp.list ?: emptyList()

        for ((idx, item) in items.take(16).withIndex()) {
            val itemTempC = item.main?.temp ?: currentTempC
            val itemWeather = item.weather?.firstOrNull()
            val itemCond = WeatherCondition.fromOpenWeatherId(itemWeather?.id ?: 800, itemWeather?.icon ?: "d")
            val dtMs = (item.dt ?: (System.currentTimeMillis() / 1000)) * 1000
            val date = Date(dtMs)
            val cal = Calendar.getInstance().apply { time = date }
            val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
            val label = if (idx == 0) "NOW" else hourFormat.format(date).uppercase()
            val isDay = (itemWeather?.icon ?: "d").contains("d")
            val popPct = ((item.pop ?: 0f) * 100f).roundToInt().coerceIn(0, 100)
            // Estimate rate from pop + condition (OpenWeather free tier lacks volume per step reliably)
            val rateIn = when {
                itemCond == WeatherCondition.HEAVY_RAIN || itemCond == WeatherCondition.THUNDERSTORM -> 0.15f
                itemCond == WeatherCondition.RAINY -> 0.06f + popPct * 0.001f
                itemCond == WeatherCondition.SNOWY -> 0.04f
                popPct > 40 -> 0.02f
                else -> 0f
            }

            hourlyList.add(
                HourlyForecast(
                    timeLabel = label,
                    fullTime = item.dtTxt ?: "",
                    hourOfDay = hourOfDay,
                    tempC = itemTempC,
                    tempF = celsiusToFahrenheit(itemTempC),
                    condition = itemCond,
                    precipChancePercent = popPct,
                    precipRateInches = (rateIn * 100).roundToInt() / 100f,
                    windSpeedMph = ((item.wind?.speed ?: 4f) * 2.23694f * 10).roundToInt() / 10f,
                    windDirectionDegrees = item.wind?.deg ?: 180,
                    humidityPercent = item.main?.humidity ?: 60,
                    uvIndex = estimateUvForHour(hourOfDay, isDay, itemCond),
                    isDaytime = isDay
                )
            )
        }

        val groupedByDay = items.groupBy { item ->
            val dtMs = (item.dt ?: (System.currentTimeMillis() / 1000)) * 1000
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(dtMs))
        }

        for ((idx, entry) in groupedByDay.entries.take(7).withIndex()) {
            val dayItems = entry.value
            val firstItem = dayItems.first()
            val dtMs = (firstItem.dt ?: (System.currentTimeMillis() / 1000)) * 1000
            val date = Date(dtMs)

            val maxC = dayItems.mapNotNull { it.main?.tempMax ?: it.main?.temp }.maxOrNull() ?: currentTempC
            val minC = dayItems.mapNotNull { it.main?.tempMin ?: it.main?.temp }.minOrNull() ?: currentTempC
            val pop = dayItems.mapNotNull { it.pop }.maxOrNull() ?: 0f
            // Prefer midday / most severe condition for the day icon
            val dayCond = dayItems
                .mapNotNull { it.weather?.firstOrNull() }
                .maxByOrNull { severityScore(it.id ?: 800) }
                ?.let { WeatherCondition.fromOpenWeatherId(it.id ?: 800, it.icon ?: "d") }
                ?: WeatherCondition.fromOpenWeatherId(
                    firstItem.weather?.firstOrNull()?.id ?: 800,
                    firstItem.weather?.firstOrNull()?.icon ?: "d"
                )

            val dayName = if (idx == 0) "TODAY" else dayFormat.format(date).uppercase()
            val dateLabel = dateFormat.format(date).uppercase()
            val avgHum = dayItems.mapNotNull { it.main?.humidity }.average().takeIf { !it.isNaN() }?.toInt() ?: humidity
            val dayUv = dayItems.map { item ->
                val cal = Calendar.getInstance().apply {
                    time = Date((item.dt ?: 0L) * 1000)
                }
                val h = cal.get(Calendar.HOUR_OF_DAY)
                val dayIcon = (item.weather?.firstOrNull()?.icon ?: "d").contains("d")
                val cond = WeatherCondition.fromOpenWeatherId(
                    item.weather?.firstOrNull()?.id ?: 800,
                    item.weather?.firstOrNull()?.icon ?: "d"
                )
                estimateUvForHour(h, dayIcon, cond)
            }.maxOrNull() ?: 3f
            val precipInches = ((pop * 0.25f) * 100).roundToInt() / 100f

            dailyList.add(
                DailyForecast(
                    dayName = dayName,
                    dateLabel = dateLabel,
                    condition = dayCond,
                    maxTempC = maxC,
                    maxTempF = celsiusToFahrenheit(maxC),
                    minTempC = minC,
                    minTempF = celsiusToFahrenheit(minC),
                    precipChancePercent = (pop * 100).roundToInt().coerceIn(0, 100),
                    precipAmountInches = precipInches,
                    uvIndexMax = dayUv,
                    humidityPercent = avgHum
                )
            )
        }

        val cityName = if (!currentResp.name.isNullOrBlank()) currentResp.name else city.name
        val nowUv = hourlyList.firstOrNull()?.uvIndex ?: estimateUvForHour(
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            true,
            condition
        )

        return WeatherForecastData(
            cityName = cityName,
            country = currentResp.sys?.country ?: city.country,
            currentTempC = currentTempC,
            currentTempF = currentTempF,
            highTempC = highC,
            highTempF = celsiusToFahrenheit(highC),
            lowTempC = lowC,
            lowTempF = celsiusToFahrenheit(lowC),
            condition = condition,
            humidityPercent = humidity,
            windSpeedMph = windSpeedMph,
            windDirectionDegrees = windDeg,
            precipChancePercent = hourlyList.firstOrNull()?.precipChancePercent ?: 20,
            precipRateInches = hourlyList.firstOrNull()?.precipRateInches ?: 0f,
            uvIndex = nowUv,
            airQualityIndex = estimateAqi(city, humidity, windSpeedMph),
            hourlyList = if (hourlyList.isNotEmpty()) hourlyList else generateMockHourly(city),
            dailyList = if (dailyList.isNotEmpty()) dailyList else generateMockDaily(city)
        )
    }

    private suspend fun fetchFromOpenMeteo(city: CityEntity): WeatherForecastData {
        val response = ApiClient.weatherApi.getWeatherForecast(city.latitude, city.longitude)

        val currentDto = response.current
        val hourlyDto = response.hourly
        val dailyDto = response.daily

        // Require at least current temperature — otherwise treat as hard failure
        val currentTempC = currentDto?.temperature2m?.toFloat()
            ?: throw IllegalStateException("Open-Meteo returned no current temperature for ${city.name}")
        val currentTempF = celsiusToFahrenheit(currentTempC)
        val condition = WeatherCondition.fromWmoCode(
            currentDto?.weatherCode ?: 3,
            (currentDto?.isDay ?: 1) == 1
        )

        val times = hourlyDto?.time.orEmpty()
        val temps = hourlyDto?.temperature2m.orEmpty()
        val codes = hourlyDto?.weatherCode.orEmpty()
        val probs = hourlyDto?.precipitationProbability.orEmpty()
        val precips = hourlyDto?.precipitation.orEmpty()
        val winds = hourlyDto?.windSpeed10m.orEmpty()
        val windDirs = hourlyDto?.windDirection10m.orEmpty()
        val humidities = hourlyDto?.relativeHumidity2m.orEmpty()
        val uvs = hourlyDto?.uvIndex.orEmpty()
        val days = hourlyDto?.isDay.orEmpty()

        // Align "NOW" to the current hour using the API current.time (timezone=auto)
        val currentTimeStr = currentDto?.time
        val startIndex = when {
            currentTimeStr != null && times.isNotEmpty() -> {
                val exact = times.indexOfFirst { it.startsWith(currentTimeStr.take(13)) } // yyyy-MM-ddTHH
                if (exact >= 0) {
                    exact
                } else {
                    // nearest hour at or before current
                    val currentHourKey = currentTimeStr.take(13)
                    times.indexOfLast { it.take(13) <= currentHourKey }.coerceAtLeast(0)
                }
            }
            times.isNotEmpty() -> {
                val hourNow = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                times.indexOfFirst { t ->
                    try {
                        t.substringAfter("T").substringBefore(":").toInt() >= hourNow
                    } catch (_: Exception) {
                        false
                    }
                }.let { if (it >= 0) it else 0 }
            }
            else -> 0
        }

        val hourlyList = mutableListOf<HourlyForecast>()
        val countToTake = 24.coerceAtMost((times.size - startIndex).coerceAtLeast(0))

        for (i in 0 until countToTake) {
            val idx = startIndex + i
            if (idx >= times.size) break

            val rawTime = times[idx]
            val hourInt = try {
                rawTime.substringAfter("T").substringBefore(":").toInt()
            } catch (_: Exception) {
                (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + i) % 24
            }

            val label = if (i == 0) "NOW" else formatHourLabel(hourInt)
            // Prefer live current temp for the NOW slot so UI never shows a stale hour
            val tempC = if (i == 0) {
                currentTempC
            } else {
                temps.getOrNull(idx)?.toFloat() ?: currentTempC
            }
            val wCode = codes.getOrNull(idx) ?: 0
            val isDaytime = (days.getOrNull(idx) ?: if (hourInt in 6..19) 1 else 0) == 1
            val cond = if (i == 0) condition else WeatherCondition.fromWmoCode(wCode ?: 0, isDaytime)
            val prob = (probs.getOrNull(idx) ?: 0)?.coerceIn(0, 100) ?: 0
            val precipMm = precips.getOrNull(idx)?.toFloat() ?: 0f
            val precipInches = precipMm / 25.4f
            val wSpeed = ((winds.getOrNull(idx)?.toFloat() ?: 8f) * 0.621371f)
            val wDir = windDirs.getOrNull(idx) ?: 180
            val hum = humidities.getOrNull(idx) ?: (currentDto?.relativeHumidity2m ?: 60)
            val uv = uvs.getOrNull(idx)?.toFloat() ?: 0f

            hourlyList.add(
                HourlyForecast(
                    timeLabel = label,
                    fullTime = rawTime,
                    hourOfDay = hourInt,
                    tempC = tempC,
                    tempF = celsiusToFahrenheit(tempC),
                    condition = cond,
                    precipChancePercent = prob,
                    precipRateInches = (precipInches * 100).roundToInt() / 100f,
                    windSpeedMph = (wSpeed * 10).roundToInt() / 10f,
                    windDirectionDegrees = wDir ?: 180,
                    humidityPercent = hum ?: 60,
                    uvIndex = (uv * 10).roundToInt() / 10f,
                    isDaytime = isDaytime
                )
            )
        }

        // If hourly block missing, still expose a single NOW slot from current
        if (hourlyList.isEmpty()) {
            val hourInt = try {
                currentTimeStr?.substringAfter("T")?.substringBefore(":")?.toInt()
                    ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            } catch (_: Exception) {
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            }
            hourlyList.add(
                HourlyForecast(
                    timeLabel = "NOW",
                    fullTime = currentTimeStr ?: "",
                    hourOfDay = hourInt,
                    tempC = currentTempC,
                    tempF = currentTempF,
                    condition = condition,
                    precipChancePercent = 0,
                    precipRateInches = ((currentDto?.precipitation?.toFloat() ?: 0f) / 25.4f * 100).roundToInt() / 100f,
                    windSpeedMph = (((currentDto?.windSpeed10m?.toFloat() ?: 0f) * 0.621371f) * 10).roundToInt() / 10f,
                    windDirectionDegrees = currentDto?.windDirection10m ?: 180,
                    humidityPercent = currentDto?.relativeHumidity2m ?: 60,
                    uvIndex = 0f,
                    isDaytime = (currentDto?.isDay ?: 1) == 1
                )
            )
        }

        val dailyList = mutableListOf<DailyForecast>()
        val dTimes = dailyDto?.time.orEmpty()
        val dCodes = dailyDto?.weatherCode.orEmpty()
        val dMaxs = dailyDto?.temperature2mMax.orEmpty()
        val dMins = dailyDto?.temperature2mMin.orEmpty()
        val dProbs = dailyDto?.precipitationProbabilityMax.orEmpty()
        val dSums = dailyDto?.precipitationSum.orEmpty()
        val dUvs = dailyDto?.uvIndexMax.orEmpty()

        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val dateFormat = SimpleDateFormat("MMM d", Locale.US)
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val humidityByDate = mutableMapOf<String, MutableList<Int>>()
        times.forEachIndexed { idx, t ->
            val dayKey = t.substringBefore("T")
            val hum = humidities.getOrNull(idx) ?: return@forEachIndexed
            humidityByDate.getOrPut(dayKey) { mutableListOf() }.add(hum)
        }

        for (i in dTimes.indices) {
            if (i >= 7) break
            val dateStr = dTimes[i]
            val dateObj = try {
                isoFormat.parse(dateStr)
            } catch (_: Exception) {
                Date()
            }
            val dayName = if (i == 0) "TODAY" else dayFormat.format(dateObj ?: Date()).uppercase()
            val dateLabel = dateFormat.format(dateObj ?: Date()).uppercase()

            val maxC = dMaxs.getOrNull(i)?.toFloat() ?: (currentTempC + 4)
            val minC = dMins.getOrNull(i)?.toFloat() ?: (currentTempC - 4)
            val wCode = dCodes.getOrNull(i) ?: 0
            val prob = (dProbs.getOrNull(i) ?: 0).coerceIn(0, 100)
            val sumInches = ((dSums.getOrNull(i)?.toFloat() ?: 0f) / 25.4f)
            val dayUv = dUvs.getOrNull(i)?.toFloat()
                ?: hourlyList.filter { it.fullTime.startsWith(dateStr) }.maxOfOrNull { it.uvIndex }
                ?: 0f
            val dayHum = humidityByDate[dateStr]?.average()?.toInt()
                ?: (currentDto?.relativeHumidity2m ?: 65)

            dailyList.add(
                DailyForecast(
                    dayName = dayName,
                    dateLabel = dateLabel,
                    condition = WeatherCondition.fromWmoCode(wCode ?: 0, true),
                    maxTempC = maxC,
                    maxTempF = celsiusToFahrenheit(maxC),
                    minTempC = minC,
                    minTempF = celsiusToFahrenheit(minC),
                    precipChancePercent = prob,
                    precipAmountInches = (sumInches * 100).roundToInt() / 100f,
                    uvIndexMax = (dayUv * 10).roundToInt() / 10f,
                    humidityPercent = dayHum
                )
            )
        }

        val highC = dailyList.firstOrNull()?.maxTempC ?: (currentTempC + 3)
        val lowC = dailyList.firstOrNull()?.minTempC ?: (currentTempC - 5)
        val windMph = (((currentDto?.windSpeed10m?.toFloat() ?: 10f) * 0.621371f) * 10).roundToInt() / 10f
        val hum = currentDto?.relativeHumidity2m ?: 65

        return WeatherForecastData(
            cityName = city.name,
            country = city.country,
            currentTempC = currentTempC,
            currentTempF = currentTempF,
            highTempC = highC,
            highTempF = celsiusToFahrenheit(highC),
            lowTempC = lowC,
            lowTempF = celsiusToFahrenheit(lowC),
            condition = condition,
            humidityPercent = hum,
            windSpeedMph = windMph,
            windDirectionDegrees = currentDto?.windDirection10m ?: 180,
            precipChancePercent = hourlyList.firstOrNull()?.precipChancePercent ?: 0,
            precipRateInches = hourlyList.firstOrNull()?.precipRateInches ?: 0f,
            uvIndex = hourlyList.firstOrNull()?.uvIndex
                ?: dailyList.firstOrNull()?.uvIndexMax
                ?: 0f,
            airQualityIndex = estimateAqi(city, hum, windMph),
            hourlyList = hourlyList,
            dailyList = if (dailyList.isNotEmpty()) dailyList else generateMockDaily(city)
        )
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

    /** Clear-sky UV curve by hour, reduced for clouds / precip. */
    private fun estimateUvForHour(hour: Int, isDaytime: Boolean, condition: WeatherCondition): Float {
        if (!isDaytime || hour !in 6..19) return 0f
        val solar = when (hour) {
            6, 19 -> 0.5f
            7, 18 -> 1.5f
            8, 17 -> 3.0f
            9, 16 -> 5.0f
            10, 15 -> 6.5f
            11, 14 -> 8.0f
            12, 13 -> 9.0f
            else -> 2f
        }
        val factor = when (condition) {
            WeatherCondition.SUNNY, WeatherCondition.CLEAR -> 1f
            WeatherCondition.PARTLY_CLOUDY -> 0.75f
            WeatherCondition.MOSTLY_CLOUDY, WeatherCondition.CLOUDY -> 0.45f
            WeatherCondition.HAZE -> 0.55f
            WeatherCondition.RAINY, WeatherCondition.HEAVY_RAIN, WeatherCondition.THUNDERSTORM -> 0.2f
            WeatherCondition.SNOWY -> 0.35f
            WeatherCondition.WINDY -> 0.85f
        }
        return ((solar * factor) * 10).roundToInt() / 10f
    }

    private fun severityScore(weatherId: Int): Int = when (weatherId) {
        in 200..232 -> 90
        in 500..531 -> 70
        in 300..321 -> 60
        in 600..622 -> 65
        in 701..781 -> 30
        800 -> 0
        801, 802 -> 10
        803, 804 -> 20
        else -> 15
    }

    /**
     * Lightweight AQI estimate when no air-quality endpoint is wired.
     * Varies by city seed + humidity/wind so it is not a flat constant.
     */
    private fun estimateAqi(city: CityEntity, humidity: Int, windMph: Float): Int {
        val seed = (city.latitude * 17 + city.longitude * 31).toInt().let { kotlin.math.abs(it) % 40 }
        val humidityPenalty = ((humidity - 40).coerceAtLeast(0) * 0.4f).toInt()
        val windBonus = (windMph * 1.2f).toInt().coerceAtMost(25)
        return (28 + seed + humidityPenalty - windBonus).coerceIn(12, 160)
    }

    private fun generateMockWeatherForCity(city: CityEntity): WeatherForecastData {
        // Location-aware placeholder so offline still changes with city/coords
        val baseTempC = mockBaseTempC(city)
        val currentTempC = baseTempC
        val currentTempF = celsiusToFahrenheit(currentTempC)
        val hourly = generateMockHourly(city, baseTempC)
        val daily = generateMockDaily(city, baseTempC)

        return WeatherForecastData(
            cityName = city.name,
            country = city.country,
            currentTempC = currentTempC,
            currentTempF = currentTempF,
            highTempC = baseTempC + 4f,
            highTempF = celsiusToFahrenheit(baseTempC + 4f),
            lowTempC = baseTempC - 5f,
            lowTempF = celsiusToFahrenheit(baseTempC - 5f),
            condition = WeatherCondition.CLOUDY,
            humidityPercent = 74,
            windSpeedMph = 12.4f,
            windDirectionDegrees = 220,
            precipChancePercent = 35,
            precipRateInches = 0.02f,
            uvIndex = 3.5f,
            airQualityIndex = estimateAqi(city, 74, 12.4f),
            hourlyList = hourly,
            dailyList = daily
        )
    }

    /** Rough climate estimate from latitude so mocks are not a single frozen value. */
    private fun mockBaseTempC(city: CityEntity): Float {
        val lat = kotlin.math.abs(city.latitude).toFloat()
        val lonSeed = ((city.longitude * 10).toInt() % 7)
        val base = 30f - lat * 0.42f + lonSeed * 0.6f
        return ((base * 10).roundToInt() / 10f).coerceIn(-15f, 38f)
    }

    private fun generateMockHourly(city: CityEntity, baseTempC: Float = mockBaseTempC(city)): List<HourlyForecast> {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val list = mutableListOf<HourlyForecast>()

        val chanceCurve = listOf(12, 18, 28, 45, 68, 82, 74, 55, 38, 22, 14, 10)
        val rateCurve = listOf(0f, 0f, 0.02f, 0.05f, 0.10f, 0.14f, 0.11f, 0.06f, 0.03f, 0f, 0f, 0f)

        for (i in 0 until 12) {
            val h = (currentHour + i * 2) % 24
            val label = if (i == 0) "NOW" else formatHourLabel(h)
            val isDay = h in 6..19
            val chance = chanceCurve[i]
            val rate = rateCurve[i]
            val cond = when {
                chance >= 70 -> WeatherCondition.RAINY
                chance >= 45 -> WeatherCondition.MOSTLY_CLOUDY
                chance >= 25 -> WeatherCondition.PARTLY_CLOUDY
                isDay -> WeatherCondition.SUNNY
                else -> WeatherCondition.CLEAR
            }
            val tempC = baseTempC + (i % 5) - 1f - if (isDay) 0f else 2f

            list.add(
                HourlyForecast(
                    timeLabel = label,
                    fullTime = "2026-07-25T${if (h < 10) "0$h" else "$h"}:00",
                    hourOfDay = h,
                    tempC = tempC,
                    tempF = celsiusToFahrenheit(tempC),
                    condition = cond,
                    precipChancePercent = chance,
                    precipRateInches = rate,
                    windSpeedMph = 10f + (i * 1.2f),
                    windDirectionDegrees = 180 + (i * 15),
                    humidityPercent = 65 + (i * 2),
                    uvIndex = estimateUvForHour(h, isDay, cond),
                    isDaytime = isDay
                )
            )
        }
        return list
    }

    private fun generateMockDaily(city: CityEntity, baseTempC: Float = mockBaseTempC(city)): List<DailyForecast> {
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val dateFormat = SimpleDateFormat("MMM d", Locale.US)
        val conditions = listOf(
            WeatherCondition.CLOUDY,
            WeatherCondition.RAINY,
            WeatherCondition.SUNNY,
            WeatherCondition.PARTLY_CLOUDY,
            WeatherCondition.SUNNY,
            WeatherCondition.CLOUDY,
            WeatherCondition.RAINY
        )
        val chanceCurve = listOf(35, 78, 10, 28, 12, 42, 70)
        val amountCurve = listOf(0.05f, 0.28f, 0f, 0.04f, 0f, 0.08f, 0.22f)
        val uvCurve = listOf(4.2f, 2.1f, 8.5f, 6.0f, 9.0f, 3.5f, 1.8f)
        val humCurve = listOf(72, 88, 45, 55, 40, 68, 82)

        return (0 until 7).map { idx ->
            val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, idx) }
            val dayName = if (idx == 0) "TODAY" else dayFormat.format(dayCal.time).uppercase()
            val dateLabel = dateFormat.format(dayCal.time).uppercase()
            val maxC = baseTempC + 3f + (idx % 3)
            val minC = baseTempC - 5f + (idx % 2)
            DailyForecast(
                dayName = dayName,
                dateLabel = dateLabel,
                condition = conditions[idx],
                maxTempC = maxC,
                maxTempF = celsiusToFahrenheit(maxC),
                minTempC = minC,
                minTempF = celsiusToFahrenheit(minC),
                precipChancePercent = chanceCurve[idx],
                precipAmountInches = amountCurve[idx],
                uvIndexMax = uvCurve[idx],
                humidityPercent = humCurve[idx]
            )
        }
    }
}
