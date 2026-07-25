package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyForecast
import com.example.data.model.HourlyForecast
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherForecastData
import com.example.ui.components.BlueWaveGraphCanvas
import com.example.ui.components.LiveWidgetsView
import com.example.ui.components.MoonPhaseView
import com.example.ui.components.SevereWeatherAlertView
import com.example.ui.components.ThreeDIslandCanvas
import com.example.ui.components.ThreeDWeatherCanvas
import com.example.ui.components.UvMeterCanvas
import com.example.ui.components.WeeklyPrecipGraphCanvas
import com.example.ui.components.WindCompassCanvas
import com.example.ui.components.WeatherFooter
import com.example.ui.components.formatPrecipInches
import com.example.ui.viewmodel.TemperatureUnit
import com.example.ui.viewmodel.WidgetType
import com.example.util.rememberDropletFeedback
import kotlin.math.roundToInt

@Composable
fun DetailedForecastScreen(
    data: WeatherForecastData,
    selectedHourIndex: Int,
    temperatureUnit: TemperatureUnit,
    pinnedWidgets: List<WidgetType> = WidgetType.values().toList(),
    onToggleWidgetPin: (WidgetType) -> Unit = {},
    onHourSelected: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0=PRECIP ... 6=7-DAY
    var isWeekMode by remember { mutableStateOf(false) }
    var selectedDayIndex by remember { mutableIntStateOf(0) }

    val (playFeedback, _) = rememberDropletFeedback()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF2F2F7)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Get more detailed",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = "forecasts",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = "${data.cityName.uppercase()} · ${data.country.uppercase()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                IconButton(
                    onClick = {
                        playFeedback()
                        onClose()
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFE5E5EA))
                        .testTag("close_details_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = Color(0xFF1C1C1E)
                    )
                }
            }

            // Tab chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    "PRECIP" to "tab_precip",
                    "WIDGETS" to "tab_widgets",
                    "MOON" to "tab_moon",
                    "ALERTS" to "tab_alerts",
                    "WIND" to "tab_wind",
                    "UV & AIR" to "tab_uv_air",
                    "7-DAY" to "tab_7day"
                )
                tabs.forEachIndexed { idx, (label, tag) ->
                    val isSel = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSel) Color(0xFF1C1C1E) else Color(0xFFE5E5EA))
                            .clickable {
                                playFeedback()
                                selectedTab = idx
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag(tag),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSel) Color.White else Color(0xFF8E8E93)
                        )
                    }
                }
            }

            // ── TAB 0: PRECIPITATION ──────────────────────────────────────
            if (selectedTab == 0) {
                PrecipTabContent(
                    data = data,
                    selectedHourIndex = selectedHourIndex,
                    selectedDayIndex = selectedDayIndex.coerceIn(0, data.dailyList.lastIndex.coerceAtLeast(0)),
                    isWeekMode = isWeekMode,
                    onHourSelected = onHourSelected,
                    onDaySelected = { selectedDayIndex = it },
                    onWeekModeChange = { isWeekMode = it },
                    playFeedback = playFeedback
                )
            }

            // ── TAB 1: WIDGETS ────────────────────────────────────────────
            if (selectedTab == 1) {
                LiveWidgetsView(
                    data = data,
                    pinnedWidgets = pinnedWidgets,
                    onToggleWidgetPin = onToggleWidgetPin
                )
            }

            // ── TAB 2: MOON ───────────────────────────────────────────────
            if (selectedTab == 2) {
                MoonPhaseView(data = data)
            }

            // ── TAB 3: ALERTS ─────────────────────────────────────────────
            if (selectedTab == 3) {
                SevereWeatherAlertView(data = data)
            }

            // ── TAB 4: WIND ───────────────────────────────────────────────
            if (selectedTab == 4) {
                WindTabContent(
                    data = data,
                    selectedHourIndex = selectedHourIndex,
                    isWeekMode = isWeekMode,
                    onWeekModeChange = { isWeekMode = it },
                    playFeedback = playFeedback
                )
            }

            // ── TAB 5: UV & AIR ───────────────────────────────────────────
            if (selectedTab == 5) {
                UvAirTabContent(
                    data = data,
                    selectedHourIndex = selectedHourIndex,
                    isWeekMode = isWeekMode,
                    onWeekModeChange = { isWeekMode = it },
                    playFeedback = playFeedback
                )
            }

            // ── TAB 6: 7-DAY ──────────────────────────────────────────────
            if (selectedTab == 6) {
                SevenDayTabContent(
                    data = data,
                    temperatureUnit = temperatureUnit,
                    selectedDayIndex = selectedDayIndex.coerceIn(0, data.dailyList.lastIndex.coerceAtLeast(0)),
                    onDaySelected = {
                        playFeedback()
                        selectedDayIndex = it
                    }
                )
            }

            WeatherFooter()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PRECIP TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PrecipTabContent(
    data: WeatherForecastData,
    selectedHourIndex: Int,
    selectedDayIndex: Int,
    isWeekMode: Boolean,
    onHourSelected: (Int) -> Unit,
    onDaySelected: (Int) -> Unit,
    onWeekModeChange: (Boolean) -> Unit,
    playFeedback: () -> Unit
) {
    val currentHourly = data.hourlyList.getOrNull(selectedHourIndex)
        ?: data.hourlyList.firstOrNull()
    val currentDaily = data.dailyList.getOrNull(selectedDayIndex)
        ?: data.dailyList.firstOrNull()

    val displayRate = if (isWeekMode) {
        currentDaily?.precipAmountInches ?: 0f
    } else {
        currentHourly?.precipRateInches ?: data.precipRateInches
    }
    val displayChance = if (isWeekMode) {
        currentDaily?.precipChancePercent ?: 0
    } else {
        currentHourly?.precipChancePercent ?: data.precipChancePercent
    }
    val displayCondition = if (isWeekMode) {
        currentDaily?.condition ?: data.condition
    } else {
        currentHourly?.condition ?: data.condition
    }
    val intensityLabel = precipIntensityLabel(displayRate, isWeekMode)
    val nextRainLabel = if (isWeekMode) {
        nextRainyDayLabel(data.dailyList, selectedDayIndex)
    } else {
        nextRainHourLabel(data.hourlyList, selectedHourIndex)
    }

    // Hero 3D island
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFE8F4FF), Color(0xFFF2F2F7))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        ThreeDIslandCanvas(
            precipRateInches = displayRate.coerceAtLeast(0.02f),
            modifier = Modifier.fillMaxSize()
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Summary chip row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InfoChip(
            label = if (isWeekMode) "DAY TOTAL" else "RATE",
            value = if (isWeekMode) formatPrecipInches(displayRate) else "${displayRate} IN/HR",
            accent = Color(0xFF007AFF)
        )
        InfoChip(
            label = "CHANCE",
            value = "$displayChance%",
            accent = Color(0xFF30B0C7)
        )
        InfoChip(
            label = "INTENSITY",
            value = intensityLabel,
            accent = Color(0xFF5856D6)
        )
        InfoChip(
            label = if (isWeekMode) "NEXT RAIN" else "NEXT",
            value = nextRainLabel,
            accent = Color(0xFFFF9500)
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Detailed metrics card
    MetricCard {
        MetricRow(
            label = if (isWeekMode) "ACCUMULATION" else "RATE",
            value = if (isWeekMode) formatPrecipInches(displayRate) else "${displayRate} IN",
            valueColor = Color.White
        )
        MetricDivider()
        MetricRow(label = "CHANCE", value = "$displayChance%", valueColor = Color.White)
        MetricDivider()
        MetricRow(
            label = "TYPE",
            value = displayCondition.label,
            valueColor = Color(0xFF30B0C7)
        )
        MetricDivider()
        MetricRow(
            label = "INTENSITY",
            value = intensityLabel,
            valueColor = Color(0xFF64D2FF)
        )
        MetricDivider()
        MetricRow(
            label = if (isWeekMode) "LOOKING AHEAD" else "NEXT PRECIP",
            value = nextRainLabel,
            valueColor = Color(0xFFFFB300)
        )
        if (!isWeekMode && currentHourly != null) {
            MetricDivider()
            MetricRow(
                label = "HUMIDITY",
                value = "${currentHourly.humidityPercent}%",
                valueColor = Color.White
            )
            MetricDivider()
            MetricRow(
                label = "TIME",
                value = currentHourly.timeLabel,
                valueColor = Color(0xFF8E8E93)
            )
        }
        if (isWeekMode && currentDaily != null) {
            MetricDivider()
            MetricRow(
                label = "DAY",
                value = "${currentDaily.dayName} · ${currentDaily.dateLabel}",
                valueColor = Color(0xFF8E8E93)
            )
            MetricDivider()
            MetricRow(
                label = "HIGH / LOW",
                value = "${currentDaily.maxTempF.roundToInt()}° / ${currentDaily.minTempF.roundToInt()}°",
                valueColor = Color.White
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Graph switches between day (hourly wave) and week (daily bars)
    AnimatedContent(
        targetState = isWeekMode,
        transitionSpec = {
            (fadeIn() + slideInHorizontally { if (targetState) it / 4 else -it / 4 }) togetherWith
                (fadeOut() + slideOutHorizontally { if (targetState) -it / 4 else it / 4 })
        },
        label = "precipGraphMode"
    ) { weekMode ->
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (weekMode) "7-DAY PRECIPITATION" else "HOURLY PRECIPITATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (weekMode) {
                WeeklyPrecipGraphCanvas(
                    dailyList = data.dailyList,
                    selectedIndex = selectedDayIndex,
                    onIndexSelected = {
                        playFeedback()
                        onDaySelected(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("weekly_precip_graph")
                )
            } else {
                BlueWaveGraphCanvas(
                    hourlyList = data.hourlyList,
                    selectedIndex = selectedHourIndex,
                    onIndexSelected = {
                        playFeedback()
                        onHourSelected(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            }
        }
    }

    DayWeekToggle(
        isWeekMode = isWeekMode,
        onChange = {
            playFeedback()
            onWeekModeChange(it)
        },
        modifier = Modifier.padding(top = 16.dp)
    )

    // Hourly / daily breakdown list
    Spacer(modifier = Modifier.height(18.dp))
    Text(
        text = if (isWeekMode) "WEEKLY BREAKDOWN" else "HOURLY BREAKDOWN",
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
        color = Color(0xFF8E8E93),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )

    if (isWeekMode) {
        data.dailyList.forEachIndexed { idx, day ->
            PrecipDayRow(
                day = day,
                isSelected = idx == selectedDayIndex,
                onClick = {
                    playFeedback()
                    onDaySelected(idx)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    } else {
        data.hourlyList.take(12).forEachIndexed { idx, hour ->
            PrecipHourRow(
                hour = hour,
                isSelected = idx == selectedHourIndex,
                onClick = {
                    playFeedback()
                    onHourSelected(idx)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PrecipHourRow(
    hour: HourlyForecast,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Color(0xFF1C1C1E) else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = hour.timeLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (isSelected) Color.White else Color(0xFF1C1C1E)
            )
            Text(
                text = hour.condition.label,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isSelected) Color(0xFF8E8E93) else Color(0xFF8E8E93)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${hour.precipChancePercent}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSelected) Color(0xFF64D2FF) else Color(0xFF007AFF)
                )
                Text(
                    text = "CHANCE",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF8E8E93)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${hour.precipRateInches} IN",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSelected) Color.White else Color(0xFF1C1C1E)
                )
                Text(
                    text = "RATE",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    }
}

@Composable
private fun PrecipDayRow(
    day: DailyForecast,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Color(0xFF1C1C1E) else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = day.dayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (isSelected) Color.White else Color(0xFF1C1C1E)
            )
            Text(
                text = "${day.dateLabel} · ${day.condition.label}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF8E8E93)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${day.precipChancePercent}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSelected) Color(0xFF64D2FF) else Color(0xFF007AFF)
                )
                Text(
                    text = "CHANCE",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF8E8E93)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatPrecipInches(day.precipAmountInches),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSelected) Color.White else Color(0xFF1C1C1E)
                )
                Text(
                    text = "TOTAL",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WIND TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WindTabContent(
    data: WeatherForecastData,
    selectedHourIndex: Int,
    isWeekMode: Boolean,
    onWeekModeChange: (Boolean) -> Unit,
    playFeedback: () -> Unit
) {
    val currentHourly = data.hourlyList.getOrNull(selectedHourIndex)
        ?: data.hourlyList.firstOrNull()
    val speed = currentHourly?.windSpeedMph ?: data.windSpeedMph
    val degrees = currentHourly?.windDirectionDegrees ?: data.windDirectionDegrees
    val directionLabel = degreesToCompass(degrees)
    val beaufort = beaufortLabel(speed)
    val avgWeekSpeed = data.hourlyList.map { it.windSpeedMph }.average().toFloat()
    val maxWeekSpeed = data.hourlyList.maxOfOrNull { it.windSpeedMph } ?: speed

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFFE8F4FF))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        WindCompassCanvas(
            degrees = degrees,
            speedMph = speed,
            modifier = Modifier.fillMaxSize()
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InfoChip(label = "SPEED", value = "${speed} MPH", accent = Color(0xFF007AFF))
        InfoChip(label = "DIR", value = "$directionLabel $degrees°", accent = Color(0xFF5856D6))
        InfoChip(label = "FORCE", value = beaufort, accent = Color(0xFFFF9500))
        InfoChip(label = "GUST MAX", value = "${maxWeekSpeed.roundToInt()} MPH", accent = Color(0xFFFF3B30))
    }

    Spacer(modifier = Modifier.height(14.dp))

    MetricCard {
        MetricRow(label = "SPEED", value = "$speed MPH", valueColor = Color.White)
        MetricDivider()
        MetricRow(label = "DIRECTION", value = "$directionLabel ($degrees°)", valueColor = Color.White)
        MetricDivider()
        MetricRow(label = "BEAUFORT", value = beaufort, valueColor = Color(0xFFFFB300))
        MetricDivider()
        MetricRow(
            label = "PERIOD AVG",
            value = "${((avgWeekSpeed * 10).roundToInt() / 10f)} MPH",
            valueColor = Color(0xFF64D2FF)
        )
        MetricDivider()
        MetricRow(
            label = "PEAK GUST",
            value = "${maxWeekSpeed.roundToInt()} MPH",
            valueColor = Color(0xFFFF453A)
        )
        MetricDivider()
        MetricRow(
            label = "FEELS LIKE",
            value = windFeelLabel(speed),
            valueColor = Color(0xFF30B0C7)
        )
    }

    if (isWeekMode) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "WIND BY HOUR",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            color = Color(0xFF8E8E93),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        data.hourlyList.take(8).forEach { hour ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = hour.timeLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF1C1C1E)
                )
                Text(
                    text = "${hour.windSpeedMph} MPH · ${degreesToCompass(hour.windDirectionDegrees)}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF007AFF)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }

    DayWeekToggle(
        isWeekMode = isWeekMode,
        onChange = {
            playFeedback()
            onWeekModeChange(it)
        },
        modifier = Modifier.padding(top = 16.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// UV & AIR TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UvAirTabContent(
    data: WeatherForecastData,
    selectedHourIndex: Int,
    isWeekMode: Boolean,
    onWeekModeChange: (Boolean) -> Unit,
    playFeedback: () -> Unit
) {
    val currentHourly = data.hourlyList.getOrNull(selectedHourIndex)
        ?: data.hourlyList.firstOrNull()
    val uv = currentHourly?.uvIndex ?: data.uvIndex
    val humidity = currentHourly?.humidityPercent ?: data.humidityPercent
    val aqi = data.airQualityIndex
    val uvLabel = uvRiskLabel(uv)
    val aqiLabel = aqiRiskLabel(aqi)
    val maxUv = data.hourlyList.maxOfOrNull { it.uvIndex } ?: uv

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        UvMeterCanvas(
            uvIndex = uv,
            modifier = Modifier.fillMaxSize()
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InfoChip(label = "UV", value = "${uv.roundToInt()} · $uvLabel", accent = uvColor(uv))
        InfoChip(label = "AQI", value = "$aqi · $aqiLabel", accent = aqiColor(aqi))
        InfoChip(label = "HUMIDITY", value = "$humidity%", accent = Color(0xFF30B0C7))
        InfoChip(label = "PEAK UV", value = maxUv.roundToInt().toString(), accent = Color(0xFFFF3B30))
    }

    Spacer(modifier = Modifier.height(14.dp))

    MetricCard {
        MetricRow(
            label = "UV INDEX",
            value = "${uv.roundToInt()} ($uvLabel)",
            valueColor = uvColor(uv)
        )
        MetricDivider()
        MetricRow(
            label = "AIR QUALITY",
            value = "$aqi AQI ($aqiLabel)",
            valueColor = aqiColor(aqi)
        )
        MetricDivider()
        MetricRow(label = "HUMIDITY", value = "$humidity%", valueColor = Color.White)
        MetricDivider()
        MetricRow(
            label = "PEAK UV TODAY",
            value = maxUv.roundToInt().toString(),
            valueColor = Color(0xFFFF9500)
        )
        MetricDivider()
        MetricRow(
            label = "SUN PROTECTION",
            value = sunProtectionAdvice(uv),
            valueColor = Color(0xFF64D2FF)
        )
        MetricDivider()
        MetricRow(
            label = "AIR ADVICE",
            value = airAdvice(aqi),
            valueColor = Color(0xFF30B0C7)
        )
    }

    if (isWeekMode) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "UV BY HOUR",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            color = Color(0xFF8E8E93),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        data.hourlyList.take(8).forEach { hour ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = hour.timeLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF1C1C1E)
                )
                Text(
                    text = "UV ${hour.uvIndex.roundToInt()} · ${uvRiskLabel(hour.uvIndex)} · ${hour.humidityPercent}% RH",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = uvColor(hour.uvIndex)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }

    DayWeekToggle(
        isWeekMode = isWeekMode,
        onChange = {
            playFeedback()
            onWeekModeChange(it)
        },
        modifier = Modifier.padding(top = 16.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 7-DAY TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SevenDayTabContent(
    data: WeatherForecastData,
    temperatureUnit: TemperatureUnit,
    selectedDayIndex: Int,
    onDaySelected: (Int) -> Unit
) {
    val selected = data.dailyList.getOrNull(selectedDayIndex)
        ?: data.dailyList.firstOrNull()
        ?: return

    // Selected day hero card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1C1C1E), Color(0xFF2C2C2E))
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selected.dayName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.White
                    )
                    Text(
                        text = selected.dateLabel,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF8E8E93)
                    )
                }
                Box(modifier = Modifier.size(72.dp)) {
                    ThreeDWeatherCanvas(
                        condition = selected.condition,
                        isDaytime = true,
                        modelScale = 0.7f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = selected.condition.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF64D2FF)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val high = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) {
                    selected.maxTempF.roundToInt()
                } else {
                    selected.maxTempC.roundToInt()
                }
                val low = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) {
                    selected.minTempF.roundToInt()
                } else {
                    selected.minTempC.roundToInt()
                }
                Column {
                    Text("HIGH", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                    Text("${high}°", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                Column {
                    Text("LOW", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                    Text("${low}°", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF8E8E93))
                }
                Column {
                    Text("RAIN", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                    Text("${selected.precipChancePercent}%", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF30B0C7))
                }
                Column {
                    Text("TOTAL", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                    Text(
                        formatPrecipInches(selected.precipAmountInches),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF007AFF),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Full week stack with rich rows
    data.dailyList.forEachIndexed { idx, daily ->
        val maxTemp = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) {
            daily.maxTempF.roundToInt()
        } else {
            daily.maxTempC.roundToInt()
        }
        val minTemp = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) {
            daily.minTempF.roundToInt()
        } else {
            daily.minTempC.roundToInt()
        }
        val isSel = idx == selectedDayIndex

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(if (isSel) Color(0xFF1C1C1E) else Color.White)
                .border(
                    width = if (isSel) 0.dp else 1.dp,
                    color = Color(0xFFE5E5EA),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable { onDaySelected(idx) }
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag("day_row_$idx"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(72.dp)) {
                Text(
                    text = daily.dayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSel) Color.White else Color(0xFF1C1C1E)
                )
                Text(
                    text = daily.dateLabel,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF8E8E93)
                )
            }

            // Condition + rain chance
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = conditionEmoji(daily.condition),
                    fontSize = 18.sp
                )
                Text(
                    text = daily.condition.label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSel) Color(0xFF8E8E93) else Color(0xFF8E8E93)
                )
            }

            // Precip badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.width(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = if (daily.precipChancePercent > 40) Color(0xFF007AFF) else Color(0xFF8E8E93),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "${daily.precipChancePercent}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSel) Color(0xFF64D2FF) else Color(0xFF007AFF)
                )
            }

            // Temps
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${maxTemp}°",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSel) Color.White else Color(0xFF1C1C1E)
                )
                Text(
                    text = "${minTemp}°",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF8E8E93)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED UI ATOMS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DayWeekToggle(
    isWeekMode: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE5E5EA))
                .padding(4.dp)
                .testTag("day_week_toggle"),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (!isWeekMode) Color(0xFF1C1C1E) else Color.Transparent)
                    .clickable { onChange(false) }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("day_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (!isWeekMode) Color.White else Color(0xFF8E8E93)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isWeekMode) Color(0xFF1C1C1E) else Color.Transparent)
                    .clickable { onChange(true) }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("week_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "WEEK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isWeekMode) Color.White else Color(0xFF8E8E93)
                )
            }
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    value: String,
    accent: Color
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E5EA), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF8E8E93)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = accent,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun MetricCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C1C1E))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        content()
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF8E8E93)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = valueColor
        )
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF2C2C2E))
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────

