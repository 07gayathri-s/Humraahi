package com.humraahi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SunsetOrange80,
    onPrimary = OnSunsetOrange80,
    primaryContainer = DarkSunsetOrangeContainer,
    onPrimaryContainer = OnDarkSunsetOrangeContainer,
    secondary = SunsetPink80,
    onSecondary = OnSunsetPink80,
    secondaryContainer = DarkSunsetPinkContainer,
    onSecondaryContainer = OnDarkSunsetPinkContainer,
    tertiary = OceanBlue80,
    onTertiary = OnOceanBlue80,
    tertiaryContainer = DarkOceanBlueContainer,
    onTertiaryContainer = OnDarkOceanBlueContainer,
    background = NightOceanBackground,
    onBackground = NightOceanOnSurface,
    surface = NightOceanSurface,
    onSurface = NightOceanOnSurface,
    surfaceVariant = NightOceanSurfaceVariant,
    onSurfaceVariant = NightOceanOnSurfaceVariant,
    outline = NightOceanOutline
)

private val LightColorScheme = lightColorScheme(
    primary = SunsetOrange40,
    onPrimary = OnSunsetOrange40,
    primaryContainer = SunsetOrange90,
    onPrimaryContainer = OnSunsetOrange90,
    secondary = SunsetPink40,
    onSecondary = OnSunsetPink40,
    secondaryContainer = SunsetPink90,
    onSecondaryContainer = OnSunsetPink90,
    tertiary = OceanBlue40,
    onTertiary = OnOceanBlue40,
    tertiaryContainer = OceanBlue90,
    onTertiaryContainer = OnOceanBlue90,
    background = LagoonBackground,
    onBackground = OceanInk,
    surface = SeaFoamSurface,
    onSurface = OceanInk,
    surfaceVariant = AquaSurfaceVariant,
    onSurfaceVariant = OceanInkVariant,
    outline = AquaOutline
)

@Composable
fun HumraahiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
