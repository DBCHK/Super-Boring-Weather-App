package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherForecastData
import kotlin.math.roundToInt

private data class AlertCard(
    val id: String,
    val title: String,
    val severity: String,
    val severityColor: Color,
    val summary: String,
    val action: String,
    val threatScore: Float,
    val expiresLabel: String
)

/**
 * Interactive ALERTS hub: radar-style threat ring, tappable cards, severity dial, acknowledge flow.
 */
@Composable
fun SevereWeatherAlertView(
    data: WeatherForecastData,
    modifier: Modifier = Modifier
) {
    val motion = rememberDeviceMotionState(intensity = 1.1f)
    val infinite = rememberInfiniteTransition(label = "alertPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val ringSweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4800, easing = LinearEasingCompat),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringSweep"
    )

    val alerts = remember(data.condition, data.cityName, data.precipChancePercent, data.windSpeedMph) {
        buildAlerts(data)
    }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var expandedId by remember { mutableStateOf<String?>(alerts.firstOrNull()?.id) }
    var acknowledged by remember { mutableStateOf(setOf<String>()) }
    // Interactive severity dial 0..1 (user can drag)
    var dialBoost by remember { mutableFloatStateOf(0f) }

    val selected = alerts.getOrNull(selectedIndex) ?: alerts.first()
    val threat = (selected.threatScore + dialBoost * 0.25f).coerceIn(0.05f, 1f)
    val threatAnim by animateFloatAsState(
        targetValue = threat,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 180f),
        label = "threat"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Threat radar hero ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A0A0A), Color(0xFF0A0A0B), Color(0xFF121214))
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        // Drag right/up raises perceived threat (interactive)
                        dialBoost = (dialBoost + drag.x / size.width * 0.55f - drag.y / size.height * 0.35f)
                            .coerceIn(-0.35f, 0.55f)
                    }
                }
                .testTag("alert_threat_radar"),
            contentAlignment = Alignment.Center
        ) {
            // Background weather glyph reacts to tilt
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = motion.offsetX * 28f
                        translationY = motion.offsetY * 18f
                        rotationZ = motion.yawDeg * 0.15f
                    }
            ) {
                ThreeDWeatherCanvas(
                    condition = when (data.condition) {
                        WeatherCondition.THUNDERSTORM -> WeatherCondition.THUNDERSTORM
                        WeatherCondition.SNOWY -> WeatherCondition.SNOWY
                        WeatherCondition.RAINY, WeatherCondition.HEAVY_RAIN -> WeatherCondition.HEAVY_RAIN
                        else -> WeatherCondition.CLOUDY
                    },
                    isDaytime = false,
                    modelScale = 1.1f,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Radar rings + threat arc (device tilt shifts center slightly)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = motion.offsetX * 12f
                        translationY = motion.offsetY * 10f
                    }
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f + 8f
                val r = size.minDimension * 0.34f

                for (i in 1..3) {
                    drawCircle(
                        color = Color(0xFFFF453A).copy(alpha = 0.12f * pulse),
                        radius = r * (0.45f + i * 0.28f),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f)
                    )
                }

                // Sweep arm
                val rad = Math.toRadians(ringSweep.toDouble())
                drawLine(
                    color = Color(0xFFFFB300).copy(alpha = 0.35f),
                    start = Offset(cx, cy),
                    end = Offset(
                        cx + kotlin.math.cos(rad).toFloat() * r * 1.25f,
                        cy + kotlin.math.sin(rad).toFloat() * r * 1.25f
                    ),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )

                // Threat arc
                drawArc(
                    color = selected.severityColor.copy(alpha = 0.9f),
                    startAngle = -90f,
                    sweepAngle = 360f * threatAnim,
                    useCenter = false,
                    topLeft = Offset(cx - r, cy - r),
                    size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = 6f,
                    center = Offset(cx, cy)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(threatAnim * 100).roundToInt()}",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Text(
                    text = "THREAT INDEX",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFFFFB300)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "DRAG TO SIMULATE · TILT PHONE",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF8E8E93)
                )
            }

            // Floating alert pill
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFB300).copy(alpha = pulse))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("severe_weather_alert_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFF1C1C1E),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = selected.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF1C1C1E),
                        maxLines = 1
                    )
                }
            }
        }

        // ── Interactive alert cards ─────────────────────────────────────
        Text(
            text = "ACTIVE ALERTS · TAP TO EXPAND",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            color = Color(0xFF8E8E93),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )

        alerts.forEachIndexed { index, alert ->
            val isSelected = index == selectedIndex
            val isExpanded = expandedId == alert.id
            val isAck = acknowledged.contains(alert.id)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) Color(0xFF1C1C1E) else Color.White)
                    .border(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = Color(0xFFE5E5EA),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable {
                        selectedIndex = index
                        expandedId = if (isExpanded) null else alert.id
                    }
                    .padding(14.dp)
                    .testTag("alert_card_${alert.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isAck) Color(0xFF34C759) else alert.severityColor)
                        )
                        Column {
                            Text(
                                text = alert.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) Color.White else Color(0xFF1C1C1E)
                            )
                            Text(
                                text = "${alert.severity} · ${alert.expiresLabel}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { rotationZ = if (isExpanded) 180f else 0f }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (alert.threatScore + if (isSelected) dialBoost * 0.2f else 0f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = alert.severityColor,
                    trackColor = if (isSelected) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
                )

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = alert.summary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) Color(0xFFD1D1D6) else Color(0xFF3A3A3C),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF2C2C2E) else Color(0xFFF2F2F7))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = alert.action,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) Color.White else Color(0xFF1C1C1E)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isAck) Color(0xFF34C759) else Color(0xFFFFB300))
                                .clickable {
                                    acknowledged = if (isAck) {
                                        acknowledged - alert.id
                                    } else {
                                        acknowledged + alert.id
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("alert_ack_${alert.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF1C1C1E),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isAck) "ACKNOWLEDGED" else "I UNDERSTAND · ACK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF1C1C1E)
                            )
                        }
                    }
                }
            }
        }

        // Meta strip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF1C1C1E))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetaRow("ISSUED BY", "NATIONAL WEATHER SERVICE")
            Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2E)))
            MetaRow("LOCATION", data.cityName.uppercase())
            Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2E)))
            MetaRow(
                "ACK'D",
                "${acknowledged.size} / ${alerts.size}"
            )
            Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2E)))
            MetaRow(
                "TIP",
                "Drag radar · tilt phone · expand cards"
            )
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

