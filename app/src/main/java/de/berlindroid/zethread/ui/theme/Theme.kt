package de.berlindroid.zethread.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ExpressiveDarkColorScheme = darkColorScheme(
    primary = MintPrimary,
    onPrimary = MintOnPrimary,
    primaryContainer = MintPrimaryContainer,
    onPrimaryContainer = MintOnPrimaryContainer,
    secondary = Color(0xFFB3CCBE),
    onSecondary = Color(0xFF1E352B),
    secondaryContainer = Color(0xFF354B40),
    onSecondaryContainer = Color(0xFFCEE9D9),
    tertiary = Color(0xFFA5CDDE),
    onTertiary = Color(0xFF063543),
    background = SurfaceDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainerHighDark,
    onSurfaceVariant = TextSecondary,
    outline = OutlineDark,
    outlineVariant = Color(0xFF424945),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun ZeThreadTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ExpressiveDarkColorScheme,
        typography = Typography,
        content = content
    )
}