package com.emfad.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val EMFADDarkColorScheme = darkColorScheme(
    primary = EMFADBlue,
    onPrimary = EMFADWhite,
    primaryContainer = EMFADDarkBlue,
    onPrimaryContainer = EMFADLightBlue,
    
    secondary = EMFADYellow,
    onSecondary = EMFADBlack,
    secondaryContainer = EMFADDarkYellow,
    onSecondaryContainer = EMFADLightYellow,
    
    tertiary = EMFADGreen,
    onTertiary = EMFADBlack,
    tertiaryContainer = Color(0xFF004D26),
    onTertiaryContainer = Color(0xFF66FF99),
    
    error = EMFADRed,
    onError = EMFADWhite,
    errorContainer = Color(0xFF660000),
    onErrorContainer = Color(0xFFFF6666),
    
    background = EMFADBlack,
    onBackground = EMFADWhite,
    surface = EMFADDarkGray,
    onSurface = EMFADWhite,
    surfaceVariant = EMFADGray,
    onSurfaceVariant = EMFADOffWhite,
    
    outline = EMFADLightGray,
    outlineVariant = EMFADGray,
    scrim = Color(0x80000000),
    
    inverseSurface = EMFADWhite,
    inverseOnSurface = EMFADBlack,
    inversePrimary = EMFADDarkBlue,
    
    surfaceDim = Color(0xFF0D0D0D),
    surfaceBright = Color(0xFF333333),
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = EMFADDarkGray,
    surfaceContainerHigh = Color(0xFF2D2D2D),
    surfaceContainerHighest = EMFADLightGray
)

private val EMFADLightColorScheme = lightColorScheme(
    primary = EMFADBlue,
    onPrimary = EMFADWhite,
    primaryContainer = EMFADLightBlue,
    onPrimaryContainer = EMFADDarkBlue,
    
    secondary = EMFADYellow,
    onSecondary = EMFADBlack,
    secondaryContainer = EMFADLightYellow,
    onSecondaryContainer = EMFADDarkYellow,
    
    tertiary = EMFADGreen,
    onTertiary = EMFADWhite,
    tertiaryContainer = Color(0xFF99FFB3),
    onTertiaryContainer = Color(0xFF004D26),
    
    error = EMFADRed,
    onError = EMFADWhite,
    errorContainer = Color(0xFFFF9999),
    onErrorContainer = Color(0xFF660000),
    
    background = EMFADWhite,
    onBackground = EMFADBlack,
    surface = EMFADOffWhite,
    onSurface = EMFADBlack,
    surfaceVariant = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF404040),
    
    outline = EMFADGray,
    outlineVariant = EMFADLightGray,
    scrim = Color(0x80000000),
    
    inverseSurface = EMFADBlack,
    inverseOnSurface = EMFADWhite,
    inversePrimary = EMFADLightBlue,
    
    surfaceDim = Color(0xFFE0E0E0),
    surfaceBright = EMFADWhite,
    surfaceContainerLowest = EMFADWhite,
    surfaceContainerLow = Color(0xFFF8F8F8),
    surfaceContainer = EMFADOffWhite,
    surfaceContainerHigh = Color(0xFFE6E6E6),
    surfaceContainerHighest = Color(0xFFD9D9D9)
)

@Composable
fun EMFADAnalyzerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled for EMFAD branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> EMFADDarkColorScheme
        else -> EMFADLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
