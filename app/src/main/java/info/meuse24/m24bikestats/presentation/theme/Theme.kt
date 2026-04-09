package info.meuse24.m24bikestats.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import info.meuse24.m24bikestats.domain.model.DisplayMode

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = Green30,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = ForestSecondary80,
    onSecondary = Color(0xFF223527),
    secondaryContainer = ForestSecondary30,
    onSecondaryContainer = ForestSecondary90,
    tertiary = TealAccent80,
    onTertiary = Color(0xFF043542),
    tertiaryContainer = TealAccent30,
    onTertiaryContainer = TealAccent90,
    error = Error80,
    onError = Color(0xFF690005),
    errorContainer = Error30,
    onErrorContainer = Error90,
    background = Neutral8,
    onBackground = Neutral90,
    surface = Neutral8,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    scrim = Color(0xFF000000),
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral17,
    inversePrimary = Green40,
    surfaceDim = Neutral8,
    surfaceBright = Neutral24,
    surfaceContainerLowest = Neutral6,
    surfaceContainerLow = Neutral10,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral15,
    surfaceContainerHighest = Neutral20,
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Green90,
    onPrimaryContainer = Color(0xFF072112),
    secondary = ForestSecondary40,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = ForestSecondary90,
    onSecondaryContainer = Color(0xFF0C1F13),
    tertiary = TealAccent40,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = TealAccent90,
    onTertiaryContainer = Color(0xFF001F28),
    error = Error40,
    onError = Color(0xFFFFFFFF),
    errorContainer = Error90,
    onErrorContainer = Color(0xFF410002),
    background = Neutral98,
    onBackground = Neutral10,
    surface = Neutral98,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    scrim = Color(0xFF000000),
    inverseSurface = Neutral17,
    inverseOnSurface = Neutral94,
    inversePrimary = Green80,
    surfaceDim = Neutral87,
    surfaceBright = Neutral98,
    surfaceContainerLowest = Neutral99,
    surfaceContainerLow = Neutral96,
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral92,
    surfaceContainerHighest = Neutral90,
)

@Composable
fun M24BikeStatsTheme(
    displayMode: DisplayMode = DisplayMode.AUTOMATIC,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (displayMode) {
        DisplayMode.AUTOMATIC -> isSystemInDarkTheme()
        DisplayMode.LIGHT -> false
        DisplayMode.DARK -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
