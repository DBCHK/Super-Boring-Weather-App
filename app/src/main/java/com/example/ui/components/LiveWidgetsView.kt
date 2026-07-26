package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherForecastData
import com.example.ui.theme.LocalThemePalette
import com.example.ui.theme.NbColors
import com.example.ui.viewmodel.TemperatureUnit
import com.example.ui.viewmodel.WidgetType
import com.example.util.MoonPhaseCalculator
import com.example.util.rememberDropletPlayers
import kotlin.math.roundToInt

/**
 * In-app Live Widgets showcase — mirrors Not Boring widget marketing cards.
 * Theme-aware; uses real forecast + moon math (no hardcoded 13% / °F-only).
 */
@Composable
fun LiveWidgetsView(
    data: WeatherForecastData,
    pinnedWidgets: List<WidgetType> = WidgetType.entries.toList(),
    onToggleWidgetPin: (WidgetType) -> Unit = {},
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    modifier: Modifier = Modifier
) {
    var showAddWidgetGallery by remember { mutableStateOf(false) }
    val feedback = rememberDropletPlayers()
    val palette = LocalThemePalette.current
    val moon = remember { MoonPhaseCalculator.forDate() }
    val illum = (moon.illumination * 100).roundToInt().coerceIn(0, 100)

    fun displayTemp(c: Float, f: Float): Int =
        if (temperatureUnit == TemperatureUnit.CELSIUS) c.roundToInt() else f.roundToInt()

    val currentTemp = displayTemp(data.currentTempC, data.currentTempF)
    val high = displayTemp(data.highTempC, data.highTempF)
    val low = displayTemp(data.lowTempC, data.lowTempF)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LIVE WIDGETS & MINUTE FORECASTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.0.sp,
                    color = palette.secondaryText
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = NbColors.Cyan,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "REFRESHES EVERY 39 MINS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = NbColors.Cyan
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.chromeBg)
                    .bouncyClick {
                        feedback.plink()
                        showAddWidgetGallery = !showAddWidgetGallery
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("add_widget_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (showAddWidgetGallery) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "Add Widget",
                        tint = palette.chromeFg,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (showAddWidgetGallery) "DONE" else "ADD +",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = palette.chromeFg
                    )
                }
            }
        }

        AnimatedVisibility(visible = showAddWidgetGallery) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "WIDGET GALLERY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = palette.primaryText
                )
                WidgetType.entries.forEach { widget ->
                    val isPinned = pinnedWidgets.contains(widget)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isPinned) palette.chipSelectedBg else palette.chipBg
                            )
                            .bouncyClick {
                                feedback.tick()
                                onToggleWidgetPin(widget)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = widget.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isPinned) palette.chipSelectedFg else palette.primaryText
                            )
                            Text(
                                text = widget.description,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isPinned) {
                                    palette.chipSelectedFg.copy(alpha = 0.7f)
                                } else {
                                    palette.secondaryText
                                }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPinned) NbColors.Cyan else palette.tertiaryText
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = "Pin",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Row 1: Hero orb + 3-day
        if (pinnedWidgets.contains(WidgetType.RED_SUN_ORB) ||
            pinnedWidgets.contains(WidgetType.THREE_DAY_FORECAST)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pinnedWidgets.contains(WidgetType.RED_SUN_ORB)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(156.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(palette.cardDark)
                            .padding(16.dp)
                            .testTag("widget_red_sun"),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Column {
                            Text(
                                text = data.cityName.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = NbColors.Mist
                            )
                            Text(
                                text = "$currentTemp°",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            // Red sun signature — force sunny orb for marketing card
                            ThreeDWeatherCanvas(
                                condition = WeatherCondition.SUNNY,
                                isDaytime = true,
                                modelScale = 1.1f,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                if (pinnedWidgets.contains(WidgetType.THREE_DAY_FORECAST)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(156.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(palette.cardLight)
                            .padding(14.dp)
                            .testTag("widget_3day_forecast"),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            data.dailyList.take(3).forEachIndexed { index, daily ->
                                val hi = displayTemp(daily.maxTempC, daily.maxTempF)
                                val lo = displayTemp(daily.minTempC, daily.minTempF)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (index == 0) "TODAY" else daily.dayName.take(3).uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = NbColors.Mist
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = conditionEmoji(daily.condition), fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$hi°",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = NbColors.Ink
                                    )
                                    Text(
                                        text = "$lo°",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = NbColors.Mist
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Row 2: Wide rain / condition banner
        if (pinnedWidgets.contains(WidgetType.WIDE_RAIN_BANNER)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(palette.cardDark)
                    .padding(18.dp)
                    .testTag("widget_wide_rain_banner")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$currentTemp°",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = data.cityName.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = NbColors.Mist
                            )
                            Text(
                                text = data.condition.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }
                    Box(modifier = Modifier.size(68.dp)) {
                        ThreeDWeatherCanvas(
                            condition = data.condition,
                            isDaytime = true,
                            modelScale = 1.0f,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // High / low capsule bar (blue active high)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(NbColors.InkElevated),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(NbColors.ScrubHandle),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$high°  ◐",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$low°  ◑",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NbColors.PaperMuted
                        )
                    }
                }
            }
        }

        // Row 3: AQI + Moon
        if (pinnedWidgets.contains(WidgetType.AIR_QUALITY) ||
            pinnedWidgets.contains(WidgetType.MOON_PHASE)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pinnedWidgets.contains(WidgetType.AIR_QUALITY)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(148.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(palette.cardLight)
                            .padding(16.dp)
                            .testTag("widget_air_quality"),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "AIR QUALITY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = NbColors.Mist
                                )
                                Text(
                                    text = "AQI:${data.airQualityIndex}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = NbColors.Ink
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            data.airQualityIndex <= 50 -> NbColors.Live
                                            data.airQualityIndex <= 100 -> NbColors.Warning
                                            else -> NbColors.Orange
                                        }
                                    )
                            )
                        }
                        Text(
                            text = when {
                                data.airQualityIndex <= 50 -> "GOOD"
                                data.airQualityIndex <= 100 -> "OKAY"
                                data.airQualityIndex <= 150 -> "MEH"
                                else -> "ROUGH"
                            },
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = NbColors.Ink
                        )
                    }
                }

                if (pinnedWidgets.contains(WidgetType.MOON_PHASE)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(148.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(palette.cardDark)
                            .padding(16.dp)
                            .testTag("widget_moon_phase"),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = moon.phaseName.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = NbColors.Mist,
                                maxLines = 1
                            )
                            Text(
                                text = "$illum%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            ThreeDMoonCanvas(
                                illuminationPercent = illum,
                                phaseName = moon.phaseName,
                                isWaxing = moon.isWaxing,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Compact rain condition card
        if (pinnedWidgets.contains(WidgetType.WIND_COMPASS) ||
            pinnedWidgets.contains(WidgetType.UV_METER)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pinnedWidgets.contains(WidgetType.WIND_COMPASS)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(palette.cardDark)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "WIND",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NbColors.Mist
                        )
                        Text(
                            text = "${data.windSpeedMph.roundToInt()} mph",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.White
                        )
                        Text(
                            text = "${data.windDirectionDegrees}°",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NbColors.CyanGlow
                        )
                    }
                }
                if (pinnedWidgets.contains(WidgetType.UV_METER)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(palette.cardLight)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "UV INDEX",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NbColors.Mist
                        )
                        Text(
                            text = "${data.uvIndex.roundToInt()}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = NbColors.Ink
                        )
                        Text(
                            text = when {
                                data.uvIndex < 3f -> "LOW"
                                data.uvIndex < 6f -> "MODERATE"
                                data.uvIndex < 8f -> "HIGH"
                                else -> "VERY HIGH"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = NbColors.Orange
                        )
                    }
                }
            }
        }
    }
}
