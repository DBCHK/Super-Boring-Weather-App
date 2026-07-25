package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Main screen theme modes. */
enum class AppThemeMode {
    LIGHT,
    YELLOW,
    DARK
}

/**
 * Visual tokens so 3D elements, particles, and chrome stay readable per theme.
 * Dark mode uses light/white surfaces with soft shadows for depth.
 */
@Immutable
data class ThemePalette(
    val background: Color,
    val primaryText: Color,
    val secondaryText: Color,
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
    val isDark: Boolean
) {
    companion object {
        fun forMode(mode: AppThemeMode): ThemePalette = when (mode) {
            AppThemeMode.LIGHT -> ThemePalette(
                background = Color(0xFFF2F2F7),
                primaryText = Color(0xFF1C1C1E),
                secondaryText = Color(0xFF8E8E93),
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
                isDark = false
            )
            AppThemeMode.YELLOW -> ThemePalette(
                background = Color(0xFFFFB300),
                primaryText = Color(0xFF1C1C1E),
                secondaryText = Color(0xFF3A3A3C),
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
                isDark = false
            )
            AppThemeMode.DARK -> ThemePalette(
                background = Color(0xFF1C1C1E),
                primaryText = Color.White,
                secondaryText = Color(0xFF8E8E93),
                chromeBg = Color(0xFF2C2C2E),
                chromeFg = Color.White,
                chromeMuted = Color(0xFF8E8E93),
                // White digits with cool grey depth so they pop on charcoal
                elementFill = Color(0xFFF5F5F7),
                elementShadow = Color(0xFF000000).copy(alpha = 0.55f),
                elementShade = Color(0xFFC7C7CC),
                elementHighlight = Color.White,
                particlePrimary = Color.White.copy(alpha = 0.28f),
                particleSecondary = Color(0xFF64D2FF).copy(alpha = 0.55f),
                scrubberTrack = Color(0xFF2C2C2E),
                scrubberActive = Color.White,
                isDark = true
            )
        }
    }
}
