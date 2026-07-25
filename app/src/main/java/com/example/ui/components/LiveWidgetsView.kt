package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.ui.viewmodel.WidgetType
import com.example.util.rememberDropletFeedback
import kotlin.math.roundToInt

@Composable
fun LiveWidgetsView(
    data: WeatherForecastData,
    pinnedWidgets: List<WidgetType> = WidgetType.values().toList(),
    onToggleWidgetPin: (WidgetType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddWidgetGallery by remember { mutableStateOf(false) }
    val (playFeedback, _) = rememberDropletFeedback()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header with 39-min Auto Refresh Badge & Add Widget Button
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
                    letterSpacing = 1.2.sp,
                    color = Color(0xFF8E8E93)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "39min Auto Refresh",
                        tint = Color(0xFF30B0C7),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "REFRESHES EVERY 39 MINS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF30B0C7)
                    )
                }
            }

            // ADD WIDGET + Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1C1C1E))
                    .clickable {
                        playFeedback()
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
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (showAddWidgetGallery) "DONE" else "ADD WIDGET +",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }

        // Add Widget Picker Drawer / Gallery
        AnimatedVisibility(visible = showAddWidgetGallery) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFE5E5EA))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "WIDGET GALLERY & CUSTOMIZER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF1C1C1E)
                )

                WidgetType.values().forEach { widget ->
                    val isPinned = pinnedWidgets.contains(widget)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isPinned) Color.White else Color(0xFFD1D1D6))
                            .clickable {
                                playFeedback()
                                onToggleWidgetPin(widget)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = widget.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF1C1C1E)
                            )
                            Text(
                                text = widget.description,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF8E8E93)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isPinned) Color(0xFF30B0C7) else Color(0xFF8E8E93)),
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

        // Row 1: Two Square Widgets Side-by-Side (Hero Red Sun + 3-Day Forecast)
        if (pinnedWidgets.contains(WidgetType.RED_SUN_ORB) || pinnedWidgets.contains(WidgetType.THREE_DAY_FORECAST)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (pinnedWidgets.contains(WidgetType.RED_SUN_ORB)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(160.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF2C2C2E))
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
                                color = Color(0xFF8E8E93)
                            )
                            Text(
                                text = "${data.currentTempF.roundToInt()}°",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }

                        // 3D Red Sun Orb
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            ThreeDWeatherCanvas(
                                condition = WeatherCondition.SUNNY,
                                isDaytime = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                if (pinnedWidgets.contains(WidgetType.THREE_DAY_FORECAST)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(160.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(16.dp)
                            .testTag("widget_3day_forecast"),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val days = data.dailyList.take(3)
                            days.forEach { daily ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = daily.dayName.take(3).uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF8E8E93)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE5E5EA)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = if (daily.condition == WeatherCondition.SUNNY) "☀️" else "☁️", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${daily.maxTempF.roundToInt()}°",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF1C1C1E)
                                    )
                                    Text(
                                        text = "${daily.minTempF.roundToInt()}°",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Row 2: Wide Dark Banner Widget (65 SEATTLE LIGHT RAIN + 3D Cloud + Timeline Bar)
        if (pinnedWidgets.contains(WidgetType.WIDE_RAIN_BANNER)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF2C2C2E))
                    .padding(20.dp)
                    .testTag("widget_wide_rain_banner")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${data.currentTempF.roundToInt()}°",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = data.cityName.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF8E8E93)
                            )
                            Text(
                                text = data.condition.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }

                    // 3D Weather Canvas Cloud
                    Box(modifier = Modifier.size(70.dp)) {
                        ThreeDWeatherCanvas(
                            condition = data.condition,
                            isDaytime = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Capsule Temperature Slider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF3A3A3C)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF00A2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${data.highTempF.roundToInt()}° ◐",
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
                            text = "${data.lowTempF.roundToInt()}° ◑",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFE5E5EA)
                        )
                    }
                }
            }
        }

        // Row 3: Air Quality & Moon Phase Square Widgets
        if (pinnedWidgets.contains(WidgetType.AIR_QUALITY) || pinnedWidgets.contains(WidgetType.MOON_PHASE)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (pinnedWidgets.contains(WidgetType.AIR_QUALITY)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(160.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
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
                                    color = Color(0xFF8E8E93)
                                )
                                Text(
                                    text = "AQI:${data.airQualityIndex}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF1C1C1E)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF30B0C7))
                            )
                        }

                        Text(
                            text = if (data.airQualityIndex <= 50) "EXCELLENT" else "GOOD",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF1C1C1E)
                        )
                    }
                }

                if (pinnedWidgets.contains(WidgetType.MOON_PHASE)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(160.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF2C2C2E))
                            .padding(16.dp)
                            .testTag("widget_moon_phase"),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "WANING CRESCENT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF8E8E93)
                            )
                            Text(
                                text = "13%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }

                        // 3D Mini Moon Graphic
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            ThreeDMoonCanvas(
                                illuminationPercent = 13,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

