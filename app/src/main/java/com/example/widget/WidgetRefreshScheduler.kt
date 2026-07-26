package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * Schedules a ~30 minute inexact alarm so home-screen widgets refresh
 * weather data without the user opening the app.
 *
 * Android may batch this for battery savings, but it does not require
 * the activity to be in the foreground.
 */
object WidgetRefreshScheduler {

    const val ACTION_PERIODIC_REFRESH = "com.example.widget.ACTION_PERIODIC_REFRESH"

    private const val REQUEST_CODE = 44_001
    /** 30 minutes */
    private const val INTERVAL_MS = 30L * 60L * 1000L

    fun schedule(context: Context) {
        val app = context.applicationContext
        if (!hasAnyWidgets(app)) {
            cancel(app)
            return
        }
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(app)
        // Cancel previous then re-arm so we never stack duplicates
        alarmManager.cancel(pending)
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + INTERVAL_MS,
            INTERVAL_MS,
            pending
        )
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(app))
    }

    fun hasAnyWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return WidgetUpdateHelper.providerClasses.any { clazz ->
            manager.getAppWidgetIds(ComponentName(context, clazz)).isNotEmpty()
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        // Deliver to a known widget provider; BaseWeatherWidgetProvider handles the action.
        val intent = Intent(context, com.example.widget.providers.RedSunWidgetProvider::class.java).apply {
            action = ACTION_PERIODIC_REFRESH
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
