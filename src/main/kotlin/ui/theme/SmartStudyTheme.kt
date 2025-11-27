package com.smartstudy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightPalette = lightColors(
    primary = Color(0xFF5A6DFF),
    primaryVariant = Color(0xFF3E4FD8),
    secondary = Color(0xFF7B61FF),
    secondaryVariant = Color(0xFF6847D8),
    background = Color(0xFFF5F7FB),
    surface = Color.White,
    error = Color(0xFFE57373),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1F2A44),
    onSurface = Color(0xFF1F2A44),
    onError = Color.White
)

private val DarkPalette = darkColors(
    primary = Color(0xFF8AA9FF),
    secondary = Color(0xFFB388FF)
)

@Composable
fun SmartStudyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colors: Colors = if (darkTheme) DarkPalette else LightPalette,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = colors,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}

