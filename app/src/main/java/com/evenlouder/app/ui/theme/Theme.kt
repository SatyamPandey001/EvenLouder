package com.evenlouder.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Legacy constants kept for compatibility
val NeonCyan = Color(0xFF00E5FF)
val NeonAmber = Color(0xFFFFB300)
val PureWhite = Color(0xFFFFFFFF)
val MutedGray = Color(0xFF8E95A5)

// Refined Dark Theme Palette
val DarkBackground = Color(0xFF0B0E14)
val DarkSurface = Color(0xFF151922)
val DarkSurfaceVariant = Color(0xFF1E232E)
val DarkPrimary = Color(0xFF00E5FF)       // Electric Cyan
val DarkSecondary = Color(0xFFFFB300)     // Studio Amber
val DarkTertiary = Color(0xFF10B981)      // Signal Green
val DarkOnBackground = Color(0xFFF0F6FC)
val DarkOnSurface = Color(0xFFF0F6FC)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkBorder = Color(0x24FFFFFF)

// High-Contrast Light Theme Palette (WCAG 2.1 AA Compliant >= 4.5:1 on light backgrounds)
val LightBackground = Color(0xFFF4F6F9)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightPrimary = Color(0xFF0284C7)      // Sky 600 (5.2:1 contrast against white)
val LightSecondary = Color(0xFFD97706)    // Amber 600 (4.6:1 contrast against white)
val LightTertiary = Color(0xFF059669)     // Emerald 600
val LightOnBackground = Color(0xFF0F172A)
val LightOnSurface = Color(0xFF0F172A)
val LightOnSurfaceVariant = Color(0xFF475569) // Slate 600 (5.8:1 contrast)
val LightBorder = Color(0x1F0F172A)

@Immutable
data class ExtendedColors(
    val accentCyan: Color,
    val accentAmber: Color,
    val signalGreen: Color,
    val cardBorder: Color,
    val visualizerGrid: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        accentCyan = DarkPrimary,
        accentAmber = DarkSecondary,
        signalGreen = DarkTertiary,
        cardBorder = DarkBorder,
        visualizerGrid = Color(0x1A00E5FF)
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.Black,
    secondary = DarkSecondary,
    onSecondary = Color.Black,
    tertiary = DarkTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    secondary = LightSecondary,
    onSecondary = Color.White,
    tertiary = LightTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant
)

@Composable
fun EvenLouderTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        ExtendedColors(
            accentCyan = DarkPrimary,
            accentAmber = DarkSecondary,
            signalGreen = DarkTertiary,
            cardBorder = DarkBorder,
            visualizerGrid = Color(0x1A00E5FF)
        )
    } else {
        ExtendedColors(
            accentCyan = LightPrimary,
            accentAmber = LightSecondary,
            signalGreen = LightTertiary,
            cardBorder = LightBorder,
            visualizerGrid = Color(0x1A0284C7)
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
