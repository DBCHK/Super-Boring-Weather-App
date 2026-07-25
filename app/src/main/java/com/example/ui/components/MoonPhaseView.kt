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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherForecastData
import com.example.util.MoonPhaseCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * Moon tab matched to the Not Boring–style reference:
 * large dark-stage moon, metric stack, day scrubber, DAY/WEEK toggle.
 */
@Composable
fun MoonPhaseView(
    data: WeatherForecastData,
    modifier: Modifier = Modifier
) {
    val todaySnap = remember { MoonPhaseCalculator.forDate() }
    val (moonrise, moonset) = remember { MoonPhaseCalculator.approximateRiseSetLabels() }

    var isWeekMode by remember { mutableStateOf(false) }
    var selectedWeekDay by remember { mutableIntStateOf(0) }
    // 0 = new → 0.5 = full → 1 = new (synodic fraction)
    var phaseScrub by remember {
        mutableFloatStateOf(todaySnap.phaseFraction.toFloat().coerceIn(0f, 1f))
    }

    val weekPhases = remember {
        (0 until 7).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
            val snap = MoonPhaseCalculator.forDate(cal)
            val dayName = if (offset == 0) {
                "TODAY"
            } else {
                cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US)
                    ?.uppercase() ?: "DAY"
            }
            val dateLabel =
                android.text.format.DateFormat.format("MMM d", cal).toString().uppercase()
            MoonDayRow(dayName, dateLabel, snap)
        }
    }

    val displayIllumination: Int
    val displayIsWaxing: Boolean
    val displayPhaseName: String

    if (isWeekMode) {
        val snap = weekPhases.getOrNull(selectedWeekDay)?.snap ?: todaySnap
        displayIllumination = (snap.illumination * 100).roundToInt().coerceIn(0, 100)
        displayIsWaxing = snap.isWaxing
        displayPhaseName = snap.phaseName
    } else {
        val f = phaseScrub.toDouble()
        displayIllumination =
            (((1.0 - cos(f * 2.0 * Math.PI)) / 2.0) * 100).roundToInt().coerceIn(0, 100)
        displayIsWaxing = phaseScrub < 0.5f
        displayPhaseName = phaseNameFromScrub(phaseScrub)
    }

    val dateFmt = remember { SimpleDateFormat("M/d/yy", Locale.US) }
    val fullMoonLabel = remember(todaySnap.daysToFull) {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, todaySnap.daysToFull) }
        dateFmt.format(cal.time)
    }
    val newMoonLabel = remember(todaySnap.daysToNew) {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, todaySnap.daysToNew) }
        dateFmt.format(cal.time)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF0A0A0B))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center
        ) {
            ThreeDMoonCanvas(
                illuminationPercent = displayIllumination,
                phaseName = displayPhaseName,
                isWaxing = displayIsWaxing,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            MetricLine("ILLUMINATION", "$displayIllumination%")
            MetricDivider()
            MetricLine("PHASE", displayPhaseName)
            MetricDivider()
            MetricLine("MOONRISE", moonrise)
            MetricDivider()
            MetricLine("MOONSET", moonset)
            MetricDivider()
            MetricLine("FULL MOON", fullMoonLabel)
            MetricDivider()
            MetricLine("NEW MOON", newMoonLabel)
            if (isWeekMode) {
                MetricDivider()
                val day = weekPhases.getOrNull(selectedWeekDay)
                MetricLine("DAY", "${day?.name ?: "TODAY"} · ${day?.dateLabel ?: ""}")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        AnimatedContent(
            targetState = isWeekMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "moonMode"
        ) { weekMode ->
            if (weekMode) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "7-DAY LUNAR PHASES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                    )
                    weekPhases.forEachIndexed { idx, row ->
                        val pct = (row.snap.illumination * 100).roundToInt()
                        val selected = idx == selectedWeekDay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) Color(0xFF1C1C1E) else Color(0xFF141416))
                                .clickable {
                                    selectedWeekDay = idx
                                    phaseScrub = row.snap.phaseFraction.toFloat()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("moon_week_day_$idx"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = row.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                                Text(
                                    text = row.dateLabel,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(34.dp)) {
                                    ThreeDMoonCanvas(
                                        illuminationPercent = pct,
                                        phaseName = row.snap.phaseName,
                                        isWaxing = row.snap.isWaxing,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$pct%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                    Text(
                                        text = row.snap.phaseName.take(12),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("NOW", "6A", "12P", "6P").forEach { label ->
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 2.dp, top = 2.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            listOf("100", "75", "50", "25", "0").forEach { t ->
                                Text(
                                    text = t,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF636366),
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }

                        Slider(
                            value = phaseScrub,
                            onValueChange = { phaseScrub = it },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color(0xFF3A3A3C)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 28.dp, top = 8.dp)
                                .testTag("moon_phase_slider")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 36.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        repeat(7) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF636366))
                            )
                        }
                    }

                    Text(
                        text = "SCRUB THE LUNAR CYCLE · LIVE PHASE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF636366),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF1C1C1E))
                .padding(3.dp)
        ) {
            SegmentChip(
                label = "DAY",
                selected = !isWeekMode,
                onClick = { isWeekMode = false },
                testTag = "moon_day_toggle"
            )
            SegmentChip(
                label = "WEEK",
                selected = isWeekMode,
                onClick = { isWeekMode = true },
                testTag = "moon_week_toggle"
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = data.cityName.uppercase(),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF636366),
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SegmentChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = if (selected) Color(0xFF1C1C1E) else Color(0xFF8E8E93)
        )
    }
}

private data class MoonDayRow(
    val name: String,
    val dateLabel: String,
    val snap: MoonPhaseCalculator.MoonSnapshot
)

@Composable
private fun MetricLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF8E8E93),
            letterSpacing = 0.6.sp
        )
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
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF2C2C2E))
    )
}

private fun phaseNameFromScrub(fraction: Float): String {
    val f = fraction.coerceIn(0f, 1f)
    return when {
        f < 0.03f || f > 0.97f -> "NEW MOON"
        f < 0.22f -> "WAXING CRESCENT"
        f < 0.28f -> "FIRST QUARTER"
        f < 0.47f -> "WAXING GIBBOUS"
        f < 0.53f -> "FULL MOON"
        f < 0.72f -> "WANING GIBBOUS"
        f < 0.78f -> "LAST QUARTER"
        else -> "WANING CRESCENT"
    }
}