private fun precipIntensityLabel(rateOrAmount: Float, isWeek: Boolean): String {
    return if (isWeek) {
        when {
            rateOrAmount < 0.01f -> "DRY"
            rateOrAmount < 0.1f -> "LIGHT"
            rateOrAmount < 0.3f -> "MODERATE"
            rateOrAmount < 0.75f -> "HEAVY"
            else -> "EXTREME"
        }
    } else {
        when {
            rateOrAmount < 0.01f -> "NONE"
            rateOrAmount < 0.05f -> "DRIZZLE"
            rateOrAmount < 0.15f -> "LIGHT"
            rateOrAmount < 0.35f -> "MODERATE"
            else -> "HEAVY"
        }
    }
}

private fun nextRainHourLabel(hours: List<HourlyForecast>, fromIndex: Int): String {
    val upcoming = hours.drop(fromIndex + 1).firstOrNull { it.precipChancePercent >= 40 }
    return upcoming?.timeLabel ?: "NONE SOON"
}

private fun nextRainyDayLabel(days: List<DailyForecast>, fromIndex: Int): String {
    val upcoming = days.drop(fromIndex + 1).firstOrNull { it.precipChancePercent >= 40 }
    return upcoming?.dayName ?: "CLEAR AHEAD"
}

private fun degreesToCompass(degrees: Int): String {
    val dirs = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val idx = ((degrees % 360) / 22.5f).roundToInt() % 16
    return dirs[idx]
}

