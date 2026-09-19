package dev.retza.mak.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MakAccent,
    onPrimary = Color.White,
    primaryContainer = MakAccentSoft,
    onPrimaryContainer = MakAccent,
    secondary = MakTeal,
    onSecondary = Color.White,
    secondaryContainer = MakTealSoft,
    onSecondaryContainer = MakTeal,
    tertiary = MakOrange,
    onTertiary = Color.White,
    tertiaryContainer = MakOrangeSoft,
    onTertiaryContainer = MakOrange,
    error = MakRed,
    onError = Color.White,
    errorContainer = MakRedSoft,
    onErrorContainer = MakRed,
    background = MakPaper,
    onBackground = MakInk,
    surface = MakPaper,
    onSurface = MakInk,
    surfaceVariant = MakSoft,
    onSurfaceVariant = MakMuted,
    outline = MakLine,
    outlineVariant = MakFieldBorder,
    surfaceContainerLowest = MakPaper,
    surfaceContainerLow = MakSoft,
    surfaceContainer = MakSoft,
    surfaceContainerHigh = MakAccentSoft
)

private val DarkColorScheme = darkColorScheme(
    primary = MakAccentDark,
    onPrimary = MakBackgroundDark,
    primaryContainer = MakAccentSoftDark,
    onPrimaryContainer = MakAccentDark,
    secondary = MakTealDark,
    onSecondary = MakBackgroundDark,
    secondaryContainer = MakTealSoftDark,
    onSecondaryContainer = MakTealDark,
    tertiary = MakOrangeDark,
    onTertiary = MakBackgroundDark,
    tertiaryContainer = MakOrangeSoftDark,
    onTertiaryContainer = MakOrangeDark,
    error = Color(0xFFE8A0A4),
    onError = MakBackgroundDark,
    errorContainer = Color(0xFF5A2A30),
    onErrorContainer = Color(0xFFF3C4C7),
    background = MakBackgroundDark,
    onBackground = MakInkDark,
    surface = MakPaperDark,
    onSurface = MakInkDark,
    surfaceVariant = MakSoftDark,
    onSurfaceVariant = MakMutedDark,
    outline = MakLineDark,
    outlineVariant = MakLineDark,
    surfaceContainerLowest = MakPaperDark,
    surfaceContainerLow = MakSoftDark,
    surfaceContainer = MakSoftDark,
    surfaceContainerHigh = MakAccentSoftDark
)

private val MakShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun MAKTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = MakShapes,
        content = content
    )
}
