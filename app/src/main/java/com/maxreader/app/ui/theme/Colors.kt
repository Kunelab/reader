package com.maxreader.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Theme definitions
enum class AppTheme(val label: String) {
    DARK("Dark"),
    AMOLED("AMOLED Black"),
    SEPIA("Sepia"),
    LIGHT("Light")
}

data class ThemeColors(
    val background: Color,
    val surface: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val orpColor: Color
)

val DarkThemeColors = ThemeColors(
    background = Color(0xFF1A1A2E),
    surface = Color(0xFF16213E),
    accent = Color(0xFFE94560),
    textPrimary = Color(0xFFEEEEEE),
    textSecondary = Color(0xFF888888),
    textMuted = Color(0xFF555555),
    orpColor = Color(0xFFE94560)
)

val AmoledThemeColors = ThemeColors(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    accent = Color(0xFFE94560),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF777777),
    textMuted = Color(0xFF444444),
    orpColor = Color(0xFFE94560)
)

val SepiaThemeColors = ThemeColors(
    background = Color(0xFFF4ECD8),
    surface = Color(0xFFE8DCC8),
    accent = Color(0xFF8B4513),
    textPrimary = Color(0xFF3E2723),
    textSecondary = Color(0xFF6D4C41),
    textMuted = Color(0xFFA1887F),
    orpColor = Color(0xFFD84315)
)

val LightThemeColors = ThemeColors(
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFEEEEEE),
    accent = Color(0xFFE94560),
    textPrimary = Color(0xFF212121),
    textSecondary = Color(0xFF757575),
    textMuted = Color(0xFFBDBDBD),
    orpColor = Color(0xFFE94560)
)

fun themeColorsFor(theme: AppTheme): ThemeColors = when (theme) {
    AppTheme.DARK -> DarkThemeColors
    AppTheme.AMOLED -> AmoledThemeColors
    AppTheme.SEPIA -> SepiaThemeColors
    AppTheme.LIGHT -> LightThemeColors
}

val LocalThemeColors = compositionLocalOf { DarkThemeColors }

// Legacy color references — these point to default Dark theme
// Existing code references them but we'll migrate to LocalThemeColors
val DarkBackground = Color(0xFF1A1A2E)
val DarkSurface = Color(0xFF16213E)
val AccentRed = Color(0xFFE94560)
val AccentBlue = Color(0xFF0F3460)
val TextPrimary = Color(0xFFEEEEEE)
val TextSecondary = Color(0xFF888888)
val TextMuted = Color(0xFF555555)
val OrpColor = Color(0xFFE94560)
