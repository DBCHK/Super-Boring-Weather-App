package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HourlyForecast
import com.example.ui.theme.NbColors
import com.example.util.rememberDropletPlayers
import kotlin.math.roundToInt

/**
 * Not Boring–style day scrubber:
 * high · blue handle · low, time labels under the track, drag to time-travel.
 */
@Composable
fun TimelineScrubber(
    hourlyList: List<HourlyForecast>,
    selectedIndex: Int,
    highTemp: Int,
    lowTemp: Int,
    onHourSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0xFFE5E5EA),
    activeColor: Color = Color(0xFF1C1C1E),
    labelColor: Color = Color(0xFF8E8E93),
    handleColor: Color = NbColors.ScrubHandle
) {
    if (hourlyList.isEmpty()) return

    val feedback = rememberDropletPlayers()
    val lastIndex = remember { intArrayOf(selectedIndex) }
    val density = LocalDensity.current
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val fraction = selectedIndex.toFloat() / (hourlyList.size - 1).coerceAtLeast(1)
    val pillOffset by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (isDragging) Spring.StiffnessHigh else Spring.StiffnessMediumLow
        ),
        label = "scrubPill"
    )

    val pillBounce = remember { Animatable(1f) }
    LaunchedEffect(selectedIndex) {
        pillBounce.snapTo(0.86f)
        pillBounce.animateTo(
            1f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    val dragPulse by rememberInfiniteTransition(label = "dragPulse").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dragPulseVal"
    )

    val trackScale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "trackScale"
    )

    val selectedHour = hourlyList.getOrNull(selectedIndex)
    val hourLabel = selectedHour?.timeLabel ?: "NOW"
    val hourCondition = selectedHour?.condition?.label?.take(14) ?: ""

    fun selectFromX(x: Float, width: Float) {
        if (width <= 0f || hourlyList.isEmpty()) return
        val f = (x / width).coerceIn(0f, 1f)
        val newIndex = (f * (hourlyList.size - 1)).roundToInt()
        if (newIndex != lastIndex[0]) {
            lastIndex[0] = newIndex
            feedback.scrubTick(newIndex)
            onHourSelected(newIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = "$hourLabel · $hourCondition",
            transitionSpec = {
                (fadeIn(tween(160)) + slideInVertically { it / 3 } + scaleIn(initialScale = 0.92f)) togetherWith
                    (fadeOut(tween(120)) + slideOutVertically { -it / 3 } + scaleOut(targetScale = 0.92f))
            },
            label = "hourReadout",
            modifier = Modifier.padding(bottom = 10.dp)
        ) { text ->
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.6.sp,
                color = if (isDragging) handleColor else labelColor
            )
        }

        // High · track · Low (reference layout)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = trackScale
                    scaleY = trackScale
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // High temp
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$highTemp°",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = activeColor
                )
                Text(
                    text = "HIGH",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = labelColor
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(trackColor)
                    .onSizeChanged { trackWidthPx = it.width.toFloat() }
                    .pointerInput(hourlyList) {
                        detectTapGestures { offset ->
                            selectFromX(offset.x, size.width.toFloat())
                        }
                    }
                    .pointerInput(hourlyList) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onDrag = { change, _ ->
                                change.consume()
                                selectFromX(change.position.x, size.width.toFloat())
                            }
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                // Soft fill from start → handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pillOffset.coerceIn(0.02f, 1f))
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(handleColor.copy(alpha = 0.22f))
                )

                // Tick marks
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tickCount = hourlyList.size.coerceAtMost(9)
                    repeat(tickCount) { idx ->
                        val mapped = (idx.toFloat() / (tickCount - 1).coerceAtLeast(1) *
                            (hourlyList.size - 1)).roundToInt()
                        val selected = mapped == selectedIndex
                        Box(
                            modifier = Modifier
                                .size(if (selected) 6.dp else 3.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) handleColor
                                    else labelColor.copy(alpha = 0.35f)
                                )
                        )
                    }
                }

                // Signature blue handle
                val handleSizePx = with(density) { 28.dp.toPx() }
                val travel = (trackWidthPx - handleSizePx - with(density) { 12.dp.toPx() })
                    .coerceAtLeast(0f)
                val bounce = pillBounce.value

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (6.dp.toPx() + pillOffset * travel).roundToInt(),
                                y = 0
                            )
                        }
                        .graphicsLayer {
                            val s = bounce * if (isDragging) 1.12f else 1f
                            scaleX = s
                            scaleY = s
                            shadowElevation = if (isDragging) 12f else 6f
                            alpha = if (isDragging) 0.9f + 0.1f * dragPulse else 1f
                        }
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(handleColor)
                )
            }

            // Low temp
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$lowTemp°",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = activeColor
                )
                Text(
                    text = "LOW",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = labelColor
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sample time labels like the reference (NOW · 12P · 6 · 12A · 6)
            val labels = remember(hourlyList) {
                val n = hourlyList.size
                if (n <= 1) listOf("NOW")
                else {
                    listOf(0, n / 4, n / 2, (3 * n) / 4, n - 1)
                        .distinct()
                        .map { hourlyList[it].timeLabel }
                }
            }
            labels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = labelColor
                )
            }
        }

        Text(
            text = if (isDragging) "SCRUBBING…" else "DRAG TO TIME-TRAVEL",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp,
            color = if (isDragging) handleColor else labelColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
