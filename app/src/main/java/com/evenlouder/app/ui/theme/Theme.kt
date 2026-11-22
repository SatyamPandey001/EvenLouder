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

// Soft Tactile Skeuomorphic Dark Theme Palette (Base Canvas #1E2229)
val DarkBackground = Color(0xFF1E2229)
val DarkSurface = Color(0xFF1E2229)
val DarkSurfaceVariant = Color(0xFF262C36)
val DarkPrimary = Color(0xFF00E5FF)       // Electric Cyan
val DarkSecondary = Color(0xFFFFB300)     // Studio Amber
val DarkTertiary = Color(0xFF10B981)      // Signal Green
val DarkOnBackground = Color(0xFFF2F5F8)  // High-contrast sculpted text
val DarkOnSurface = Color(0xFFF2F5F8)
val DarkOnSurfaceVariant = Color(0xFF9AA5B5)
val DarkBorder = Color(0x33FFFFFF)

// Soft Tactile Skeuomorphic Light Theme Palette (Base Canvas #EEF1F5)
val LightBackground = Color(0xFFEEF1F5)
val LightSurface = Color(0xFFEEF1F5)
val LightSurfaceVariant = Color(0xFFE2E7ED)
val LightPrimary = Color(0xFF0284C7)      // Sky 600 (high contrast against soft canvas)
val LightSecondary = Color(0xFFD97706)    // Amber 600
val LightTertiary = Color(0xFF059669)     // Emerald 600
val LightOnBackground = Color(0xFF131924) // Deep Slate (AAA contrast >= 7:1)
val LightOnSurface = Color(0xFF131924)
val LightOnSurfaceVariant = Color(0xFF546377) // Slate 600
val LightBorder = Color(0x2E718096)

@Immutable
data class ExtendedColors(
    val accentCyan: Color,
    val accentAmber: Color,
    val signalGreen: Color,
    val cardBorder: Color,
    val visualizerGrid: Color,
    val isDark: Boolean = true,
    val tactileLightShadow: Color = TactileTokens.DarkShadowLight,
    val tactileDarkShadow: Color = TactileTokens.DarkShadowDark,
    val tactileSpecular: Color = TactileTokens.DarkSpecularBorder,
    val primaryCtaGradient: List<Color> = TactileTokens.PrimaryCtaGradientDark
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        accentCyan = DarkPrimary,
        accentAmber = DarkSecondary,
        signalGreen = DarkTertiary,
        cardBorder = DarkBorder,
        visualizerGrid = Color(0x1A00E5FF),
        isDark = true,
        tactileLightShadow = TactileTokens.DarkShadowLight,
        tactileDarkShadow = TactileTokens.DarkShadowDark,
        tactileSpecular = TactileTokens.DarkSpecularBorder,
        primaryCtaGradient = TactileTokens.PrimaryCtaGradientDark
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
            visualizerGrid = Color(0x1A00E5FF),
            isDark = true,
            tactileLightShadow = TactileTokens.DarkShadowLight,
            tactileDarkShadow = TactileTokens.DarkShadowDark,
            tactileSpecular = TactileTokens.DarkSpecularBorder,
            primaryCtaGradient = TactileTokens.PrimaryCtaGradientDark
        )
    } else {
        ExtendedColors(
            accentCyan = LightPrimary,
            accentAmber = LightSecondary,
            signalGreen = LightTertiary,
            cardBorder = LightBorder,
            visualizerGrid = Color(0x1A0284C7),
            isDark = false,
            tactileLightShadow = TactileTokens.LightShadowLight,
            tactileDarkShadow = TactileTokens.LightShadowDark,
            tactileSpecular = TactileTokens.LightSpecularBorder,
            primaryCtaGradient = TactileTokens.PrimaryCtaGradientLight
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