// LinearEasing alias for animation import clarity in this file
private val LinearEasingCompat = androidx.compose.animation.core.LinearEasing

private fun buildAlerts(data: WeatherForecastData): List<AlertCard> {
    val city = data.cityName
    val primary = when (data.condition) {
        WeatherCondition.THUNDERSTORM -> AlertCard(
            id = "storm",
            title = "THUNDERSTORM WARNING",
            severity = "SEVERE",
            severityColor = Color(0xFFFF453A),
            summary = "Damaging winds and frequent lightning possible near $city. Outdoor activities unsafe until the cell passes.",
            action = "SEEK INDOOR SHELTER NOW",
            threatScore = 0.88f,
            expiresLabel = "EXPIRES 3H"
        )
        WeatherCondition.HEAVY_RAIN, WeatherCondition.RAINY -> AlertCard(
            id = "flood",
            title = "FLOOD ADVISORY",
            severity = "MODERATE",
            severityColor = Color(0xFF007AFF),
            summary = "Heavy rain rates near $city. Ponding on roads and poor visibility expected. ${data.precipChancePercent}% precip chance in the window.",
            action = "AVOID LOW-LYING ROADS",
            threatScore = 0.62f,
            expiresLabel = "EXPIRES 6H"
        )
        WeatherCondition.SNOWY -> AlertCard(
            id = "snow",
            title = "WINTER WEATHER",
            severity = "HIGH",
            severityColor = Color(0xFF64D2FF),
            summary = "Snow accumulation and slick surfaces around $city. Travel delays likely; wind ${data.windSpeedMph} mph.",
            action = "ALLOW EXTRA TRAVEL TIME",
            threatScore = 0.74f,
            expiresLabel = "EXPIRES 12H"
        )
        else -> AlertCard(
            id = "watch",
            title = "WEATHER WATCH",
            severity = "ADVISORY",
            severityColor = Color(0xFFFFB300),
            summary = "Conditions around $city are changeable. Stay weather-aware; UV ${data.uvIndex.roundToInt()} and AQI ${data.airQualityIndex}.",
            action = "MONITOR CONDITIONS",
            threatScore = 0.35f,
            expiresLabel = "ONGOING"
        )
    }

    val air = AlertCard(
        id = "air",
        title = "AIR QUALITY NOTE",
        severity = if (data.airQualityIndex > 100) "ELEVATED" else "GOOD",
        severityColor = if (data.airQualityIndex > 100) Color(0xFFFF9500) else Color(0xFF34C759),
        summary = "AQI sits at ${data.airQualityIndex} for $city. Sensitive groups should limit prolonged outdoor exertion if elevated.",
        action = if (data.airQualityIndex > 100) "LIMIT OUTDOOR EXERTION" else "CONDITIONS FAVORABLE",
        threatScore = (data.airQualityIndex / 200f).coerceIn(0.1f, 0.9f),
        expiresLabel = "TODAY"
    )

    val wind = AlertCard(
        id = "wind",
        title = "WIND BRIEFING",
        severity = if (data.windSpeedMph > 20f) "GUSTY" else "CALM",
        severityColor = if (data.windSpeedMph > 20f) Color(0xFFAF52DE) else Color(0xFF8E8E93),
        summary = "Sustained winds near ${data.windSpeedMph} mph. Secure loose outdoor items if gusts pick up.",
        action = if (data.windSpeedMph > 20f) "SECURE LOOSE OBJECTS" else "NO ACTION NEEDED",
        threatScore = (data.windSpeedMph / 40f).coerceIn(0.08f, 0.85f),
        expiresLabel = "NEXT 12H"
    )

    return listOf(primary, air, wind)
}
