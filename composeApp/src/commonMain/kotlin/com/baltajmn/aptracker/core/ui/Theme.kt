package com.baltajmn.aptracker.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val apTrackerDark = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3B1D8A),
    onPrimaryContainer = Color(0xFFD8C8FF),
    secondary = Color(0xFF06B6D4),
    onSecondary = Color(0xFF002933),
    secondaryContainer = Color(0xFF0E4F5C),
    onSecondaryContainer = Color(0xFFA0E9F5),
    tertiary = Color(0xFFFFD166),
    onTertiary = Color(0xFF3A2600),
    tertiaryContainer = Color(0xFF533800),
    onTertiaryContainer = Color(0xFFFFE0A0),
    background = Color(0xFF080C14),
    onBackground = Color(0xFFE8EDF5),
    surface = Color(0xFF0F1623),
    onSurface = Color(0xFFE8EDF5),
    surfaceVariant = Color(0xFF1C2740),
    onSurfaceVariant = Color(0xFF8B9AB5),
    surfaceBright = Color(0xFF243352),
    surfaceContainerLow = Color(0xFF0B1119),
    surfaceContainerHigh = Color(0xFF1C2740),
    error = Color(0xFFEE6666),
    onError = Color(0xFF3A0000),
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFCCCC),
    outline = Color(0xFF2A3D5C),
    outlineVariant = Color(0xFF1A2840),
)

private val apTrackerLight = lightColorScheme(
    primary = Color(0xFF5B21B6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFF0891B2),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFF0F8),
    onSecondaryContainer = Color(0xFF003544),
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF0D1117),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0D1117),
    surfaceVariant = Color(0xFFE8ECF4),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFDC2626),
    outline = Color(0xFFB0BEC5),
)

object ApColors {
    val Progression  = Color(0xFFAF99EF)
    val Useful       = Color(0xFF6EFF6E)
    val Trap         = Color(0xFFEE6666)
    val Filler       = Color(0xFF8B9AB5)
    val Connected    = Color(0xFF6EFF6E)
    val Connecting   = Color(0xFFFFD166)
    val Disconnected = Color(0xFFEE6666)
}

private val ApTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 22.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 16.sp, letterSpacing = 0.15.sp),
    labelMedium   = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 10.sp, letterSpacing = 0.5.sp),
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) apTrackerDark else apTrackerLight,
        typography = ApTypography,
        content = content
    )
}
