package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = OnBluePrimaryDark,
    primaryContainer = BluePrimaryContainerDark,
    onPrimaryContainer = OnBluePrimaryContainerDark,
    secondary = TealSecondaryDark,
    onSecondary = OnTealSecondaryDark,
    secondaryContainer = TealSecondaryContainerDark,
    onSecondaryContainer = OnTealSecondaryContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = OnAmberTertiaryDark,
    tertiaryContainer = AmberTertiaryContainerDark,
    onTertiaryContainer = OnAmberTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = ErrorRed,
    errorContainer = ErrorRedContainer,
    onErrorContainer = OnErrorRedContainer
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimaryLight,
    onPrimary = OnBluePrimaryLight,
    primaryContainer = BluePrimaryContainerLight,
    onPrimaryContainer = OnBluePrimaryContainerLight,
    secondary = TealSecondaryLight,
    onSecondary = OnTealSecondaryLight,
    secondaryContainer = TealSecondaryContainerLight,
    onSecondaryContainer = OnTealSecondaryContainerLight,
    tertiary = AmberTertiaryLight,
    onTertiary = OnAmberTertiaryLight,
    tertiaryContainer = AmberTertiaryContainerLight,
    onTertiaryContainer = OnAmberTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = ErrorRed,
    errorContainer = ErrorRedContainer,
    onErrorContainer = OnErrorRedContainer
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun McqLearningTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
