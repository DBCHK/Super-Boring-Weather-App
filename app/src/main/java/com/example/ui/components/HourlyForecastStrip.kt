package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HourlyForecast
import com.example.data.model.WeatherCondition
import com.example.ui.theme.LocalThemePalette
import com.example.ui.viewmodel.TemperatureUnit
import com.example.util.rememberDropletPlayers
import kotlin.math.roundToInt

/**
 * Not Boring–style horizontal hour picker: time, emoji condition, temp.
 * Tap an hour to scrub the main forecast.
 */
@Composable
fun HourlyForecastStrip(
    hours: List<HourlyForecast>,
    selectedIndex: Int,
    temperatureUnit: TemperatureUnit,
    onHourSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (hours.isEmpty()) return
    val palette = LocalThemePalette.current
    val feedback = rememberDropletPlayers()
    val scroll = rememberScrollState()

    // Keep selected hour roughly in view
    LaunchedEffect(selectedIndex) {
        val approx = (selectedIndex * 72).coerceAtLeast(0)
        scroll.animateScrollTo(approx)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "NEXT HOURS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp,
            color = palette.secondaryText,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .testTag("hourly_forecast_strip"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            hours.take(24).forEachIndexed { index, hour ->
                val selected = index == selectedIndex
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1.06f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "hourCard$index"
                )
                val temp = if (temperatureUnit == TemperatureUnit.CELSIUS) {
                    hour.tempC.roundToInt()
                } else {
                    hour.tempF.roundToInt()
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .width(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (selected) palette.chipSelectedBg else palette.surface
                        )
                        .bouncyClick {
                            feedback.tick()
                            onHourSelected(index)
                        }
                        .padding(vertical = 12.dp, horizontal = 6.dp)
                ) {
                    Text(
                        text = hour.timeLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (selected) palette.chipSelectedFg else palette.secondaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = conditionEmoji(hour.condition),
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$temp°",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = if (selected) palette.chipSelectedFg else palette.primaryText
                    )
                    if (hour.precipChancePercent >= 30) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${hour.precipChancePercent}%",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (selected) {
                                palette.chipSelectedFg.copy(alpha = 0.75f)
                            } else {
                                palette.accent
                            }
                        )
                    }
                }
            }
        }
    }
}

fun conditionEmoji(condition: WeatherCondition): String = when (condition) {
    WeatherCondition.SUNNY -> "☀️"
    WeatherCondition.CLEAR -> "🌙"
    WeatherCondition.PARTLY_CLOUDY -> "⛅"
    WeatherCondition.MOSTLY_CLOUDY, WeatherCondition.CLOUDY -> "☁️"
    WeatherCondition.RAINY -> "🌧"
    WeatherCondition.HEAVY_RAIN -> "⛈"
    WeatherCondition.THUNDERSTORM -> "⚡"
    WeatherCondition.SNOWY -> "❄️"
    WeatherCondition.HAZE -> "🌫"
    WeatherCondition.WINDY -> "💨"
}
