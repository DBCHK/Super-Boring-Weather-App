package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val ColorWhite = Color.White

private val NotBoringLightScheme = lightColorScheme(
    primary = NbColors.Ink,
    onPrimary = ColorWhite,
    primaryContainer = NbColors.PaperMuted,
    onPrimaryContainer = NbColors.Ink,
    secondary = NbColors.Sky,
    onSecondary = ColorWhite,
    tertiary = NbColors.Yellow,
    onTertiary = NbColors.Ink,
    background = NbColors.Paper,
    onBackground = NbColors.Ink,
    surface = NbColors.PaperPure,
    onSurface = NbColors.Ink,
    surfaceVariant = NbColors.PaperMuted,
    onSurfaceVariant = NbColors.Mist,
    error = NbColors.Danger,
    onError = ColorWhite,
    outline = NbColors.PaperLine
)

private val NotBoringYellowScheme = lightColorScheme(
    primary = NbColors.Ink,
    onPrimary = NbColors.Yellow,
    primaryContainer = NbColors.Ink,
    onPrimaryContainer = ColorWhite,
    secondary = NbColors.Ink,
    onSecondary = NbColors.Yellow,
    tertiary = NbColors.ScrubHandle,
    onTertiary = ColorWhite,
    background = NbColors.Yellow,
    onBackground = NbColors.Ink,
    surface = NbColors.YellowSoft,
    onSurface = NbColors.Ink,
    surfaceVariant = NbColors.Ink.copy(alpha = 0.12f),
    onSurfaceVariant = Color(0xFF3A2A00),
    error = Color(0xFFB71C1C),
    onError = ColorWhite,
    outline = NbColors.Ink.copy(alpha = 0.2f)
)

private val NotBoringDarkScheme = darkColorScheme(
    primary = ColorWhite,
    onPrimary = NbColors.Ink,
    primaryContainer = NbColors.InkSoft,
    onPrimaryContainer = ColorWhite,
    secondary = NbColors.SkyBright,
    onSecondary = ColorWhite,
    tertiary = NbColors.Yellow,
    onTertiary = NbColors.Ink,
    background = NbColors.Ink,
    onBackground = ColorWhite,
    surface = NbColors.InkSoft,
    onSurface = ColorWhite,
    surfaceVariant = NbColors.InkElevated,
    onSurfaceVariant = NbColors.Mist,
    error = NbColors.DangerDark,
    onError = ColorWhite,
    outline = NbColors.InkElevated
)

@Composable
fun NotBoringWeatherTheme(
    mode: AppThemeMode = AppThemeMode.YELLOW,
    content: @Composable () -> Unit
) {
    val palette = ThemePalette.forMode(mode)
    val colorScheme = when (mode) {
        AppThemeMode.LIGHT -> NotBoringLightScheme
        AppThemeMode.YELLOW -> NotBoringYellowScheme
        AppThemeMode.DARK -> NotBoringDarkScheme
    }

    CompositionLocalProvider(
        LocalThemePalette provides palette,
        LocalAppThemeMode provides mode
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/** Legacy alias — routes into [NotBoringWeatherTheme]. */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    NotBoringWeatherTheme(
        mode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.YELLOW,
        content = content
    )
}
