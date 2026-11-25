package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = CobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = OnCobaltContainer,
    secondary = MintSecondary,
    onSecondary = Color(0xFF06231B),
    secondaryContainer = MintContainer,
    onSecondaryContainer = OnMintContainer,
    tertiary = AmberTertiary,
    onTertiary = Color(0xFF2B1600),
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = ObsidianBackground,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerHighest = Color(0xFF2B3549),
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF222A3A),
    error = CoralError,
    onError = Color(0xFF370609),
    errorContainer = CoralErrorContainer,
    onErrorContainer = OnCoralErrorContainer,
)

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE4FF),
    onPrimaryContainer = Color(0xFF14245C),
    secondary = Color(0xFF0CA678),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC3FAE8),
    onSecondaryContainer = Color(0xFF063526),
    tertiary = Color(0xFFE8590C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8CC),
    onTertiaryContainer = Color(0xFF471C04),
    background = IvoryLightBackground,
    onBackground = TextPrimaryLight,
    surface = IvoryLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = IvoryLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = Color(0xFFF1F4FA),
    surfaceContainer = IvoryLightSurfaceVariant,
    surfaceContainerHigh = Color(0xFFE2E7F3),
    outline = IvoryLightOutline,
    error = Color(0xFFE03131),
    onError = Color.White,
)

val ImmichShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ImmichVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
        typography = AppTypography,
        shapes = ImmichShapes,
        content = content,
    )
}
