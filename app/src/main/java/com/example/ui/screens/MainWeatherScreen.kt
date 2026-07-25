package com.example.ui.screens

import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ThreeDDigitsRow
import com.example.ui.components.ThreeDWeatherCanvas
import com.example.ui.components.TimelineScrubber
import com.example.ui.components.WeatherBackgroundShaderCanvas
import com.example.ui.components.WeatherFooter
import com.example.ui.components.rememberInteractive3DState
import com.example.ui.viewmodel.TemperatureUnit
import com.example.ui.viewmodel.WeatherUiState
import com.example.util.rememberDropletFeedback
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun MainWeatherScreen(
    weatherUiState: WeatherUiState,
    selectedHourIndex: Int,
    temperatureUnit: TemperatureUnit,
    onHourSelected: (Int) -> Unit,
    onToggleUnit: () -> Unit,
    onOpenCitySheet: () -> Unit,
    onDetectLocation: () -> Unit,
    onRetry: () -> Unit = {},
    onRefresh: () -> Unit = onRetry,
    onOpenDetailsCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var themeMode by remember { mutableIntStateOf(0) } // 0 = Light Minimal, 1 = Vibrant Yellow (Image 5), 2 = Dark Charcoal

    val backgroundColor = when (themeMode) {
        1 -> Color(0xFFFFB300) // Vibrant Gold/Yellow (Matching Image 5)
        2 -> Color(0xFF1C1C1E) // Dark Charcoal
        else -> Color(0xFFF2F2F7) // Light Minimal
    }

    val primaryTextColor = when (themeMode) {
        1 -> Color(0xFF1C1C1E)
        2 -> Color.White
        else -> Color(0xFF1C1C1E)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor
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
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            }

            is WeatherUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = weatherUiState.message,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            color = Color(0xFFFF3B30)
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1C1C1E))
                                .clickable { onRetry() }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                                .testTag("retry_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "RETRY",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            is WeatherUiState.Success -> {
                val data = weatherUiState.data
                val hourly = data.hourlyList
                val currentHourly = hourly.getOrNull(selectedHourIndex) ?: hourly.firstOrNull()

                // Index 0 / NOW always uses live current temp for the selected city
                val displayedTemp = if (selectedHourIndex == 0 || currentHourly == null) {
                    if (temperatureUnit == TemperatureUnit.FAHRENHEIT) data.currentTempF.roundToInt()
                    else data.currentTempC.roundToInt()
                } else {
                    if (temperatureUnit == TemperatureUnit.FAHRENHEIT) currentHourly.tempF.roundToInt()
                    else currentHourly.tempC.roundToInt()
                }

                val highTemp = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) data.highTempF.roundToInt() else data.highTempC.roundToInt()
                val lowTemp = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) data.lowTempF.roundToInt() else data.lowTempC.roundToInt()
                val condition = currentHourly?.condition ?: data.condition

                val (playFeedback, _) = rememberDropletFeedback()
                val scrollState = rememberScrollState()

                Box(modifier = Modifier.fillMaxSize()) {
                    // Background Particle Shader Canvas Layer behind giant typography
                    WeatherBackgroundShaderCanvas(
                        condition = condition,
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Compact top chrome — smaller chips & icons
                        val chromeBg = if (themeMode == 1) Color(0xFF1C1C1E) else Color(0xFFE5E5EA)
                        val chromeFg = if (themeMode == 1) Color.White else Color(0xFF1C1C1E)
                        val chromeMuted = if (themeMode == 1) Color.White.copy(alpha = 0.45f) else Color(0xFF8E8E93)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    // City pill
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(chromeBg)
                                            .clickable {
                                                playFeedback()
                                                onOpenCitySheet()
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                            .testTag("city_selector_button"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Cities",
                                            tint = chromeFg,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = data.cityName.uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = chromeFg,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Detect location — slim circular hit target
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(chromeBg)
                                            .clickable {
                                                playFeedback()
                                                onDetectLocation()
                                            }
                                            .testTag("detect_location_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = "Detect Location",
                                            tint = chromeFg,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    // Theme
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(chromeBg)
                                            .clickable {
                                                playFeedback()
                                                themeMode = (themeMode + 1) % 3
                                            }
                                            .testTag("theme_switcher_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = "Toggle Theme",
                                            tint = chromeFg,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                // Unit capsule — °C first (default)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(chromeBg)
                                        .clickable {
                                            playFeedback()
                                            onToggleUnit()
                                        }
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                        .testTag("unit_toggle_button"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "°C",
                                        fontSize = 11.sp,
                                        fontWeight = if (temperatureUnit == TemperatureUnit.CELSIUS) FontWeight.Black else FontWeight.Normal,
                                        color = if (temperatureUnit == TemperatureUnit.CELSIUS) chromeFg else chromeMuted
                                    )
                                    Text(
                                        text = "|",
                                        fontSize = 10.sp,
                                        color = chromeMuted
                                    )
                                    Text(
                                        text = "°F",
                                        fontSize = 11.sp,
                                        fontWeight = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) FontWeight.Black else FontWeight.Normal,
                                        color = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) chromeFg else chromeMuted
                                    )
                                }
                            }

                            // Yellow theme: hero quote + rotating humorous lines
                            if (themeMode == 1) {
                                val yellowQuotes = remember {
                                    listOf(
                                        "LIFE'S TOO SHORT TO WASTE TIME ON BORING APPS",
                                        "UMBRELLA? THAT'S CUTE. THE CLOUDS SAID NO.",
                                        "WEATHER SO EXTRA IT NEEDS A PUBLICIST",
                                        "SUN'S OUT. EXCUSES ARE CANCELLED.",
                                        "IF RAIN HAD A PERSONALITY, IT'D BE PETTY",
                                        "HOT TAKE: IT'S LITERALLY HOT",
                                        "CLOUDS DOING THE MOST. AGAIN.",
                                        "FORECAST: 100% CHANCE OF CHAOS"
                                    )
                                }
                                var quoteIndex by remember { mutableIntStateOf(0) }
                                LaunchedEffect(themeMode) {
                                    quoteIndex = 0
                                    while (true) {
                                        delay(4500)
                                        quoteIndex = (quoteIndex + 1) % yellowQuotes.size
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = yellowQuotes[quoteIndex],
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.SansSerif,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFF1C1C1E),
                                    letterSpacing = 0.3.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(horizontal = 18.dp)
                                )
                            }
                        }

                        // Hero 3D Weather + Temperature — slightly larger models over digits
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy((-16).dp),
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ThreeDWeatherCanvas(
                                    condition = condition,
                                    isDaytime = currentHourly?.isDaytime ?: true,
                                    modelScale = 1.85f,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            val tempInteraction = rememberInteractive3DState(
                                initialPitch = 0f,
                                initialYaw = 0f,
                                maxPitch = 0f,
                                maxYaw = 38f, // never flip past readable range
                                autoSpinDegPerSec = 12f,
                                autoSpinOscillate = true // left ↔ right, not full 360°
                            )
                            ThreeDDigitsRow(
                                number = displayedTemp,
                                interactionState = tempInteraction,
                                scaleToUnits = 1.55f,
                                spacing = 1.05f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            )

                            Text(
                                text = condition.label,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = 2.sp,
                                color = primaryTextColor,
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
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .testTag("timeline_scrubber")
                        )

                        // Pull / Click for Detailed Cards Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    playFeedback()
                                    onOpenDetailsCard()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("detailed_forecast_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Expand",
                                tint = if (themeMode == 1) Color(0xFF1C1C1E) else Color(0xFF8E8E93),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "DETAILED FORECASTS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp,
                                color = if (themeMode == 1) Color(0xFF1C1C1E) else Color(0xFF8E8E93)
                            )
                        }

                        WeatherFooter(
                            textColor = primaryTextColor
                        )
                    }
                }
            }
        }
    }
}
