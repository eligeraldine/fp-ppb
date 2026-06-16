package com.example.eas.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = MatchaGreen,
    secondary = TeaAmber,
    tertiary = SoftCream,
    background = Color(0xFF1B241D),
    surface = Color(0xFF1B241D),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White, // Diganti dari SoftCream ke Putih agar kontras tinggi
    onSurface = Color.White     // Diganti agar terbaca
)

private val LightColorScheme = lightColorScheme(
    primary = DarkLeaf,
    secondary = TeaAmber,
    tertiary = MatchaGreen,
    background = SoftCream,
    surface = SoftCream,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DarkLeaf,
    onSurface = DarkLeaf
)

@Composable
fun EASTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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