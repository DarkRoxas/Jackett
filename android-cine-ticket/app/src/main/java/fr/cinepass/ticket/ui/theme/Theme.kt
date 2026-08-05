package fr.cinepass.ticket.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Indigo = Color(0xFF5B4BD6)
private val IndigoDark = Color(0xFF1B1033)
private val Gold = Color(0xFFE8B04B)
private val Ink = Color(0xFF141019)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBEB2FF),
    onPrimary = Color(0xFF2A1B6B),
    primaryContainer = Indigo,
    onPrimaryContainer = Color.White,
    secondary = Gold,
    onSecondary = Color(0xFF3A2A05),
    background = IndigoDark,
    onBackground = Color(0xFFEDE7F6),
    surface = Color(0xFF221741),
    onSurface = Color(0xFFEDE7F6),
    surfaceVariant = Color(0xFF2E2350),
    onSurfaceVariant = Color(0xFFC9C1E0),
)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3DEFF),
    onPrimaryContainer = Color(0xFF1B1033),
    secondary = Color(0xFF8A6100),
    onSecondary = Color.White,
    background = Color(0xFFFBF8FF),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEAE4F6),
    onSurfaceVariant = Color(0xFF4A4358),
)

@Composable
fun CinePassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
