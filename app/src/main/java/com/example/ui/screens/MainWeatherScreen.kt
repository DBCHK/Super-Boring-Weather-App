package com.example.ui.screens

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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherCondition
import com.example.ui.components.AirQualityHeroCard
import com.example.ui.components.ConfettiBurst
import com.example.ui.components.DailyGlanceRow
import com.example.ui.components.HourlyForecastStrip
import com.example.ui.components.LiveWidgetsView
import com.example.ui.components.NotBoringCopy
import com.example.ui.components.ThreeDDigitsRow
import com.example.ui.components.ThreeDWeatherCanvas
import com.example.ui.components.TimelineScrubber
import com.example.ui.components.VibeMeterCard
import com.example.ui.components.WeatherBackgroundShaderCanvas
import com.example.ui.components.WeatherFooter
import com.example.ui.components.WeatherStoryCard
import com.example.ui.components.bouncyClick
import com.example.ui.components.conditionEmoji
import com.example.ui.components.entrance
import com.example.ui.components.interactive3D
import com.example.ui.components.rememberEntranceProgress
import com.example.ui.components.rememberInteractive3DState
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ThemePalette
import com.example.ui.viewmodel.TemperatureUnit
import com.example.ui.viewmodel.WeatherUiState
import com.example.ui.viewmodel.WidgetType
import com.example.util.rememberDropletPlayers
import java.util.Calendar
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun MainWeatherScreen(
    weatherUiState: WeatherUiState,
    selectedHourIndex: Int,
    temperatureUnit: TemperatureUnit,
    themeMode: Int = 1,
    onThemeModeChange: (Int) -> Unit = {},
    lastRefreshAtMs: Long = 0L,
    pinnedWidgets: List<WidgetType> = WidgetType.entries.toList(),
    onToggleWidgetPin: (WidgetType) -> Unit = {},
    onHourSelected: (Int) -> Unit,
    onToggleUnit: () -> Unit,
    onOpenCitySheet: () -> Unit,
    onDetectLocation: () -> Unit,
    onRetry: () -> Unit = {},
    onRefresh: () -> Unit = onRetry,
    onOpenDetailsCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appTheme = when (themeMode) {
        0 -> AppThemeMode.LIGHT
        2 -> AppThemeMode.DARK
        else -> AppThemeMode.YELLOW
    }
    val palette = ThemePalette.forMode(appTheme)
    val backgroundColor = palette.background
    val primaryTextColor = palette.primaryText

    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Stage vignette — stronger on yellow, soft on light, none on dark
            if (themeMode == 0 || themeMode == 1) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val edge = if (themeMode == 1) {
                        listOf(
                            Color.Transparent,
                            Color(0xFF8B5A00).copy(alpha = 0.18f),
                            Color(0xFF3D2200).copy(alpha = 0.42f)
                        )
                    } else {
                        listOf(
                            Color.Transparent,
                            Color(0xFF8E8E93).copy(alpha = 0.06f),
                            Color(0xFF1C1C1E).copy(alpha = 0.12f)
                        )
                    }
                    drawRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to edge[0],
                                0.50f to edge[0],
                                0.78f to edge[1],
                                1.0f to edge[2]
                            ),
                            center = center,
                            radius = size.maxDimension * 0.76f
                        )
                    )
                }
            }

            when (weatherUiState) {
                is WeatherUiState.Loading -> NotBoringLoading(palette = palette)

                is WeatherUiState.Error -> NotBoringError(
                    message = weatherUiState.message,
                    palette = palette,
                    onRetry = onRetry
                )

                is WeatherUiState.Success -> {
                    val data = weatherUiState.data
                    val hourly = data.hourlyList
                    val currentHourly = hourly.getOrNull(selectedHourIndex) ?: hourly.firstOrNull()

                    val displayedTemp = if (selectedHourIndex == 0 || currentHourly == null) {
                        if (temperatureUnit == TemperatureUnit.FAHRENHEIT) data.currentTempF.roundToInt()
                        else data.currentTempC.roundToInt()
                    } else {
                        if (temperatureUnit == TemperatureUnit.FAHRENHEIT) currentHourly.tempF.roundToInt()
                        else currentHourly.tempC.roundToInt()
                    }

                    val highTemp =
                        if (temperatureUnit == TemperatureUnit.FAHRENHEIT) data.highTempF.roundToInt()
                        else data.highTempC.roundToInt()
                    val lowTemp =
                        if (temperatureUnit == TemperatureUnit.FAHRENHEIT) data.lowTempF.roundToInt()
                        else data.lowTempC.roundToInt()
                    val condition = currentHourly?.condition ?: data.condition

                    val feedback = rememberDropletPlayers()
                    val scrollState = rememberScrollState()
                    var swipeUpAccum by remember { mutableFloatStateOf(0f) }
                    var showConfetti by remember { mutableStateOf(false) }
                    var shareToast by remember { mutableStateOf(false) }
                    val clipboard = LocalClipboardManager.current
                    val heroEnter = rememberEntranceProgress(delayMs = 30, durationMs = 620)
                    val chipsEnter = rememberEntranceProgress(delayMs = 160, durationMs = 500)
                    val scrubEnter = rememberEntranceProgress(delayMs = 240, durationMs = 500)
                    val featuresEnter = rememberEntranceProgress(delayMs = 320, durationMs = 560)
                    val greeting = remember {
                        NotBoringCopy.dayPartGreeting(
                            Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        )
                    }
                    val tempCForCopy = if (temperatureUnit == TemperatureUnit.CELSIUS) {
                        displayedTemp.toFloat()
                    } else {
                        (displayedTemp - 32) * 5f / 9f
                    }

                    val chevronBob by rememberInfiniteTransition(label = "chevron").animateFloat(
                        initialValue = 0f,
                        targetValue = -10f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "chevronBob"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (swipeUpAccum < -110f) {
                                            feedback.whooshUp()
                                            onOpenDetailsCard()
                                        }
                                        swipeUpAccum = 0f
                                    },
                                    onDragCancel = { swipeUpAccum = 0f },
                                    onVerticalDrag = { change, dragAmount ->
                                        if (dragAmount < 0f || swipeUpAccum < 0f) {
                                            swipeUpAccum += dragAmount
                                            change.consume()
                                        }
                                    }
                                )
                            }
                    ) {
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

                        ConfettiBurst(
                            active = showConfetti,
                            onFinished = { showConfetti = false }
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            val chromeBg = palette.chromeBg
                            val chromeFg = palette.chromeFg
                            val chromeMuted = palette.chromeMuted

                            // ── Signature tagline (Not Boring home) ───────────
                            Text(
                                text = NotBoringCopy.TAGLINE,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                textAlign = TextAlign.Center,
                                color = primaryTextColor,
                                lineHeight = 20.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, start = 12.dp, end = 12.dp)
                                    .entrance(heroEnter, risePx = 16f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Compact chrome row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(chromeBg)
                                            .bouncyClick {
                                                feedback.plink()
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

                                    ChromeIconButton(
                                        bg = chromeBg,
                                        onClick = {
                                            feedback.chime()
                                            onDetectLocation()
                                        },
                                        testTag = "detect_location_button"
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = "Detect Location",
                                            tint = chromeFg,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    ChromeIconButton(
                                        bg = chromeBg,
                                        onClick = {
                                            feedback.swoosh()
                                            // Cycle: Yellow (1) → Dark (2) → Light (0) → Yellow
                                            onThemeModeChange((themeMode + 1) % 3)
                                        },
                                        testTag = "theme_switcher_button"
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = "Toggle Theme",
                                            tint = chromeFg,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    ChromeIconButton(
                                        bg = chromeBg,
                                        onClick = {
                                            feedback.chime()
                                            val unit =
                                                if (temperatureUnit == TemperatureUnit.CELSIUS) "C" else "F"
                                            val blurb =
                                                "${data.cityName}: $displayedTemp°$unit · " +
                                                    "${condition.label} ${conditionEmoji(condition)} · " +
                                                    "H $highTemp° L $lowTemp° · via NOT BORING WEATHER"
                                            clipboard.setText(AnnotatedString(blurb))
                                            shareToast = true
                                        },
                                        testTag = "share_weather_button"
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Copy forecast",
                                            tint = chromeFg,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(chromeBg)
                                        .bouncyClick {
                                            feedback.snap()
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
                                        fontWeight = if (temperatureUnit == TemperatureUnit.CELSIUS) {
                                            FontWeight.Black
                                        } else {
                                            FontWeight.Normal
                                        },
                                        color = if (temperatureUnit == TemperatureUnit.CELSIUS) {
                                            chromeFg
                                        } else {
                                            chromeMuted
                                        }
                                    )
                                    Text(text = "|", fontSize = 10.sp, color = chromeMuted)
                                    Text(
                                        text = "°F",
                                        fontSize = 11.sp,
                                        fontWeight = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) {
                                            FontWeight.Black
                                        } else {
                                            FontWeight.Normal
                                        },
                                        color = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) {
                                            chromeFg
                                        } else {
                                            chromeMuted
                                        }
                                    )
                                }
                            }

                            // Greeting under chrome
                            Text(
                                text = "$greeting · ${data.cityName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.3.sp,
                                color = palette.secondaryText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, start = 4.dp),
                                textAlign = TextAlign.Start
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // ── TOWERING HERO STICK ──────────────────────────
                            val heroInteraction = rememberInteractive3DState(
                                initialPitch = 14f,
                                initialYaw = 0f,
                                maxPitch = 48f,
                                maxYaw = 32f,
                                autoSpinDegPerSec = 8f,
                                autoSpinOscillate = true
                            )
                            val heroStickArm = 0.42f

                            val glowColor = remember(condition, palette.isDark) {
                                conditionGlowColor(condition, palette.isDark)
                            }
                            val glowPulse by rememberInfiniteTransition(label = "glow").animateFloat(
                                initialValue = 0.55f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2200, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "glowPulse"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp)
                                    .entrance(heroEnter, risePx = 40f),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(360.dp)
                                ) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                glowColor.copy(alpha = 0.38f * glowPulse),
                                                glowColor.copy(alpha = 0.10f * glowPulse),
                                                Color.Transparent
                                            ),
                                            center = center,
                                            radius = size.minDimension * 0.58f
                                        ),
                                        radius = size.minDimension * 0.58f,
                                        center = center
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .interactive3D(
                                            heroInteraction,
                                            enablePitch = true,
                                            enableDeviceTilt = true
                                        )
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    feedback.splash()
                                                    showConfetti = true
                                                }
                                            )
                                        }
                                ) {
                                    // Weather orb — large, stage-center
                                    ThreeDWeatherCanvas(
                                        condition = condition,
                                        isDaytime = currentHourly?.isDaytime ?: true,
                                        modelScale = 1.85f,
                                        tintColor = palette.elementFill,
                                        shadeColor = palette.elementShade,
                                        interactionState = heroInteraction,
                                        enableGestures = false,
                                        applyInteractionRotation = true,
                                        stickArmY = heroStickArm,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(190.dp)
                                    )

                                    Spacer(modifier = Modifier.height(0.dp))

                                    // Towering temperature digits
                                    ThreeDDigitsRow(
                                        number = displayedTemp,
                                        interactionState = heroInteraction,
                                        scaleToUnits = 2.15f,
                                        spacing = 0.72f,
                                        fillColor = palette.elementFill,
                                        shadeColor = palette.elementShade,
                                        shadowColor = palette.elementShadow,
                                        highlightColor = palette.elementHighlight,
                                        enableGestures = false,
                                        applyInteractionRotation = true,
                                        stickArmY = -heroStickArm,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                            .offset(y = (-28).dp)
                                    )
                                }
                            }

                            LaunchedEffect(shareToast) {
                                if (shareToast) {
                                    delay(1800)
                                    shareToast = false
                                }
                            }
                            AnimatedVisibility(
                                visible = shareToast,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Text(
                                    text = "COPIED FORECAST ✓",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = palette.accent,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            LiveRefreshBadge(
                                lastRefreshAtMs = lastRefreshAtMs,
                                palette = palette,
                                onRefresh = {
                                    feedback.bubble()
                                    onRefresh()
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Condition — big, clean, reference-style
                            Text(
                                text = condition.label.lowercase()
                                    .replaceFirstChar { it.titlecase() },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = 0.1.sp,
                                color = primaryTextColor
                            )

                            Text(
                                text = comfortLine(
                                    tempC = tempCForCopy,
                                    humidity = currentHourly?.humidityPercent ?: data.humidityPercent,
                                    windMph = currentHourly?.windSpeedMph ?: data.windSpeedMph
                                ),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = palette.secondaryText,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                            )

                            // Quick stats
                            val humidity = currentHourly?.humidityPercent ?: data.humidityPercent
                            val wind = (currentHourly?.windSpeedMph ?: data.windSpeedMph).roundToInt()
                            val rain = currentHourly?.precipChancePercent ?: data.precipChancePercent
                            val uv = (currentHourly?.uvIndex ?: data.uvIndex).roundToInt()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(bottom = 6.dp)
                                    .entrance(chipsEnter, risePx = 18f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WeatherStatChip("H $highTemp°", palette) { feedback.pop() }
                                WeatherStatChip("L $lowTemp°", palette) { feedback.pop() }
                                WeatherStatChip("💧 $humidity%", palette) { feedback.drip() }
                                WeatherStatChip("💨 ${wind}mph", palette) { feedback.whooshUp() }
                                WeatherStatChip("🌧 $rain%", palette) { feedback.bubble() }
                                WeatherStatChip("UV $uv", palette) { feedback.plink() }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Timeline scrubber — high/low + blue handle energy
                            TimelineScrubber(
                                hourlyList = hourly,
                                selectedIndex = selectedHourIndex,
                                highTemp = highTemp,
                                lowTemp = lowTemp,
                                onHourSelected = onHourSelected,
                                trackColor = palette.scrubberTrack,
                                activeColor = palette.scrubberActive,
                                labelColor = palette.secondaryText,
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .entrance(scrubEnter, risePx = 22f)
                                    .testTag("timeline_scrubber")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Feature stack
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .entrance(featuresEnter, risePx = 32f),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                HourlyForecastStrip(
                                    hours = hourly,
                                    selectedIndex = selectedHourIndex,
                                    temperatureUnit = temperatureUnit,
                                    onHourSelected = onHourSelected
                                )

                                VibeMeterCard(
                                    tempC = tempCForCopy,
                                    humidity = humidity,
                                    windMph = currentHourly?.windSpeedMph ?: data.windSpeedMph,
                                    precipChance = rain,
                                    uv = currentHourly?.uvIndex ?: data.uvIndex,
                                    condition = condition
                                )

                                WeatherStoryCard(
                                    condition = condition,
                                    tempC = tempCForCopy,
                                    humidity = humidity,
                                    precipChance = rain,
                                    windMph = currentHourly?.windSpeedMph ?: data.windSpeedMph
                                )

                                AirQualityHeroCard(aqi = data.airQualityIndex)

                                DailyGlanceRow(
                                    days = data.dailyList,
                                    temperatureUnit = temperatureUnit,
                                    onDayTap = {
                                        feedback.whooshUp()
                                        onOpenDetailsCard()
                                    }
                                )

                                // Live widgets showcase (in-app, Not Boring style)
                                LiveWidgetsView(
                                    data = data,
                                    pinnedWidgets = pinnedWidgets,
                                    onToggleWidgetPin = onToggleWidgetPin,
                                    temperatureUnit = temperatureUnit
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Swipe-up affordance
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .bouncyClick {
                                        feedback.whooshUp()
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

                            WeatherFooter(textColor = palette.secondaryText)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading / Error — branded, not Material-generic
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NotBoringLoading(palette: ThemePalette) {
    val pulse by rememberInfiniteTransition(label = "loadPulse").animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadPulseVal"
    )
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .padding(32.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
        ) {
            Text(
                text = NotBoringCopy.TAGLINE,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                color = palette.primaryText,
                lineHeight = 24.sp
            )
            CircularProgressIndicator(
                color = palette.primaryText,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "FETCHING FORECAST...",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.4.sp,
                color = palette.secondaryText
            )
        }
    }
}

@Composable
private fun NotBoringError(
    message: String,
    palette: ThemePalette,
    onRetry: () -> Unit
) {
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
                text = "PLOT TWIST",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp,
                color = palette.secondaryText
            )
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                color = palette.danger
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.chipSelectedBg)
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
                        color = palette.chipSelectedFg
                    )
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = palette.chipSelectedFg
                    )
                }
            }
        }
    }
}