private fun beaufortLabel(mph: Float): String {
    return when {
        mph < 1f -> "0 CALM"
        mph < 4f -> "1 LIGHT"
        mph < 8f -> "2 BREEZE"
        mph < 13f -> "3 GENTLE"
        mph < 19f -> "4 MODERATE"
        mph < 25f -> "5 FRESH"
        mph < 32f -> "6 STRONG"
        mph < 39f -> "7 NEAR GALE"
        else -> "8+ GALE"
    }
}

private fun windFeelLabel(mph: Float): String {
    return when {
        mph < 5f -> "STILL"
        mph < 12f -> "PLEASANT"
        mph < 20f -> "BREEZY"
        mph < 30f -> "WINDY"
        else -> "BLUSTERY"
    }
}

private fun uvRiskLabel(uv: Float): String {
    return when {
        uv < 3f -> "LOW"
        uv < 6f -> "MODERATE"
        uv < 8f -> "HIGH"
        uv < 11f -> "VERY HIGH"
        else -> "EXTREME"
    }
}

private fun uvColor(uv: Float): Color {
    return when {
        uv < 3f -> Color(0xFF34C759)
        uv < 6f -> Color(0xFFFFCC00)
        uv < 8f -> Color(0xFFFF9500)
        uv < 11f -> Color(0xFFFF3B30)
        else -> Color(0xFFAF52DE)
    }
}

