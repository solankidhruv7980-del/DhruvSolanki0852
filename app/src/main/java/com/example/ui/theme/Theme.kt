package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BroadcastDarkColorScheme = darkColorScheme(
    primary = BroadcastCrimson,
    onPrimary = StudioTextPrimary,
    primaryContainer = BroadcastCrimsonDim,
    onPrimaryContainer = StudioTextPrimary,
    secondary = TelemetryCyan,
    onSecondary = ObsidianFloor,
    secondaryContainer = TelemetryCyanDim,
    onSecondaryContainer = StudioTextPrimary,
    tertiary = YppEmerald,
    onTertiary = ObsidianFloor,
    tertiaryContainer = YppEmeraldDim,
    onTertiaryContainer = StudioTextPrimary,
    background = ObsidianFloor,
    onBackground = StudioTextPrimary,
    surface = RackSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = RackSurfaceElevated,
    onSurfaceVariant = StudioTextSecondary,
    surfaceContainerLowest = ObsidianFloor,
    surfaceContainerLow = RackSurface,
    surfaceContainer = RackSurfaceElevated,
    surfaceContainerHigh = RackSurfaceHighest,
    outline = RackHairline,
    outlineVariant = RackHairline,
    error = PolicyRed,
    onError = StudioTextPrimary
)

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun OmniTubeStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BroadcastDarkColorScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
