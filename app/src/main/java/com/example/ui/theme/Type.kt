package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Brutalist / Not Boring type scale.
 * Display = heavy sans. Labels = mono all-caps energy.
 */
val NbDisplay = FontFamily.SansSerif
val NbMono = FontFamily.Monospace

val Typography =
    Typography(
        displayLarge = TextStyle(
            fontFamily = NbDisplay,
            fontWeight = FontWeight.Black,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = NbDisplay,
            fontWeight = FontWeight.Black,
            fontSize = 44.sp,
            lineHeight = 50.sp,
            letterSpacing = (-0.3).sp
        ),
        displaySmall = TextStyle(
            fontFamily = NbDisplay,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            lineHeight = 38.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = NbDisplay,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            lineHeight = 34.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = NbDisplay,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = NbDisplay,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        titleLarge = TextStyle(
            fontFamily = NbDisplay,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        titleMedium = TextStyle(
            fontFamily = NbMono,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.4.sp
        ),
        titleSmall = TextStyle(
            fontFamily = NbMono,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.6.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = NbDisplay,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = NbDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodySmall = TextStyle(
            fontFamily = NbMono,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = NbMono,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.0.sp
        ),
        labelMedium = TextStyle(
            fontFamily = NbMono,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.8.sp
        ),
        labelSmall = TextStyle(
            fontFamily = NbMono,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            letterSpacing = 1.1.sp
        )
    )
