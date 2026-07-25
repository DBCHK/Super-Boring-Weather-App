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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherForecastData

@Composable
fun SevereWeatherAlertView(
    data: WeatherForecastData,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAlert")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val alertTitle = when (data.condition) {
        WeatherCondition.THUNDERSTORM -> "! THUNDERSTORM WARNING !"
        WeatherCondition.HEAVY_RAIN, WeatherCondition.RAINY -> "! SEVERE RAIN & FLOOD ADVISORY !"
        WeatherCondition.SNOWY -> "! HEAVY SNOWFALL WARNING !"
        else -> "! WEATHER WATCH & AIR ADVISORY !"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dramatic 3D Weather Canvas with Thunderstorm & Lightning
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF18181A)),
            contentAlignment = Alignment.Center
        ) {
            ThreeDWeatherCanvas(
                condition = WeatherCondition.THUNDERSTORM,
                isDaytime = false,
                modifier = Modifier.fillMaxSize()
            )

            // Bright Yellow Alert Pill Overlay (Exact matching Image 4)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFFFB300).copy(alpha = pulseAlpha))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .testTag("severe_weather_alert_pill"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFF1C1C1E),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = alertTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF1C1C1E)
                    )
                }
            }
        }

        // Severe Weather Alert Information Table
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C1C1E))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "ISSUED BY", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                Text(
                    text = "NATIONAL WEATHER SERVICE",
                    fontSize = 12.sp,
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
                Text(text = "LOCATION", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                Text(
                    text = data.cityName.uppercase(),
                    fontSize = 12.sp,
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
                Text(text = "SEVERITY", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                Text(
                    text = "HIGH / IMMEDIATE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFF453A)
                )
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2E)))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "INSTRUCTIONS", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8E8E93))
                Text(
                    text = "SEEK INDOOR SHELTER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFFB300)
                )
            }
        }
    }
}