private fun aqiRiskLabel(aqi: Int): String {
    return when {
        aqi <= 50 -> "GOOD"
        aqi <= 100 -> "MODERATE"
        aqi <= 150 -> "UNHEALTHY*"
        aqi <= 200 -> "UNHEALTHY"
        aqi <= 300 -> "VERY BAD"
        else -> "HAZARDOUS"
    }
}

private fun aqiColor(aqi: Int): Color {
    return when {
        aqi <= 50 -> Color(0xFF34C759)
        aqi <= 100 -> Color(0xFFFFCC00)
        aqi <= 150 -> Color(0xFFFF9500)
        aqi <= 200 -> Color(0xFFFF3B30)
        else -> Color(0xFFAF52DE)
    }
}

private fun sunProtectionAdvice(uv: Float): String {
    return when {
        uv < 3f -> "MINIMAL NEEDED"
        uv < 6f -> "SPF 15+ · HAT"
        uv < 8f -> "SPF 30+ · SHADE"
        uv < 11f -> "SPF 50 · AVOID MIDDAY"
        else -> "STAY INDOORS"
    }
}

private fun airAdvice(aqi: Int): String {
    return when {
        aqi <= 50 -> "GREAT OUTDOORS"
        aqi <= 100 -> "OK FOR MOST"
        aqi <= 150 -> "LIMIT EXERCISE"
        else -> "STAY INDOORS"
    }
}

private fun conditionEmoji(condition: WeatherCondition): String {
    return when (condition) {
        WeatherCondition.SUNNY, WeatherCondition.CLEAR -> "☀️"
        WeatherCondition.PARTLY_CLOUDY -> "⛅"
        WeatherCondition.CLOUDY, WeatherCondition.MOSTLY_CLOUDY -> "☁️"
        WeatherCondition.RAINY, WeatherCondition.HEAVY_RAIN -> "🌧️"
        WeatherCondition.THUNDERSTORM -> "⛈️"
        WeatherCondition.SNOWY -> "❄️"
        WeatherCondition.HAZE -> "🌫️"
        WeatherCondition.WINDY -> "💨"
    }
}
