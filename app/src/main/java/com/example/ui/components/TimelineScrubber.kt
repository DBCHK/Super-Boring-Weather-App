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
    modifier: Modifier = Modifier
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
                .background(Color(0xFFE5E5EA))
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
                            .background(if (idx == selectedIndex) Color(0xFF1C1C1E) else Color(0xFFA1A1A6))
                    )
                }
            }

            // Scrub Capsule Pill ("19 | 11" High / Low or Current Selected Hour Stats)
            val currentHourly = hourlyList.getOrNull(selectedIndex) ?: hourlyList.first()
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${highTemp}°",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Half moon / separator indicator
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
                    color = Color(0xFFA1A1A6)
                )
            }
        }

        // Horizontal Time Labels below timeline ("NOW", "12A", "3A", "6A", "9A", "12P", "3P", "6P")
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
                    color = if (isSelected) Color(0xFF1C1C1E) else Color(0xFF8E8E93)
                )
            }
        }
    }
}