@Composable
private fun ChromeIconButton(
    bg: Color,
    onClick: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(bg)
            .bouncyClick(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun WeatherStatChip(
    label: String,
    palette: ThemePalette,
    onTap: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.chipBg)
            .bouncyClick(onClick = onTap)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = palette.primaryText,
            maxLines = 1
        )
    }
}

@Composable
private fun LiveRefreshBadge(
    lastRefreshAtMs: Long,
    palette: ThemePalette,
    onRefresh: () -> Unit
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lastRefreshAtMs) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(15_000)
        }
    }
    val ageSec = if (lastRefreshAtMs <= 0L) -1L else (nowMs - lastRefreshAtMs) / 1000L
    val label = when {
        ageSec < 0L -> "SYNC"
        ageSec < 45L -> "LIVE · just now"
        ageSec < 120L -> "LIVE · 1m ago"
        ageSec < 3600L -> "LIVE · ${ageSec / 60}m ago"
        else -> "LIVE · ${ageSec / 3600}h ago"
    }
    val pulse by rememberInfiniteTransition(label = "live").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "livePulse"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(palette.chipBg)
            .clickable(onClick = onRefresh)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("live_refresh_badge"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .graphicsLayer { alpha = pulse }
                .clip(CircleShape)
                .background(if (ageSec in 0 until 120) palette.liveDot else palette.accent)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp,
            color = palette.primaryText
        )
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Refresh now",
            tint = palette.secondaryText,
            modifier = Modifier.size(12.dp)
        )
    }
}

