package com.origami.assistant.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = OrigamiPrimary,
    onPrimary = OrigamiOnPrimary,
    primaryContainer = OrigamiPrimaryContainer,
    secondary = OrigamiSecondary,
    tertiary = OrigamiTertiary,
    background = OrigamiBackground,
    surface = OrigamiSurface,
    surfaceVariant = OrigamiSurfaceVariant,
    outline = OrigamiOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = OrigamiPrimaryDark,
    onPrimary = OrigamiBackgroundDark,
    primaryContainer = Color(0xFF3D2D8C),
    secondary = OrigamiSecondary,
    tertiary = OrigamiTertiary,
    background = OrigamiBackgroundDark,
    surface = OrigamiSurfaceDark,
    surfaceVariant = OrigamiSurfaceVariantDark
)

@Composable
fun OrigamiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
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
