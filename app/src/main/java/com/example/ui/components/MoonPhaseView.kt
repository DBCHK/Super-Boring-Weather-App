package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.roundToInt

@Composable
fun MoonPhaseView(
    data: WeatherForecastData,
    modifier: Modifier = Modifier
) {
    // Approximate lunar illumination from calendar day of month (synodic-ish curve)
    val todayIllum = remember {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        // Rough 29.5-day cycle mapped onto 0..1 illumination
        val phase = (day % 30) / 29.5
        ((1.0 - cos(phase * 2.0 * Math.PI)) / 2.0).toFloat().coerceIn(0.02f, 0.98f)
    }

    var sliderVal by remember { mutableFloatStateOf(todayIllum) }
    var isWeekMode by remember { mutableStateOf(false) }
    var selectedWeekDay by remember { mutableIntStateOf(0) }

    val weekPhases = remember {
        (0 until 7).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val phase = (day % 30) / 29.5
            val illum = ((1.0 - cos(phase * 2.0 * Math.PI)) / 2.0).toFloat().coerceIn(0.02f, 0.98f)
            val dayName = if (offset == 0) {
                "TODAY"
            } else {
                cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, java.util.Locale.US)
                    ?.uppercase() ?: "DAY"
            }
            val dateLabel = android.text.format.DateFormat.format("MMM d", cal).toString().uppercase()
            Triple(dayName, dateLabel, illum)
        }
    }

    val illumination = if (isWeekMode) {
        ((weekPhases.getOrNull(selectedWeekDay)?.third ?: todayIllum) * 100).roundToInt()
    } else {
        (sliderVal * 100).roundToInt().coerceIn(0, 100)
    }

    val phaseName = phaseNameFor(illumination)

    // Approximate next full / new moon labels
    val (fullMoonLabel, newMoonLabel) = remember {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        // Full around day ~15 of lunar-ish cycle, new around day ~0/30
        val daysToFull = ((15 - (day % 30)) + 30) % 30
        val daysToNew = ((30 - (day % 30)) + 30) % 30
        val full = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, daysToFull) }
        val newM = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, daysToNew) }
        val fmt = java.text.SimpleDateFormat("M/d/yy", java.util.Locale.US)
        fmt.format(full.time) to fmt.format(newM.time)
    }

    val moonrise = remember {
        val hour = 2 + (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) % 4)
        val min = (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) * 7) % 60
        String.format("%d:%02dA", hour, min)
    }
    val moonset = remember {
        val hour = 4 + (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) % 5)
        val min = (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) * 11) % 60
        String.format("%d:%02dP", hour, min)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 3D Moon
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

        // Metrics
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C1C1E))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricLine("ILLUMINATION", "$illumination%")
            DividerLine()
            MetricLine("PHASE", phaseName)
            DividerLine()
            MetricLine("MOONRISE", moonrise)
            DividerLine()
            MetricLine("MOONSET", moonset)
            DividerLine()
            MetricLine("FULL MOON", fullMoonLabel)
            DividerLine()
            MetricLine("NEW MOON", newMoonLabel)
            if (isWeekMode) {
                DividerLine()
                val day = weekPhases.getOrNull(selectedWeekDay)
                MetricLine(
                    "DAY",
                    "${day?.first ?: "TODAY"} · ${day?.second ?: ""}"
                )
            } else {
                DividerLine()
                MetricLine("LOCATION", data.cityName.uppercase())
            }
        }

        AnimatedContent(
            targetState = isWeekMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "moonMode"
        ) { weekMode ->
            if (weekMode) {
                // Weekly moon phase strip
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "7-DAY LUNAR PHASES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    weekPhases.forEachIndexed { idx, (name, date, illum) ->
                        val pct = (illum * 100).roundToInt()
                        val selected = idx == selectedWeekDay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) Color(0xFF1C1C1E) else Color.White)
                                .clickable { selectedWeekDay = idx }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("moon_week_day_$idx"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (selected) Color.White else Color(0xFF1C1C1E)
                                )
                                Text(
                                    text = date,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp)) {
                                    ThreeDMoonCanvas(
                                        illuminationPercent = pct,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$pct%",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (selected) Color.White else Color(0xFF1C1C1E)
                                    )
                                    Text(
                                        text = phaseNameFor(pct).take(12),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                // Day mode scrubber
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
            }
        }

        // DAY / WEEK Toggle
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

@Composable
private fun MetricLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White
        )
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF2C2C2E))
    )
}

private fun phaseNameFor(illumination: Int): String {
    return when {
        illumination < 5 -> "NEW MOON"
        illumination in 5..20 -> "WAXING CRESCENT"
        illumination in 21..40 -> "WAXING CRESCENT"
        illumination in 41..55 -> "FIRST QUARTER"
        illumination in 56..80 -> "WAXING GIBBOUS"
        illumination in 81..95 -> "WAXING GIBBOUS"
        else -> "FULL MOON"
    }
}