private fun conditionGlowColor(condition: WeatherCondition, isDark: Boolean): Color {
    return when (condition) {
        WeatherCondition.SUNNY, WeatherCondition.CLEAR ->
            if (isDark) Color(0xFFFFD60A) else Color(0xFFFF9F0A)
        WeatherCondition.RAINY, WeatherCondition.HEAVY_RAIN ->
            if (isDark) Color(0xFF64D2FF) else Color(0xFF007AFF)
        WeatherCondition.THUNDERSTORM ->
            if (isDark) Color(0xFFBF5AF2) else Color(0xFFAF52DE)
        WeatherCondition.SNOWY ->
            if (isDark) Color(0xFFE5E5EA) else Color(0xFF8E8E93)
        WeatherCondition.WINDY ->
            if (isDark) Color(0xFF5AC8FA) else Color(0xFF64D2FF)
        else ->
            if (isDark) Color(0xFF8E8E93) else Color(0xFF636366)
    }
}

/** Playful one-liner from temp + humidity + wind. */
private fun comfortLine(tempC: Float, humidity: Int, windMph: Float): String {
    return when {
        tempC >= 32f -> "Scorching · hydrate like you mean it"
        tempC >= 26f && humidity >= 70 -> "Sticky heat · shade is a strategy"
        tempC >= 22f && tempC < 28f && humidity < 65 && windMph < 15f ->
            "Pretty perfect · go touch grass"
        tempC in 16f..22f && windMph < 18f -> "Jacket optional · vibes mandatory"
        tempC < 5f -> "Bundle up · winter is not a drill"
        tempC < 12f -> "Crisp air · hoodie weather unlocked"
        windMph >= 22f -> "Windy · hold onto your hat (and plans)"
        humidity >= 85 -> "Humidity doing the most today"
        else -> "Another day of atmospheric theatre"
    }
}
