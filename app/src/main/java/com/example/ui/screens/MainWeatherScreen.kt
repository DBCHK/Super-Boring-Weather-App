package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.components.interactive3D
import com.example.ui.components.rememberInteractive3DState
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ThemePalette
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
    // Default: vibrant yellow theme (matches Not Boring home reference)
    var themeMode by remember { mutableIntStateOf(1) } // 0 Light, 1 Yellow, 2 Dark
    val appTheme = when (themeMode) {
        1 -> AppThemeMode.YELLOW
        2 -> AppThemeMode.DARK
        else -> AppThemeMode.LIGHT
    }
    val palette = ThemePalette.forMode(appTheme)
    val backgroundColor = palette.background
    val primaryTextColor = palette.primaryText

    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Vignette behind content (yellow = stronger, light = slight; dark = none)
            if (themeMode == 0 || themeMode == 1) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val edge = if (themeMode == 1) {
                        listOf(
                            Color.Transparent,
                            Color(0xFF8B5A00).copy(alpha = 0.20f),
                            Color(0xFF3D2200).copy(alpha = 0.48f)
                        )
                    } else {
                        listOf(
                            Color.Transparent,
                            Color(0xFF8E8E93).copy(alpha = 0.07f),
                            Color(0xFF1C1C1E).copy(alpha = 0.14f)
                        )
                    }
                    drawRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to edge[0],
                                0.52f to edge[0],
                                0.80f to edge[1],
                                1.0f to edge[2]
                            ),
                            center = center,
                            radius = size.maxDimension * 0.74f
                        )
                    )
                }
            }

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
                            color = primaryTextColor,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "FETCHING FORECAST...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp,
                            color = palette.secondaryText
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
                var swipeUpAccum by remember { mutableFloatStateOf(0f) }

                // Subtle bounce on the "swipe up" chevron
                val chevronBob by rememberInfiniteTransition(label = "chevron").animateFloat(
                    initialValue = 0f,
                    targetValue = -8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "chevronBob"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // Swipe up anywhere on home to open details (creative sheet gesture)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (swipeUpAccum < -110f) {
                                        playFeedback()
                                        onOpenDetailsCard()
                                    }
                                    swipeUpAccum = 0f
                                },
                                onDragCancel = { swipeUpAccum = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    // Prefer upward gestures; ignore small noise
                                    if (dragAmount < 0f || swipeUpAccum < 0f) {
                                        swipeUpAccum += dragAmount
                                        change.consume()
                                    }
                                }
                            )
                        }
                ) {
                    // Background Particle Shader Canvas Layer behind giant typography
                    WeatherBackgroundShaderCanvas(
                        condition = condition,
                        particlePrimary = palette.particlePrimary,
                        particleSecondary = palette.particleSecondary,
                        isDarkTheme = palette.isDark,
                        windDirectionDegrees = currentHourly?.windDirectionDegrees
                            ?: data.windDirectionDegrees,
                        windSpeedMph = currentHourly?.windSpeedMph ?: data.windSpeedMph,
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Compact top chrome — theme-aware chips & icons
                        val chromeBg = palette.chromeBg
                        val chromeFg = palette.chromeFg
                        val chromeMuted = palette.chromeMuted

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

                            // Hero quote — reference scale & airy spacing (yellow theme)
                            if (themeMode == 1) {
                                val yellowQuotes = remember {
                                    listOf(
                                        "Life's too short to\nwaste on boring apps.",
                                        "Umbrella? That's cute.\nThe clouds said no.",
                                        "Weather so extra\nit needs a publicist.",
                                        "Sun's out.\nExcuses are cancelled.",
                                        "If rain had a personality,\nit'd be petty.",
                                        "Hot take:\nit's literally hot.",
                                        "Clouds doing the most.\nAgain.",
                                        "Forecast: 100%\nchance of chaos."
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
                                Spacer(modifier = Modifier.height(28.dp))
                                Text(
                                    text = yellowQuotes[quoteIndex],
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.SansSerif,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFF1C1C1E),
                                    letterSpacing = (-0.4).sp,
                                    lineHeight = 34.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }

                        // Hero stick: weather on TOP, digits at BOTTOM of one rigid stick.
                        // Shared pivot mid-column → pitch tips ends opposite ways; yaw swings as one body.
                        Spacer(modifier = Modifier.height(if (themeMode == 1) 36.dp else 28.dp))

                        // Pitch allowed on Y-drag / tilt, but hard-capped (<90°) so never upside-down
                        val heroInteraction = rememberInteractive3DState(
                            initialPitch = 12f,
                            initialYaw = 0f,
                            maxPitch = 52f,
                            maxYaw = 36f,
                            autoSpinDegPerSec = 9f,
                            autoSpinOscillate = true
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .interactive3D(
                                    heroInteraction,
                                    enablePitch = true, // vertical drag → stick pitch
                                    enableDeviceTilt = true,
                                    // Parent owns the stick transform; children stay local-upright
                                    applyLayerRotation = true,
                                    // Pivot between weather (top ~200dp) and digits (bottom ~200dp)
                                    layerTransformOrigin = TransformOrigin(0.5f, 0.5f)
                                )
                        ) {
                            ThreeDWeatherCanvas(
                                condition = condition,
                                isDaytime = currentHourly?.isDaytime ?: true,
                                modelScale = 2.15f,
                                tintColor = if (palette.isDark) Color(0xFFF2F2F7) else Color(0xFF2C2C2E),
                                shadeColor = if (palette.isDark) Color(0xFFC7C7CC) else Color(0xFF636366),
                                interactionState = heroInteraction,
                                enableGestures = false,
                                applyInteractionRotation = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            ThreeDDigitsRow(
                                number = displayedTemp,
                                interactionState = heroInteraction,
                                scaleToUnits = 2.20f,
                                spacing = 1.08f,
                                fillColor = palette.elementFill,
                                shadeColor = palette.elementShade,
                                shadowColor = palette.elementShadow,
                                highlightColor = palette.elementHighlight,
                                enableGestures = false,
                                applyInteractionRotation = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = condition.label.lowercase()
                                .replaceFirstChar { it.titlecase() },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.2.sp,
                            color = primaryTextColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                            trackColor = palette.scrubberTrack,
                            activeColor = palette.scrubberActive,
                            labelColor = palette.secondaryText,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .testTag("timeline_scrubber")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Swipe-up affordance (creative sheet cue)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    playFeedback()
                                    onOpenDetailsCard()
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                                .testTag("detailed_forecast_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(palette.secondaryText.copy(alpha = 0.35f))
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Swipe up for details",
                                tint = palette.secondaryText,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer { translationY = chevronBob }
                            )
                            Text(
                                text = "SWIPE UP FOR DETAILS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.2.sp,
                                color = palette.secondaryText
                            )
                        }

                        WeatherFooter(
                            textColor = palette.secondaryText
                        )
                    }
                }
            }
            } // when
        } // Box
    } // Surface
}
