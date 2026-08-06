package com.maxreader.app.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.maxreader.app.R

// Theme definitions
enum class AppTheme(@StringRes val labelRes: Int) {
    DARK(R.string.theme_dark),
    AMOLED(R.string.theme_amoled),
    SEPIA(R.string.theme_sepia),
    LIGHT(R.string.theme_light)
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

/** Material's secondary slot, which none of the four themes vary. */
val AccentBlue = Color(0xFF0F3460)
