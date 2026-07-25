package com.example.widget.providers

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.example.widget.WidgetSnapshotStore
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Shared widget lifecycle: paints from cache immediately, then refreshes weather.
 * System updatePeriodMillis is set to 30 minutes on each provider info XML.
 */
abstract class BaseWeatherWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    abstract fun providerClass(): Class<out AppWidgetProvider>

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val snap = WidgetSnapshotStore.load(context)
        appWidgetIds.forEach { id ->
            WidgetUpdateHelper.bindProvider(
                context,
                appWidgetManager,
                providerClass(),
                id,
                snap
            )
        }
        // Network refresh (also repaints when done)
        scope.launch {
            WidgetUpdateHelper.refreshAll(context)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            intent.action == Intent.ACTION_BOOT_COMPLETED
        ) {
            scope.launch { WidgetUpdateHelper.refreshAll(context) }
        }
    }

    override fun onEnabled(context: Context) {
        scope.launch { WidgetUpdateHelper.refreshAll(context) }
    }
}

class RedSunWidgetProvider : BaseWeatherWidgetProvider() {
    override fun providerClass() = RedSunWidgetProvider::class.java
}

class ThreeDayWidgetProvider : BaseWeatherWidgetProvider() {
    override fun providerClass() = ThreeDayWidgetProvider::class.java
}

class WideRainWidgetProvider : BaseWeatherWidgetProvider() {
    override fun providerClass() = WideRainWidgetProvider::class.java
}

class AirQualityWidgetProvider : BaseWeatherWidgetProvider() {
    override fun providerClass() = AirQualityWidgetProvider::class.java
}

class MoonPhaseWidgetProvider : BaseWeatherWidgetProvider() {
    override fun providerClass() = MoonPhaseWidgetProvider::class.java
}

class CompactRainWidgetProvider : BaseWeatherWidgetProvider() {
    override fun providerClass() = CompactRainWidgetProvider::class.java
}
