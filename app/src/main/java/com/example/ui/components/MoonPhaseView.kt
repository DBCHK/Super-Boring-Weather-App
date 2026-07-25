package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

@Composable
fun MoonPhaseView(
    data: WeatherForecastData,
    modifier: Modifier = Modifier
) {
    var sliderVal by remember { mutableFloatStateOf(0.13f) }
    var isWeekMode by remember { mutableStateOf(false) }

    val illumination = (sliderVal * 100).toInt().coerceIn(0, 100)
    val phaseName = when {
        illumination < 5 -> "NEW MOON"
        illumination in 5..45 -> "WANING CRESCENT"
        illumination in 46..55 -> "FIRST QUARTER"
        illumination in 56..95 -> "WAXING GIBBOUS"
        else -> "FULL MOON"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Giant 3D Moon Canvas Container (Matching Image 3)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF18181A)),
            contentAlignment = Alignment.Center
        ) {
            ThreeDMoonCanvas(
                illuminationPercent = illumination,
                phaseName = phaseName,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Metrics Table Box (Exact matching Image 3)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C1C1E))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "ILLUMINATION", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                Text(
                    text = "$illumination%",
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
                Text(text = "PHASE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                Text(
                    text = phaseName,
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
                Text(text = "MOONRISE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                Text(
                    text = "3:03A",
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
                Text(text = "MOONSET", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                Text(
                    text = "6:21P",
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
                Text(text = "FULL MOON", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                Text(
                    text = "9/17/26",
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
                Text(text = "NEW MOON", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                Text(
                    text = "9/2/26",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
        }

        // Scrubbable Timeline Slider (Matching Image 3)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("NOW", "6A", "12P", "6P").forEach { time ->
                    Text(
                        text = time,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF8E8E93)
                    )
                }
            }

            Slider(
                value = sliderVal,
                onValueChange = { sliderVal = it },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0xFF3A3A3C)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("moon_phase_slider")
            )
        }

        // DAY / WEEK Toggle Pill
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
                    .clickable { isWeekMode = false }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("moon_day_toggle"),
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
                    .clickable { isWeekMode = true }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("moon_week_toggle"),
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
