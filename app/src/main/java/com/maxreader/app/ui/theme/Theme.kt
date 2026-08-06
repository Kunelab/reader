package com.maxreader.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun MaxReaderTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit
) {
    val colors = themeColorsFor(appTheme)

    val colorScheme = when (appTheme) {
        AppTheme.LIGHT, AppTheme.SEPIA -> lightColorScheme(
            primary = colors.accent,
            secondary = AccentBlue,
            background = colors.background,
            surface = colors.surface,
            onPrimary = colors.textPrimary,
            onSecondary = colors.textPrimary,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
        else -> darkColorScheme(
            primary = colors.accent,
            secondary = AccentBlue,
            background = colors.background,
            surface = colors.surface,
            onPrimary = colors.textPrimary,
            onSecondary = colors.textPrimary,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    }

    CompositionLocalProvider(LocalThemeColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
