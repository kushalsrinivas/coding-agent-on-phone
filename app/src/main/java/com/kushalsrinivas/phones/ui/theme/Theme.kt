package com.kushalsrinivas.phones.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7AA2F7),
    onPrimary = Color(0xFF1A1B26),
    secondary = Color(0xFF9ECE6A),
    onSecondary = Color(0xFF1A1B26),
    tertiary = Color(0xFFBB9AF7),
    background = Color(0xFF1A1B26),
    onBackground = Color(0xFFC0CAF5),
    surface = Color(0xFF24283B),
    onSurface = Color(0xFFC0CAF5),
    surfaceVariant = Color(0xFF414868),
    onSurfaceVariant = Color(0xFFA9B1D6),
    error = Color(0xFFF7768E),
    onError = Color(0xFF1A1B26),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7DE9),
    onPrimary = Color.White,
    secondary = Color(0xFF587539),
    onSecondary = Color.White,
    tertiary = Color(0xFF7847BD),
    background = Color(0xFFD5D6DB),
    onBackground = Color(0xFF3760BF),
    surface = Color(0xFFE1E2E7),
    onSurface = Color(0xFF3760BF),
    surfaceVariant = Color(0xFFC4C8DA),
    onSurfaceVariant = Color(0xFF6172B0),
    error = Color(0xFFC64343),
    onError = Color.White,
)

@Composable
fun PhoneAgentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
