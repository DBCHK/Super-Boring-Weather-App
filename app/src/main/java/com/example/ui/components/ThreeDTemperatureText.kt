package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThreeDTemperatureText(
    temperatureValue: Int,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 120.sp,
    extrusionDepthDp: Dp = 6.dp,
    color: Color = Color(0xFF1C1C1E),
    shadowColor: Color = Color(0xFFD1D1D6)
) {
    val tempString = "$temperatureValue"

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Render 3D Extrusion Bevel Shadow Layers
        val steps = 8
        for (i in steps downTo 1) {
            val offsetX = (extrusionDepthDp.value * (i.toFloat() / steps)).dp
            val offsetY = (extrusionDepthDp.value * (i.toFloat() / steps)).dp
            
            Text(
                text = tempString,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = if (i == steps) shadowColor.copy(alpha = 0.4f) else shadowColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.offset(x = offsetX, y = offsetY)
            )
        }

        // Top Main Surface Text
        Text(
            text = tempString,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}
