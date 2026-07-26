package com.example.widget.providers

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.example.widget.WidgetRefreshScheduler
import com.example.widget.WidgetSnapshotStore
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Shared widget lifecycle:
 *  - paints from cache immediately
 *  - fetches live weather on update / boot / 30‑min alarm
 *  - keeps AlarmManager schedule alive while any weather widget is installed
 */
abstract class BaseWeatherWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    abstract fun providerClass(): Class<out AppWidgetProvider>

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Immediate paint from last snapshot (no blank frame)
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
        // Ensure 30‑min background refresh is armed
        WidgetRefreshScheduler.schedule(context)
        // Network refresh then rebind all widgets
        scope.launch {
            WidgetUpdateHelper.refreshAll(context)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        when (action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            Intent.ACTION_BOOT_COMPLETED,
            WidgetRefreshScheduler.ACTION_PERIODIC_REFRESH -> {
                // Re-arm after boot / periodic fire (alarms are cleared on reboot)
                if (action == Intent.ACTION_BOOT_COMPLETED ||
                    action == WidgetRefreshScheduler.ACTION_PERIODIC_REFRESH
                ) {
                    WidgetRefreshScheduler.schedule(context)
                }
                scope.launch {
                    WidgetUpdateHelper.refreshAll(context)
                }
            }
        }
    }

    override fun onEnabled(context: Context) {
        WidgetRefreshScheduler.schedule(context)
        scope.launch { WidgetUpdateHelper.refreshAll(context) }
    }

    override fun onDisabled(context: Context) {
        // Stop background refresh only when no weather widgets remain
        if (!WidgetRefreshScheduler.hasAnyWidgets(context)) {
            WidgetRefreshScheduler.cancel(context)
        }
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
