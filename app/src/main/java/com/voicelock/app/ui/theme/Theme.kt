package com.voicelock.app.ui.theme

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

private val DarkColors = darkColorScheme(
    primary = VoiceLockAccent,
    onPrimary = VoiceLockBackgroundDark,
    background = VoiceLockBackgroundDark,
    onBackground = VoiceLockOnDark,
    surface = VoiceLockSurfaceDark,
    onSurface = VoiceLockOnDark,
    error = VoiceLockError
)

private val LightColors = lightColorScheme(
    primary = VoiceLockAccentDark,
    onPrimary = VoiceLockBackgroundLight,
    background = VoiceLockBackgroundLight,
    onBackground = VoiceLockOnLight,
    surface = VoiceLockSurfaceLight,
    onSurface = VoiceLockOnLight,
    error = VoiceLockError
)

@Composable
fun VoiceLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VoiceLockTypography,
        content = content
    )
}

/** The hardened gate is always dark, regardless of system theme — deliberate, not themeable. */
@Composable
fun VoiceLockGateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = VoiceLockTypography,
        content = content
    )
}
