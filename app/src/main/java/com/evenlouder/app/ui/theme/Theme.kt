package com.evenlouder.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF101216)
val DarkSurface = Color(0xFF1A1D24)
val DarkSurfaceVariant = Color(0xFF242832)
val NeonCyan = Color(0xFF00E5FF)
val NeonAmber = Color(0xFFFFB300)
val PureWhite = Color(0xFFFFFFFF)
val MutedGray = Color(0xFF8E95A5)

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonAmber,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.Black,
    onBackground = PureWhite,
    onSurface = PureWhite,
    onSurfaceVariant = MutedGray
)

@Composable
fun EvenLouderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
