package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WeatherFooter(
    modifier: Modifier = Modifier,
    textColor: Color = Color(0xFF8E8E93)
) {
    val funMessages = remember {
        listOf(
            "It's always sunny somewhere! ☀️",
            "Don't let a little rain dampen your spirits. ☔",
            "Stay cool, stay hydrated! 🧊",
            "The clouds are just nature's curtains. ☁️",
            "Weather is just what happens while you're busy making plans.",
            "Expect the unexpected, especially the wind. 💨",
            "Keep your head in the clouds and your feet on the ground.",
            "A change in the weather is good for the soul."
        )
    }
    val randomMessage = remember { funMessages.random() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = randomMessage,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = textColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Made by Debabrata Chakraborty",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = textColor,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp
        )
    }
}
