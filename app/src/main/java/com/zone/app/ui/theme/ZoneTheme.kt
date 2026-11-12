package com.zone.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

/* ==========================================================================
   Zone — iOS-style look & feel.
   Light-first, generous white space, hairline separators, one confident
   accent (iOS blue), SF-grade type scale rendered with the system sans.
   ========================================================================== */

object ZoneColors {
    // Light
    val BackgroundLight = Color(0xFFFCFCFD)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceGroupedLight = Color(0xFFF2F2F7)
    val SeparatorLight = Color(0xFFE5E5EA)
    val LabelLight = Color(0xFF1C1C1E)
    val SecondaryLabelLight = Color(0xFF8E8E93)

    // Dark
    val BackgroundDark = Color(0xFF000000)
    val SurfaceDark = Color(0xFF1C1C1E)
    val SurfaceGroupedDark = Color(0xFF1C1C1E)
    val SeparatorDark = Color(0xFF38383A)
    val LabelDark = Color(0xFFF2F2F7)
    val SecondaryLabelDark = Color(0xFF98989D)

    val Blue = Color(0xFF007AFF)
    val Green = Color(0xFF34C759)
    val Red = Color(0xFFFF3B30)
    val Orange = Color(0xFFFF9500)
    val Yellow = Color(0xFFFFCC00)
    val Purple = Color(0xFFAF52DE)
    val Teal = Color(0xFF5AC8FA)
    val Pink = Color(0xFFFF2D55)
    val Indigo = Color(0xFF5856D6)
}

private val LightScheme = lightColorScheme(
    primary = ZoneColors.Blue,
    onPrimary = Color.White,
    secondary = ZoneColors.Blue,
    onSecondary = Color.White,
    background = ZoneColors.BackgroundLight,
    onBackground = ZoneColors.LabelLight,
    surface = ZoneColors.SurfaceLight,
    onSurface = ZoneColors.LabelLight,
    surfaceVariant = ZoneColors.SurfaceGroupedLight,
    onSurfaceVariant = ZoneColors.SecondaryLabelLight,
    outline = ZoneColors.SeparatorLight,
    outlineVariant = ZoneColors.SeparatorLight,
    error = ZoneColors.Red,
    onError = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    secondary = Color(0xFF0A84FF),
    onSecondary = Color.White,
    background = ZoneColors.BackgroundDark,
    onBackground = ZoneColors.LabelDark,
    surface = ZoneColors.SurfaceDark,
    onSurface = ZoneColors.LabelDark,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = ZoneColors.SecondaryLabelDark,
    outline = ZoneColors.SeparatorDark,
    outlineVariant = ZoneColors.SeparatorDark,
    error = Color(0xFFFF453A),
    onError = Color.White
)

val ZoneTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp
    )
)

val ZoneShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ZoneTheme(
    theme: String = "system", // light | dark | system
    content: @Composable () -> Unit
) {
    val dark = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = ZoneTypography,
        shapes = ZoneShapes,
        content = content
    )
}
