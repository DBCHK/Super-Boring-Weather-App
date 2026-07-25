package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherForecastData
import com.example.ui.components.ThreeDTemperatureText
import com.example.ui.components.ThreeDWeatherCanvas
import com.example.ui.components.TimelineScrubber
import com.example.ui.components.WeatherBackgroundShaderCanvas
import com.example.ui.viewmodel.TemperatureUnit
import com.example.ui.viewmodel.WeatherUiState
import com.example.util.PianoSoundManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.remember
import android.view.HapticFeedbackConstants
import kotlin.math.roundToInt

@Composable
fun MainWeatherScreen(
    weatherUiState: WeatherUiState,
    selectedHourIndex: Int,
    temperatureUnit: TemperatureUnit,
    onHourSelected: (Int) -> Unit,
    onToggleUnit: () -> Unit,
    onOpenCitySheet: () -> Unit,
    onOpenDetailsCard: () -> Unit,
    onDetectLocation: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF2F2F7) // Pure Not Boring off-white canvas theme
    ) {
        when (weatherUiState) {
            is WeatherUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1C1C1E),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "FETCHING FORECAST...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            }

            is WeatherUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "UNABLE TO LOAD WEATHER",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFF3B30)
                        )
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF1C1C1E))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            is WeatherUiState.Success -> {
                val data = weatherUiState.data
                val hourly = data.hourlyList
                val currentHourly = hourly.getOrNull(selectedHourIndex) ?: hourly.firstOrNull()

                val displayedTemp = if (currentHourly != null) {
                    if (temperatureUnit == TemperatureUnit.FAHRENHEIT) currentHourly.tempF.roundToInt()
                    else currentHourly.tempC.roundToInt()
                } else {
                    if (temperatureUnit == TemperatureUnit.FAHRENHEIT) data.currentTempF.roundToInt()
                    else data.currentTempC.roundToInt()
                }

                val highTemp = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) data.highTempF.roundToInt() else data.highTempC.roundToInt()
                val lowTemp = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) data.lowTempF.roundToInt() else data.lowTempC.roundToInt()
                val condition = currentHourly?.condition ?: data.condition

                val view = LocalView.current
                val context = view.context.applicationContext
                val soundManager = remember { PianoSoundManager(context) }

                val playFeedback = remember {
                    {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        soundManager.playSubtlePianoNote()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Background Particle Shader Canvas Layer behind giant typography
                    WeatherBackgroundShaderCanvas(
                        condition = condition,
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Navigation Header Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, start = 8.dp, end = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // City Switcher Button & Location Detector
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFFE5E5EA))
                                        .clickable {
                                            playFeedback()
                                            onOpenCitySheet()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .testTag("city_selector_button"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Cities",
                                        tint = Color(0xFF1C1C1E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = data.cityName.uppercase(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF1C1C1E)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        playFeedback()
                                        onDetectLocation()
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE5E5EA))
                                        .testTag("detect_location_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Detect Location",
                                        tint = Color(0xFF1C1C1E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // °F | °C Unit Capsule Button
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFE5E5EA))
                                    .clickable {
                                        playFeedback()
                                        onToggleUnit()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("unit_toggle_button"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "°F",
                                    fontSize = 12.sp,
                                    fontWeight = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) FontWeight.Black else FontWeight.Normal,
                                    color = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) Color(0xFF1C1C1E) else Color(0xFF8E8E93)
                                )
                                Text(
                                    text = "|",
                                    fontSize = 12.sp,
                                    color = Color(0xFF8E8E93)
                                )
                                Text(
                                    text = "°C",
                                    fontSize = 12.sp,
                                    fontWeight = if (temperatureUnit == TemperatureUnit.CELSIUS) FontWeight.Black else FontWeight.Normal,
                                    color = if (temperatureUnit == TemperatureUnit.CELSIUS) Color(0xFF1C1C1E) else Color(0xFF8E8E93)
                                )
                            }
                        }

                        // Hero 3D Weather Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            ThreeDWeatherCanvas(
                                condition = condition,
                                isDaytime = currentHourly?.isDaytime ?: true,
                                modifier = Modifier.fillMaxSize(0.85f)
                            )
                        }

                        // Giant 3D Brutalist Temperature & Condition Label
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            ThreeDTemperatureText(
                                temperatureValue = displayedTemp,
                                fontSize = 110.sp,
                                color = Color(0xFF1C1C1E),
                                shadowColor = Color(0xFFC7C7CC)
                            )

                            Text(
                                text = condition.label,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp,
                                color = Color(0xFF1C1C1E),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Timeline Scrubber Bar
                        TimelineScrubber(
                            hourlyList = hourly,
                            selectedIndex = selectedHourIndex,
                            highTemp = highTemp,
                            lowTemp = lowTemp,
                            onHourSelected = { idx ->
                                playFeedback()
                                onHourSelected(idx)
                            },
                            modifier = Modifier.testTag("timeline_scrubber")
                        )

                        // Pull / Click for Detailed Cards Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    playFeedback()
                                    onOpenDetailsCard()
                                }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("detailed_forecast_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Expand",
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "DETAILED FORECASTS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }
        }
    }
}
