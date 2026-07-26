package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Main screen theme modes — Not Boring signature trio. */
enum class AppThemeMode {
    LIGHT,
    YELLOW,
    DARK
}

/**
 * Visual tokens so 3D elements, particles, chrome, sheets, and detail tabs
 * stay readable and consistent in light / yellow / dark.
 */
@Immutable
data class ThemePalette(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val chromeBg: Color,
    val chromeFg: Color,
    val chromeMuted: Color,
    /** Digit / primary 3D solid color */
    val elementFill: Color,
    /** Soft extrusion / shadow under 3D elements */
    val elementShadow: Color,
    /** Mid-tone shade for depth on 3D forms */
    val elementShade: Color,
    /** Highlight rim on 3D forms */
    val elementHighlight: Color,
    val particlePrimary: Color,
    val particleSecondary: Color,
    val scrubberTrack: Color,
    val scrubberActive: Color,
    val scrubberHandle: Color,
    val accent: Color,
    val onAccent: Color,
    val danger: Color,
    val border: Color,
    val divider: Color,
    val chipBg: Color,
    val chipSelectedBg: Color,
    val chipSelectedFg: Color,
    val fieldBg: Color,
    val fieldBorder: Color,
    /** Hero card surfaces used in live widgets / AQI */
    val cardDark: Color,
    val cardLight: Color,
    val liveDot: Color,
    val isDark: Boolean
) {
    companion object {
        fun forMode(mode: AppThemeMode): ThemePalette = when (mode) {
            AppThemeMode.LIGHT -> ThemePalette(
                background = NbColors.Paper,
                surface = NbColors.PaperPure,
                surfaceElevated = NbColors.PaperPure,
                primaryText = NbColors.Ink,
                secondaryText = NbColors.Mist,
                tertiaryText = NbColors.MistLight,
                chromeBg = NbColors.PaperMuted,
                chromeFg = NbColors.Ink,
                chromeMuted = NbColors.Mist,
                elementFill = NbColors.Ink,
                elementShadow = NbColors.MistLight,
                elementShade = NbColors.InkElevated,
                elementHighlight = NbColors.InkMuted,
                particlePrimary = NbColors.Ink.copy(alpha = 0.14f),
                particleSecondary = NbColors.Sky.copy(alpha = 0.40f),
                scrubberTrack = NbColors.PaperMuted,
                scrubberActive = NbColors.Ink,
                scrubberHandle = NbColors.ScrubHandle,
                accent = NbColors.Sky,
                onAccent = Color.White,
                danger = NbColors.Danger,
                border = NbColors.PaperLine,
                divider = NbColors.PaperMuted,
                chipBg = NbColors.PaperMuted,
                chipSelectedBg = NbColors.Ink,
                chipSelectedFg = Color.White,
                fieldBg = NbColors.PaperPure,
                fieldBorder = NbColors.PaperMuted,
                cardDark = NbColors.WidgetDark,
                cardLight = NbColors.WidgetLight,
                liveDot = NbColors.Live,
                isDark = false
            )
            AppThemeMode.YELLOW -> ThemePalette(
                // Signature Not Boring field — warm amber stage
                background = NbColors.Yellow,
                surface = Color.White.copy(alpha = 0.92f),
                surfaceElevated = NbColors.YellowHot,
                primaryText = NbColors.Ink,
                secondaryText = Color(0xFF3A2A00),
                tertiaryText = Color(0xFF5C4A12),
                chromeBg = NbColors.Ink,
                chromeFg = Color.White,
                chromeMuted = Color.White.copy(alpha = 0.45f),
                elementFill = NbColors.Ink,
                elementShadow = Color(0xFF8B6914),
                elementShade = NbColors.InkSoft,
                elementHighlight = NbColors.InkElevated,
                particlePrimary = NbColors.Ink.copy(alpha = 0.18f),
                particleSecondary = NbColors.Ink.copy(alpha = 0.28f),
                scrubberTrack = NbColors.Ink.copy(alpha = 0.16f),
                scrubberActive = NbColors.Ink,
                scrubberHandle = NbColors.ScrubHandle,
                accent = NbColors.Ink,
                onAccent = NbColors.Yellow,
                danger = Color(0xFFB71C1C),
                border = NbColors.Ink.copy(alpha = 0.16f),
                divider = NbColors.Ink.copy(alpha = 0.10f),
                chipBg = NbColors.Ink.copy(alpha = 0.10f),
                chipSelectedBg = NbColors.Ink,
                chipSelectedFg = Color.White,
                fieldBg = Color.White.copy(alpha = 0.94f),
                fieldBorder = NbColors.Ink.copy(alpha = 0.18f),
                cardDark = NbColors.WidgetDark,
                cardLight = NbColors.WidgetLight,
                liveDot = NbColors.Live,
                isDark = false
            )
            AppThemeMode.DARK -> ThemePalette(
                background = NbColors.Ink,
                surface = NbColors.InkSoft,
                surfaceElevated = NbColors.InkElevated,
                primaryText = Color.White,
                secondaryText = NbColors.Mist,
                tertiaryText = NbColors.InkMuted,
                chromeBg = NbColors.InkSoft,
                chromeFg = Color.White,
                chromeMuted = NbColors.Mist,
                elementFill = Color(0xFFF5F5F7),
                elementShadow = Color.Black.copy(alpha = 0.55f),
                elementShade = NbColors.MistFaint,
                elementHighlight = Color.White,
                particlePrimary = Color.White.copy(alpha = 0.22f),
                particleSecondary = NbColors.CyanGlow.copy(alpha = 0.50f),
                scrubberTrack = NbColors.InkSoft,
                scrubberActive = Color.White,
                scrubberHandle = NbColors.ScrubHandle,
                accent = NbColors.SkyBright,
                onAccent = Color.White,
                danger = NbColors.DangerDark,
                border = NbColors.InkElevated,
                divider = NbColors.InkSoft,
                chipBg = NbColors.InkSoft,
                chipSelectedBg = Color.White,
                chipSelectedFg = NbColors.Ink,
                fieldBg = NbColors.InkSoft,
                fieldBorder = NbColors.InkElevated,
                cardDark = NbColors.WidgetDark,
                cardLight = NbColors.WidgetLight,
                liveDot = NbColors.Live,
                isDark = true
            )
        }
    }
}

val LocalThemePalette = staticCompositionLocalOf {
    ThemePalette.forMode(AppThemeMode.YELLOW)
}

val LocalAppThemeMode = staticCompositionLocalOf { AppThemeMode.YELLOW }
