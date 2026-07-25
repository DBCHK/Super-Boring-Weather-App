package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherForecastData
import com.example.ui.components.BlueWaveGraphCanvas
import com.example.ui.components.LiveWidgetsView
import com.example.ui.components.MoonPhaseView
import com.example.ui.components.SevereWeatherAlertView
import com.example.ui.components.ThreeDIslandCanvas
import com.example.ui.components.UvMeterCanvas
import com.example.ui.components.WindCompassCanvas
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0=PRECIP, 1=WIDGETS, 2=MOON, 3=ALERTS, 4=WIND, 5=UV&AIR, 6=7-DAY
    var isWeekMode by remember { mutableStateOf(false) }

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
            // Header Bar
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

            // Tab Selector Chips Scrollable Row (Matching all 5 screenshot feature views)
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

            // TAB 0: PRECIPITATION CARD (Matching Image 1)
            if (selectedTab == 0) {
                val currentHourly = data.hourlyList.getOrNull(selectedHourIndex) ?: data.hourlyList.first()

                // 3D Floating Terrain Island Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ThreeDIslandCanvas(
                        precipRateInches = currentHourly.precipRateInches,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Metrics Table Box (Matching Image 1: RATE, CHANCE, TYPE)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "RATE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                        Text(
                            text = "${currentHourly.precipRateInches} IN",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2E)))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "CHANCE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                        Text(
                            text = "${currentHourly.precipChancePercent}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2E)))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "TYPE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                        Text(
                            text = currentHourly.condition.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF30B0C7)
                        )
                    }
                }

                // Interactive Blue Wave Graph Canvas
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    BlueWaveGraphCanvas(
                        hourlyList = data.hourlyList,
                        selectedIndex = selectedHourIndex,
                        onIndexSelected = { idx ->
                            playFeedback()
                            onHourSelected(idx)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )

                    // DAY / WEEK Toggle Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFE5E5EA))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (!isWeekMode) Color(0xFF1C1C1E) else Color.Transparent)
                                    .clickable {
                                        playFeedback()
                                        isWeekMode = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
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
                                    .clickable {
                                        playFeedback()
                                        isWeekMode = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
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
            }

            // TAB 1: LIVE WIDGETS & MINUTE FORECASTS (Matching Image 2)
            if (selectedTab == 1) {
                LiveWidgetsView(
                    data = data,
                    pinnedWidgets = pinnedWidgets,
                    onToggleWidgetPin = onToggleWidgetPin
                )
            }

            // TAB 2: MOON PHASES & ILLUMINATION (Matching Image 3)
            if (selectedTab == 2) {
                MoonPhaseView(data = data)
            }

            // TAB 3: SEVERE WEATHER ALERTS (Matching Image 4)
            if (selectedTab == 3) {
                SevereWeatherAlertView(data = data)
            }

            // TAB 4: WIND CARD
            if (selectedTab == 4) {
                val currentHourly = data.hourlyList.getOrNull(selectedHourIndex) ?: data.hourlyList.first()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WindCompassCanvas(
                        degrees = currentHourly.windDirectionDegrees,
                        speedMph = currentHourly.windSpeedMph,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "SPEED", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                        Text(
                            text = "${currentHourly.windSpeedMph} MPH",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2E)))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "DIRECTION", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                        Text(
                            text = "${currentHourly.windDirectionDegrees}°",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }
            }

            // TAB 5: UV & AIR QUALITY CARD
            if (selectedTab == 5) {
                val currentHourly = data.hourlyList.getOrNull(selectedHourIndex) ?: data.hourlyList.first()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    UvMeterCanvas(
                        uvIndex = currentHourly.uvIndex,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "UV INDEX", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                        Text(
                            text = "${currentHourly.uvIndex} (MODERATE)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFF9500)
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2E)))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "AIR QUALITY", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                        Text(
                            text = "${data.airQualityIndex} AQI (HEALTHY)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF34C759)
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2E)))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "HUMIDITY", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                        Text(
                            text = "${currentHourly.humidityPercent}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }
            }

            // TAB 6: 7-DAY FORECAST STACK
            if (selectedTab == 6) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    data.dailyList.forEach { daily ->
                        val maxTemp = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) daily.maxTempF.roundToInt() else daily.maxTempC.roundToInt()
                        val minTemp = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) daily.minTempF.roundToInt() else daily.minTempC.roundToInt()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFE5E5EA))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = daily.dayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF1C1C1E)
                                )
                                Text(
                                    text = daily.dateLabel,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF8E8E93)
                                )
                            }

                            Text(
                                text = daily.condition.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF1C1C1E)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${maxTemp}°",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1C1C1E)
                                )
                                Text(
                                    text = "${minTemp}°",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
