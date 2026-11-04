package com.example.mobiledevelopmentproject.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// De huisstijl is enkel licht, dus er is geen donker kleurenschema.
// De surfaceContainer-kleuren staan er ook bij: anders krijgen kaarten en de
// navigatiebalk de paarse standaardtint van Material.
private val RestjeColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,
    secondary = GreenDark,
    onSecondary = SurfaceWhite,
    secondaryContainer = GreenLight,
    onSecondaryContainer = GreenDark,
    tertiary = GreenDark,
    onTertiary = SurfaceWhite,
    tertiaryContainer = GreenLight,
    onTertiaryContainer = GreenDark,
    background = BackgroundGray,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark,
    surfaceVariant = BackgroundGray,
    onSurfaceVariant = TextGray,
    surfaceContainerLowest = SurfaceWhite,
    surfaceContainerLow = SurfaceWhite,
    surfaceContainer = SurfaceWhite,
    surfaceContainerHigh = SurfaceWhite,
    surfaceContainerHighest = BackgroundGray,
    outline = BorderGray,
    outlineVariant = BorderGray,
    error = ErrorText,
    onError = SurfaceWhite,
    errorContainer = ErrorBackground,
    onErrorContainer = ErrorText
)

@Composable
fun MobileDevelopmentProjectTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RestjeColorScheme,
        typography = Typography,
        content = content
    )
}
