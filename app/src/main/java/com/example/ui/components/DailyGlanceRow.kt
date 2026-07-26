package com.example.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyForecast
import com.example.ui.theme.LocalThemePalette
import com.example.ui.viewmodel.TemperatureUnit
import com.example.util.rememberDropletPlayers
import kotlin.math.roundToInt

@Composable
fun DailyGlanceRow(
    days: List<DailyForecast>,
    temperatureUnit: TemperatureUnit,
    onDayTap: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return
    val palette = LocalThemePalette.current
    val feedback = rememberDropletPlayers()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "THIS WEEK",
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
                .horizontalScroll(rememberScrollState())
                .testTag("daily_glance_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            days.take(7).forEachIndexed { index, day ->
                val high = if (temperatureUnit == TemperatureUnit.CELSIUS) {
                    day.maxTempC.roundToInt()
                } else {
                    day.maxTempF.roundToInt()
                }
                val low = if (temperatureUnit == TemperatureUnit.CELSIUS) {
                    day.minTempC.roundToInt()
                } else {
                    day.minTempF.roundToInt()
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(palette.surface)
                        .bouncyClick {
                            feedback.pop()
                            onDayTap(index)
                        }
                        .padding(vertical = 12.dp, horizontal = 6.dp)
                ) {
                    Text(
                        text = if (index == 0) "TODAY" else day.dayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = palette.secondaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = conditionEmoji(day.condition), fontSize = 22.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$high°",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = palette.primaryText
                    )
                    Text(
                        text = "$low°",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = palette.secondaryText
                    )
                    if (day.precipChancePercent >= 20) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🌧 ${day.precipChancePercent}%",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = palette.accent
                        )
                    }
                }
            }
        }
    }
}
