package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HourlyForecast
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High | Low Capsule Bar and Scrub Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(trackColor)
                .pointerInput(hourlyList) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val trackWidth = size.width.toFloat()
                        if (trackWidth > 0 && hourlyList.isNotEmpty()) {
                            val currentFraction = selectedIndex.toFloat() / (hourlyList.size - 1).coerceAtLeast(1)
                            val deltaFraction = dragAmount.x / trackWidth
                            val newFraction = (currentFraction + deltaFraction).coerceIn(0f, 1f)
                            val newIndex = (newFraction * (hourlyList.size - 1)).roundToInt()
                            if (newIndex != selectedIndex) {
                                onHourSelected(newIndex)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Track ticks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                hourlyList.take(8).forEachIndexed { idx, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (idx == selectedIndex) 8.dp else 4.dp)
                            .clip(CircleShape)
                            .background(
                                if (idx == selectedIndex) activeColor
                                else labelColor.copy(alpha = 0.55f)
                            )
                    )
                }
            }

            // Scrub Capsule Pill (high / low)
            val pillBg = activeColor
            val pillFg = if (activeColor == Color.White || activeColor == Color(0xFFF5F5F7)) {
                Color(0xFF1C1C1E)
            } else {
                Color.White
            }
            val pillMuted = pillFg.copy(alpha = 0.55f)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(pillBg)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${highTemp}°",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = pillFg
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9500))
                )

                Text(
                    text = "${lowTemp}°",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = pillMuted
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val step = (hourlyList.size / 6).coerceAtLeast(1)
            for (i in 0 until hourlyList.size step step) {
                val item = hourlyList[i]
                val isSelected = i == selectedIndex
                Text(
                    text = item.timeLabel,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSelected) activeColor else labelColor
                )
            }
        }
    }
}
