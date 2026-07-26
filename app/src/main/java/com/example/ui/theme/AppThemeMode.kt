package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Main screen theme modes. */
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
    val isDark: Boolean
) {
    companion object {
        fun forMode(mode: AppThemeMode): ThemePalette = when (mode) {
            AppThemeMode.LIGHT -> ThemePalette(
                background = Color(0xFFF2F2F7),
                surface = Color(0xFFFFFFFF),
                surfaceElevated = Color(0xFFFFFFFF),
                primaryText = Color(0xFF1C1C1E),
                secondaryText = Color(0xFF8E8E93),
                tertiaryText = Color(0xFFAEAEB2),
                chromeBg = Color(0xFFE5E5EA),
                chromeFg = Color(0xFF1C1C1E),
                chromeMuted = Color(0xFF8E8E93),
                elementFill = Color(0xFF1C1C1E),
                elementShadow = Color(0xFFAEAEB2),
                elementShade = Color(0xFF3A3A3C),
                elementHighlight = Color(0xFF636366),
                particlePrimary = Color(0xFF1C1C1E).copy(alpha = 0.18f),
                particleSecondary = Color(0xFF007AFF).copy(alpha = 0.45f),
                scrubberTrack = Color(0xFFE5E5EA),
                scrubberActive = Color(0xFF1C1C1E),
                accent = Color(0xFF007AFF),
                onAccent = Color.White,
                danger = Color(0xFFFF3B30),
                border = Color(0xFFD1D1D6),
                divider = Color(0xFFE5E5EA),
                chipBg = Color(0xFFE5E5EA),
                chipSelectedBg = Color(0xFF1C1C1E),
                chipSelectedFg = Color.White,
                fieldBg = Color.White,
                fieldBorder = Color(0xFFE5E5EA),
                isDark = false
            )
            AppThemeMode.YELLOW -> ThemePalette(
                background = Color(0xFFFFB300),
                surface = Color(0xFFFFC107),
                surfaceElevated = Color(0xFFFFCA28),
                primaryText = Color(0xFF1C1C1E),
                secondaryText = Color(0xFF3A3A3C),
                tertiaryText = Color(0xFF5C5C60),
                chromeBg = Color(0xFF1C1C1E),
                chromeFg = Color.White,
                chromeMuted = Color.White.copy(alpha = 0.45f),
                elementFill = Color(0xFF1C1C1E),
                elementShadow = Color(0xFF8B6914),
                elementShade = Color(0xFF2C2C2E),
                elementHighlight = Color(0xFF3A3A3C),
                particlePrimary = Color(0xFF1C1C1E).copy(alpha = 0.22f),
                particleSecondary = Color(0xFF1C1C1E).copy(alpha = 0.35f),
                scrubberTrack = Color(0xFF1C1C1E).copy(alpha = 0.18f),
                scrubberActive = Color(0xFF1C1C1E),
                accent = Color(0xFF1C1C1E),
                onAccent = Color(0xFFFFB300),
                danger = Color(0xFFB71C1C),
                border = Color(0xFF1C1C1E).copy(alpha = 0.18f),
                divider = Color(0xFF1C1C1E).copy(alpha = 0.12f),
                chipBg = Color(0xFF1C1C1E).copy(alpha = 0.12f),
                chipSelectedBg = Color(0xFF1C1C1E),
                chipSelectedFg = Color.White,
                fieldBg = Color.White.copy(alpha = 0.92f),
                fieldBorder = Color(0xFF1C1C1E).copy(alpha = 0.2f),
                isDark = false
            )
            AppThemeMode.DARK -> ThemePalette(
                background = Color(0xFF1C1C1E),
                surface = Color(0xFF2C2C2E),
                surfaceElevated = Color(0xFF3A3A3C),
                primaryText = Color.White,
                secondaryText = Color(0xFF8E8E93),
                tertiaryText = Color(0xFF636366),
                chromeBg = Color(0xFF2C2C2E),
                chromeFg = Color.White,
                chromeMuted = Color(0xFF8E8E93),
                elementFill = Color(0xFFF5F5F7),
                elementShadow = Color(0xFF000000).copy(alpha = 0.55f),
                elementShade = Color(0xFFC7C7CC),
                elementHighlight = Color.White,
                particlePrimary = Color.White.copy(alpha = 0.28f),
                particleSecondary = Color(0xFF64D2FF).copy(alpha = 0.55f),
                scrubberTrack = Color(0xFF2C2C2E),
                scrubberActive = Color.White,
                accent = Color(0xFF0A84FF),
                onAccent = Color.White,
                danger = Color(0xFFFF453A),
                border = Color(0xFF3A3A3C),
                divider = Color(0xFF2C2C2E),
                chipBg = Color(0xFF2C2C2E),
                chipSelectedBg = Color.White,
                chipSelectedFg = Color(0xFF1C1C1E),
                fieldBg = Color(0xFF2C2C2E),
                fieldBorder = Color(0xFF3A3A3C),
                isDark = true
            )
        }
    }
}

val LocalThemePalette = staticCompositionLocalOf {
    ThemePalette.forMode(AppThemeMode.LIGHT)
}

val LocalAppThemeMode = staticCompositionLocalOf { AppThemeMode.LIGHT }
