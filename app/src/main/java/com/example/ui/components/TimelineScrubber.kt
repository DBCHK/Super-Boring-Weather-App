package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import com.example.util.rememberDropletPlayers
import kotlin.math.roundToInt

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
    labelColor: Color = Color(0xFF8E8E93)
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

    // Pop the pill when hour changes
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
        targetValue = if (isDragging) 1.04f else 1f,
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated hour readout — flips when scrubbing
        AnimatedContent(
            targetState = "$hourLabel · $hourCondition",
            transitionSpec = {
                (fadeIn(tween(160)) + slideInVertically { it / 3 } + scaleIn(initialScale = 0.92f)) togetherWith
                    (fadeOut(tween(120)) + slideOutVertically { -it / 3 } + scaleOut(targetScale = 0.92f))
            },
            label = "hourReadout",
            modifier = Modifier.padding(bottom = 8.dp)
        ) { text ->
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.6.sp,
                color = if (isDragging) activeColor else labelColor
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = trackScale
                    scaleY = trackScale
                }
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    if (isDragging) {
                        activeColor.copy(alpha = 0.10f).compositeOverTrack(trackColor)
                    } else {
                        trackColor
                    }
                )
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
            // Track ticks with expand + glow on selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tickCount = hourlyList.size.coerceAtMost(12)
                repeat(tickCount) { idx ->
                    val mapped = (idx.toFloat() / (tickCount - 1).coerceAtLeast(1) *
                        (hourlyList.size - 1)).roundToInt()
                    val selected = mapped == selectedIndex
                    val sizeDp by animateDpAsState(
                        targetValue = when {
                            selected && isDragging -> 12.dp
                            selected -> 10.dp
                            else -> 4.dp
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "tick$idx"
                    )
                    Box(contentAlignment = Alignment.Center) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(sizeDp + 10.dp)
                                    .graphicsLayer {
                                        alpha = if (isDragging) 0.35f * dragPulse else 0.22f
                                        scaleX = if (isDragging) 0.9f + 0.2f * dragPulse else 1f
                                        scaleY = scaleX
                                    }
                                    .clip(CircleShape)
                                    .background(activeColor.copy(alpha = 0.35f))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(sizeDp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) activeColor
                                    else labelColor.copy(alpha = 0.40f)
                                )
                        )
                    }
                }
            }

            // Floating high/low pill
            val pillBg = activeColor
            val pillFg = if (
                activeColor == Color.White ||
                activeColor == Color(0xFFF5F5F7)
            ) {
                Color(0xFF1C1C1E)
            } else {
                Color.White
            }
            val pillMuted = pillFg.copy(alpha = 0.55f)
            val pillWidthPx = with(density) { 112.dp.toPx() }
            val travel = (trackWidthPx - pillWidthPx - with(density) { 16.dp.toPx() })
                .coerceAtLeast(0f)
            val bounce = pillBounce.value

            Row(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (8.dp.toPx() + pillOffset * travel).roundToInt(),
                            y = 0
                        )
                    }
                    .graphicsLayer {
                        scaleX = bounce * if (isDragging) 1.06f else 1f
                        scaleY = bounce * if (isDragging) 1.08f else 1f
                        shadowElevation = if (isDragging) 10f else 4f
                    }
                    .width(112.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(pillBg)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${highTemp}°",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = pillFg
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "|",
                    fontSize = 12.sp,
                    color = pillMuted
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${lowTemp}°",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = pillMuted
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = hourlyList.firstOrNull()?.timeLabel ?: "NOW",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = labelColor
            )
            Text(
                text = if (isDragging) "SCRUBBING…" else "DRAG TO TIME-TRAVEL",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.6.sp,
                color = if (isDragging) activeColor else labelColor.copy(alpha = 0.75f)
            )
            Text(
                text = hourlyList.lastOrNull()?.timeLabel ?: "—",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = labelColor
            )
        }
    }
}

/** Simple opaque composite so drag tint sits on the track without transparency mud. */
private fun Color.compositeOverTrack(track: Color): Color {
    val a = alpha.coerceIn(0f, 1f)
    return Color(
        red = red * a + track.red * (1f - a),
        green = green * a + track.green * (1f - a),
        blue = blue * a + track.blue * (1f - a),
        alpha = 1f
    )
}
