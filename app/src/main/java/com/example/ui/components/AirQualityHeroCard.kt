package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalThemePalette
import com.example.ui.theme.NbColors
import com.example.util.MoonPhaseCalculator
import com.example.util.rememberDropletPlayers
import kotlin.math.roundToInt

/**
 * Not Boring–style AQI + moon pair (matches WidgetsReference cards).
 * Big bold status word, mono labels, high contrast cards.
 */
@Composable
fun AirQualityHeroCard(
    aqi: Int,
    modifier: Modifier = Modifier
) {
    val palette = LocalThemePalette.current
    val feedback = rememberDropletPlayers()
    val label = remember(aqi) { aqiWord(aqi) }
    val tip = remember(aqi) { aqiTip(aqi) }
    val dotColor = remember(aqi) { aqiDot(aqi) }
    val pulse by rememberInfiniteTransition(label = "aqiPulse").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aqiPulseVal"
    )

    val moon = remember { MoonPhaseCalculator.forDate() }
    val illum = (moon.illumination * 100).roundToInt().coerceIn(0, 100)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("air_quality_hero"),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // AQI card — white / elevated
        Column(
            modifier = Modifier
                .weight(1f)
                .height(148.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.cardLight)
                .bouncyClick { feedback.plink() }
                .padding(16.dp),
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
                        letterSpacing = 0.8.sp,
                        color = NbColors.Mist
                    )
                    Text(
                        text = "AQI:$aqi",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = NbColors.Ink
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .graphicsLayer { alpha = pulse }
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
            Column {
                Text(
                    text = label,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = NbColors.Ink,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tip,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NbColors.Mist,
                    maxLines = 2,
                    lineHeight = 13.sp
                )
            }
        }

        // Moon phase mini card — dark stage
        Column(
            modifier = Modifier
                .weight(1f)
                .height(148.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.cardDark)
                .bouncyClick { feedback.chime() }
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = moon.phaseName.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.6.sp,
                    color = NbColors.Mist,
                    maxLines = 1
                )
                Text(
                    text = "$illum%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                contentAlignment = Alignment.Center
            ) {
                ThreeDMoonCanvas(
                    illuminationPercent = illum,
                    isWaxing = moon.isWaxing,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
    }
}

private fun aqiWord(aqi: Int): String = when {
    aqi <= 50 -> "GOOD"
    aqi <= 100 -> "OKAY"
    aqi <= 150 -> "MEH"
    aqi <= 200 -> "ROUGH"
    else -> "YIKES"
}

private fun aqiTip(aqi: Int): String = when {
    aqi <= 50 -> "Breathe deep. Nature said yes."
    aqi <= 100 -> "Fine for most. Sensitive noses: maybe not."
    aqi <= 150 -> "Sensitive groups: take it easy outside."
    aqi <= 200 -> "Limit outdoor grind. Indoor arcs win."
    else -> "Stay in. Windows closed. Plot twist air."
}

private fun aqiDot(aqi: Int): Color = when {
    aqi <= 50 -> NbColors.Live
    aqi <= 100 -> NbColors.Warning
    aqi <= 150 -> NbColors.Orange
    else -> NbColors.Danger
}
