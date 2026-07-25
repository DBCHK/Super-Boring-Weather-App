package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.repository.WeatherRepository
import com.example.widget.providers.AirQualityWidgetProvider
import com.example.widget.providers.CompactRainWidgetProvider
import com.example.widget.providers.MoonPhaseWidgetProvider
import com.example.widget.providers.RedSunWidgetProvider
import com.example.widget.providers.ThreeDayWidgetProvider
import com.example.widget.providers.WideRainWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetUpdateHelper {

    val providerClasses: List<Class<*>> = listOf(
        RedSunWidgetProvider::class.java,
        ThreeDayWidgetProvider::class.java,
        WideRainWidgetProvider::class.java,
        AirQualityWidgetProvider::class.java,
        MoonPhaseWidgetProvider::class.java,
        CompactRainWidgetProvider::class.java
    )

    /**
     * Fetch live weather for the preferred city, cache snapshot, refresh every widget.
     */
    suspend fun refreshAll(context: Context) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val snap = try {
            val db = AppDatabase.getDatabase(app)
            val repo = WeatherRepository(db.cityDao())
            val city = db.cityDao().getPreferredCity()
                ?: db.cityDao().getDefaultCity()
                ?: repo.defaultCities.first()
            val data = repo.fetchWeather(city)
            WidgetSnapshot.from(data)
        } catch (_: Exception) {
            // Keep previous cache if network fails
            WidgetSnapshotStore.load(app).takeIf { it.updatedAtMs > 0 }
                ?: WidgetSnapshot(
                    cityName = "SEATTLE",
                    tempC = 18,
                    tempF = 65,
                    highC = 20,
                    highF = 68,
                    lowC = 12,
                    lowF = 54,
                    conditionLabel = "LIGHT RAIN",
                    conditionKey = "RAINY",
                    aqi = 59,
                    aqiLabel = "GOOD",
                    moonPhase = "WANING CRESCENT",
                    moonIllum = 25,
                    day0Name = "TODAY",
                    day0High = 19,
                    day0Low = 7,
                    day0Icon = "sun",
                    day1Name = "SAT",
                    day1High = 19,
                    day1Low = 7,
                    day1Icon = "sun",
                    day2Name = "SAT",
                    day2High = 19,
                    day2Low = 7,
                    day2Icon = "sun",
                    updatedAtMs = System.currentTimeMillis()
                )
        }
        WidgetSnapshotStore.save(app, snap)
        pushToAllWidgets(app, snap)
    }

    fun pushCached(context: Context) {
        val app = context.applicationContext
        val snap = WidgetSnapshotStore.load(app)
        if (snap.updatedAtMs > 0) {
            pushToAllWidgets(app, snap)
        }
    }

    /** Call from the main app after a successful weather fetch. */
    fun publishFromApp(context: Context, snap: WidgetSnapshot) {
        WidgetSnapshotStore.save(context.applicationContext, snap)
        pushToAllWidgets(context.applicationContext, snap)
    }

    private fun pushToAllWidgets(context: Context, snap: WidgetSnapshot) {
        val manager = AppWidgetManager.getInstance(context)
        providerClasses.forEach { clazz ->
            val ids = manager.getAppWidgetIds(ComponentName(context, clazz))
            if (ids.isNotEmpty()) {
                ids.forEach { id -> bindProvider(context, manager, clazz, id, snap) }
            }
        }
    }

    fun bindProvider(
        context: Context,
        manager: AppWidgetManager,
        clazz: Class<*>,
        appWidgetId: Int,
        snap: WidgetSnapshot
    ) {
        val views = when (clazz) {
            RedSunWidgetProvider::class.java -> bindRedSun(context, snap)
            ThreeDayWidgetProvider::class.java -> bindThreeDay(context, snap)
            WideRainWidgetProvider::class.java -> bindWideRain(context, snap)
            AirQualityWidgetProvider::class.java -> bindAirQuality(context, snap)
            MoonPhaseWidgetProvider::class.java -> bindMoon(context, snap)
            CompactRainWidgetProvider::class.java -> bindCompactRain(context, snap)
            else -> return
        }
        manager.updateAppWidget(appWidgetId, views)
    }

    private fun openAppIntent(context: Context): android.app.PendingIntent {
        val intent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun bindRedSun(context: Context, snap: WidgetSnapshot): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_red_sun).apply {
            setTextViewText(R.id.widget_city, snap.cityName)
            setTextViewText(R.id.widget_temp, "${snap.displayTemp()}°")
            setTextViewText(R.id.widget_condition, snap.conditionLabel)
            setTextViewText(
                R.id.widget_hi_lo,
                "H:${snap.displayHigh()}°  L:${snap.displayLow()}°"
            )
            setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        }
    }

    private fun bindThreeDay(context: Context, snap: WidgetSnapshot): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_three_day).apply {
            setTextViewText(R.id.day0_name, snap.day0Name)
            setTextViewText(R.id.day0_high, "${snap.day0High}°")
            setTextViewText(R.id.day0_low, "${snap.day0Low}°")
            setImageViewResource(R.id.day0_icon, iconRes(snap.day0Icon))

            setTextViewText(R.id.day1_name, snap.day1Name)
            setTextViewText(R.id.day1_high, "${snap.day1High}°")
            setTextViewText(R.id.day1_low, "${snap.day1Low}°")
            setImageViewResource(R.id.day1_icon, iconRes(snap.day1Icon))

            setTextViewText(R.id.day2_name, snap.day2Name)
            setTextViewText(R.id.day2_high, "${snap.day2High}°")
            setTextViewText(R.id.day2_low, "${snap.day2Low}°")
            setImageViewResource(R.id.day2_icon, iconRes(snap.day2Icon))

            setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        }
    }

    private fun bindWideRain(context: Context, snap: WidgetSnapshot): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_wide_rain).apply {
            setTextViewText(R.id.widget_temp, "${snap.displayTemp()}°")
            setTextViewText(R.id.widget_city, snap.cityName)
            setTextViewText(R.id.widget_condition, snap.conditionLabel)
            setTextViewText(R.id.widget_high, "${snap.displayHigh()}°")
            setTextViewText(R.id.widget_low, "${snap.displayLow()}°")
            setTextViewText(R.id.widget_meta, "AQI ${snap.aqi} · ${snap.aqiLabel}")
            setImageViewResource(R.id.widget_weather_icon, conditionIcon(snap.conditionKey))
            setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        }
    }

    private fun bindAirQuality(context: Context, snap: WidgetSnapshot): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_air_quality).apply {
            setTextViewText(R.id.widget_aqi_value, "AQI:${snap.aqi}")
            setTextViewText(R.id.widget_aqi_label, snap.aqiLabel)
            setTextViewText(R.id.widget_city, snap.cityName)
            setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        }
    }

    private fun bindMoon(context: Context, snap: WidgetSnapshot): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_moon_phase).apply {
            setTextViewText(R.id.widget_moon_phase, snap.moonPhase)
            setTextViewText(R.id.widget_moon_illum, "${snap.moonIllum}%")
            setTextViewText(R.id.widget_city, snap.cityName)
            setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        }
    }

    private fun bindCompactRain(context: Context, snap: WidgetSnapshot): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_compact_rain).apply {
            setTextViewText(R.id.widget_temp, "${snap.displayTemp()}°")
            setTextViewText(R.id.widget_city, snap.cityName)
            setTextViewText(R.id.widget_condition, snap.conditionLabel)
            setTextViewText(
                R.id.widget_hi_lo,
                "H ${snap.displayHigh()}° · L ${snap.displayLow()}°"
            )
            setImageViewResource(R.id.widget_weather_icon, conditionIcon(snap.conditionKey))
            setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        }
    }

    private fun iconRes(key: String): Int = when (key) {
        "sun" -> R.drawable.widget_ic_sun
        "rain" -> R.drawable.widget_ic_rain_cloud
        "snow" -> R.drawable.widget_ic_snow
        else -> R.drawable.widget_ic_cloud
    }

    private fun conditionIcon(key: String): Int = when (key) {
        "SUNNY", "CLEAR" -> R.drawable.widget_ic_sun
        "RAINY", "HEAVY_RAIN", "THUNDERSTORM" -> R.drawable.widget_ic_rain_cloud
        "SNOWY" -> R.drawable.widget_ic_snow
        else -> R.drawable.widget_ic_cloud
    }
}
