package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherCondition
import com.example.ui.theme.LocalThemePalette
import com.example.util.rememberDropletPlayers
import kotlin.math.cos
import kotlin.math.sin

/**
 * Playful 0–100 outdoor "vibe" ring — Not Boring personality, not a scientific AQI.
 */
@Composable
fun VibeMeterCard(
    tempC: Float,
    humidity: Int,
    windMph: Float,
    precipChance: Int,
    uv: Float,
    condition: WeatherCondition,
    modifier: Modifier = Modifier
) {
    val palette = LocalThemePalette.current
    val feedback = rememberDropletPlayers()
    val score = remember(tempC, humidity, windMph, precipChance, uv) {
        NotBoringCopy.vibeScore(tempC, humidity, windMph, precipChance, uv)
    }
    val label = NotBoringCopy.vibeLabel(score)
    val tip = NotBoringCopy.vibeTip(score, condition)

    val animated = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animated.snapTo(0f)
        animated.animateTo(
            score / 100f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        feedback.plink()
    }

    val glow by rememberInfiniteTransition(label = "vibeGlow").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vibeGlowVal"
    )

    val ringColor = when {
        score >= 80 -> Color(0xFF34C759)
        score >= 60 -> Color(0xFF30B0C7)
        score >= 40 -> Color(0xFFFFCC00)
        else -> Color(0xFFFF9500)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.surface)
            .bouncyClick { feedback.chime() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(92.dp)
        ) {
            Canvas(modifier = Modifier.size(92.dp)) {
                val stroke = 10f
                val pad = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(pad, pad)
                drawArc(
                    color = palette.divider,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(ringColor.copy(alpha = 0.5f * glow), ringColor, ringColor)
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * animated.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                // Tip glow
                val ang = Math.toRadians((135f + 270f * animated.value).toDouble())
                val r = size.minDimension / 2f - pad
                val cx = size.width / 2f + r * cos(ang).toFloat()
                val cy = size.height / 2f + r * sin(ang).toFloat()
                drawCircle(ringColor.copy(alpha = 0.35f * glow), radius = 12f, center = Offset(cx, cy))
                drawCircle(Color.White, radius = 5f, center = Offset(cx, cy))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(animated.value * 100).toInt()}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = palette.primaryText
                )
                Text(
                    text = "VIBE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = palette.secondaryText
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.6.sp,
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = tip,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = palette.secondaryText,
                lineHeight = 18.sp
            )
        }
    }
}
