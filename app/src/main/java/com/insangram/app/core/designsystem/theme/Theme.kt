package com.insangram.app.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Theme preference persisted in DataStore and applied at the root. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Accessibility preferences that components read directly, so reduced motion
 * and high contrast are honoured without threading parameters everywhere.
 */
data class InsangramAccessibility(
    val reducedMotion: Boolean = false,
    val highContrast: Boolean = false,
)

val LocalInsangramAccessibility = staticCompositionLocalOf { InsangramAccessibility() }

private val DarkColors = darkColorScheme(
    primary = InsangramPalette.Violet,
    onPrimary = InsangramPalette.White,
    primaryContainer = InsangramPalette.Indigo,
    onPrimaryContainer = InsangramPalette.White,
    secondary = InsangramPalette.Magenta,
    onSecondary = InsangramPalette.White,
    secondaryContainer = Color(0xFF3B1030),
    onSecondaryContainer = Color(0xFFFFD9E4),
    tertiary = InsangramPalette.Amber,
    onTertiary = InsangramPalette.Black,
    tertiaryContainer = Color(0xFF3D2A08),
    onTertiaryContainer = InsangramPalette.Yellow,
    background = InsangramPalette.NearBlack,
    onBackground = InsangramPalette.TextPrimaryDark,
    surface = InsangramPalette.NearBlack,
    onSurface = InsangramPalette.TextPrimaryDark,
    surfaceVariant = InsangramPalette.SurfaceDarkElevated,
    onSurfaceVariant = InsangramPalette.TextSecondaryDark,
    surfaceContainer = InsangramPalette.SurfaceDark,
    surfaceContainerHigh = InsangramPalette.SurfaceDarkElevated,
    outline = InsangramPalette.OutlineDark,
    outlineVariant = Color(0xFF221E2B),
    error = InsangramPalette.Danger,
    onError = InsangramPalette.White,
    errorContainer = Color(0xFF4A1214),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = InsangramPalette.Black,
)

private val LightColors = lightColorScheme(
    primary = InsangramPalette.Purple,
    onPrimary = InsangramPalette.White,
    primaryContainer = Color(0xFFEDDCFB),
    onPrimaryContainer = Color(0xFF2C0A52),
    secondary = InsangramPalette.Magenta,
    onSecondary = InsangramPalette.White,
    secondaryContainer = Color(0xFFFFD9E4),
    onSecondaryContainer = Color(0xFF41031E),
    tertiary = Color(0xFFC26A00),
    onTertiary = InsangramPalette.White,
    tertiaryContainer = Color(0xFFFFE2B8),
    onTertiaryContainer = Color(0xFF3A2200),
    background = InsangramPalette.BackgroundLight,
    onBackground = InsangramPalette.TextPrimaryLight,
    surface = InsangramPalette.SurfaceLight,
    onSurface = InsangramPalette.TextPrimaryLight,
    surfaceVariant = InsangramPalette.SurfaceLightElevated,
    onSurfaceVariant = InsangramPalette.TextSecondaryLight,
    surfaceContainer = Color(0xFFF6F2FA),
    surfaceContainerHigh = Color(0xFFEFE9F6),
    outline = InsangramPalette.OutlineLight,
    outlineVariant = Color(0xFFEAE4F0),
    error = Color(0xFFC0242A),
    onError = InsangramPalette.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410004),
    scrim = InsangramPalette.Black,
)

/** High-contrast overrides applied when the accessibility toggle is on. */
private val DarkColorsHighContrast = DarkColors.copy(
    background = InsangramPalette.Black,
    surface = InsangramPalette.Black,
    onSurface = InsangramPalette.White,
    onSurfaceVariant = Color(0xFFE4DFEC),
    outline = Color(0xFF6F6880),
)

private val LightColorsHighContrast = LightColors.copy(
    background = InsangramPalette.White,
    onSurface = InsangramPalette.Black,
    onSurfaceVariant = Color(0xFF2A2433),
    outline = Color(0xFF554D63),
)

@Composable
fun InsangramTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    reducedMotion: Boolean = false,
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        // Dynamic colour is opt-in (Settings > Appearance) and only available
        // on Android 12+; the brand scheme is the default everywhere else.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark && highContrast -> DarkColorsHighContrast
        dark -> DarkColors
        highContrast -> LightColorsHighContrast
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(
        LocalInsangramSpacing provides InsangramSpacing(),
        LocalInsangramAccessibility provides InsangramAccessibility(reducedMotion, highContrast),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = InsangramTypography,
            shapes = InsangramShapes,
            content = content,
        )
    }
}
