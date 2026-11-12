package com.zone.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Palette tokens that need to track dark/light explicitly (Material's scheme stays primary). */
data class ZonePalette(
    val background: Color,
    val surface: Color,
    val grouped: Color,
    val separator: Color,
    val label: Color,
    val secondary: Color,
    val accent: Color,
    val green: Color,
    val red: Color,
    val orange: Color,
    val yellow: Color,
    val purple: Color,
    val teal: Color,
    val pink: Color,
    val indigo: Color,
    val isDark: Boolean
)

@Composable
fun zonePalette(): ZonePalette {
    val dark = isSystemInDarkTheme()
    return if (dark) {
        ZonePalette(
            background = ZoneColors.BackgroundDark,
            surface = ZoneColors.SurfaceDark,
            grouped = ZoneColors.SurfaceGroupedDark,
            separator = ZoneColors.SeparatorDark,
            label = ZoneColors.LabelDark,
            secondary = ZoneColors.SecondaryLabelDark,
            accent = Color(0xFF0A84FF),
            green = Color(0xFF30D158),
            red = Color(0xFFFF453A),
            orange = Color(0xFFFF9F0A),
            yellow = Color(0xFFFFD60A),
            purple = Color(0xFFBF5AF2),
            teal = Color(0xFF64D2FF),
            pink = Color(0xFFFF375F),
            indigo = Color(0xFF5E5CE6),
            isDark = true
        )
    } else {
        ZonePalette(
            background = ZoneColors.BackgroundLight,
            surface = ZoneColors.SurfaceLight,
            grouped = ZoneColors.SurfaceGroupedLight,
            separator = ZoneColors.SeparatorLight,
            label = ZoneColors.LabelLight,
            secondary = ZoneColors.SecondaryLabelLight,
            accent = ZoneColors.Blue,
            green = ZoneColors.Green,
            red = ZoneColors.Red,
            orange = ZoneColors.Orange,
            yellow = ZoneColors.Yellow,
            purple = ZoneColors.Purple,
            teal = ZoneColors.Teal,
            pink = ZoneColors.Pink,
            indigo = ZoneColors.Indigo,
            isDark = false
        )
    }
}

/** Soft paper tints for note cards (light) / translucent overlays (dark). */
@Composable
fun notePaper(color: String): Color = if (isSystemInDarkTheme()) {
    when (color) {
        "sand" -> Color(0xFF2C2A24)
        "sage" -> Color(0xFF242D25)
        "clay" -> Color(0xFF2E2622)
        else -> zonePalette().surface
    }
} else {
    when (color) {
        "sand" -> Color(0xFFFFFBEB)
        "sage" -> Color(0xFFF2FAF3)
        "clay" -> Color(0xFFFCF3EE)
        else -> ZoneColors.SurfaceLight
    }
}
