package com.example.highlightextractor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Design tokens for Inktrail.
 *
 * The look is borrowed from printed matter: warm paper, black ink, a fluorescent
 * highlighter, halftone dots and hard offset shadows. These colours are deliberately
 * not Material suggestions — [InktrailTheme] maps them onto a Material scheme only so
 * that stock controls (sliders, text selection) inherit the right contrast.
 */
@Immutable
data class InkPalette(
    val bg: Color,
    val sheet: Color,
    val panel: Color,
    val fg: Color,
    val dim: Color,
    val acid: Color,
    val hot: Color,
    val cobalt: Color,
    val red: Color,
    /** Text/ink that always sits on an acid surface (must stay dark in both themes). */
    val onAccent: Color,
    /** A deliberately inverted surface: black-ish paper for the loud panels. */
    val invertBg: Color,
    val invertFg: Color,
    val isDark: Boolean,
)

val InkBlack = Color(0xFF151410)
val InkPaper = Color(0xFFF7F2E4)

val LightInkPalette = InkPalette(
    bg = Color(0xFFF2EDE0),
    sheet = Color(0xFFFBF7EC),
    panel = Color(0xFFE7E0CF),
    fg = Color(0xFF151410),
    dim = Color(0xFF6F695B),
    acid = Color(0xFFDFFF00),
    hot = Color(0xFFFF4F8F),
    cobalt = Color(0xFF2946E8),
    red = Color(0xFFDE3A24),
    onAccent = InkBlack,
    invertBg = InkBlack,
    invertFg = InkPaper,
    isDark = false,
)

val DarkInkPalette = InkPalette(
    bg = Color(0xFF12110F),
    sheet = Color(0xFF1B1A17),
    panel = Color(0xFF25231D),
    fg = Color(0xFFF2EDE0),
    dim = Color(0xFF968F7F),
    acid = Color(0xFFDFFF00),
    hot = Color(0xFFFF6EA6),
    cobalt = Color(0xFF8B9BFF),
    red = Color(0xFFFF6A55),
    onAccent = InkBlack,
    invertBg = Color(0xFF1D1B16),
    invertFg = Color(0xFFF2EDE0),
    isDark = true,
)

/** Marker colours used by the preset swatches. Kept vibrant in both themes. */
object MarkerInk {
    val yellow = Color(0xFFF2E300)
    val green = Color(0xFF3DCB79)
    val pink = Color(0xFFFF4F9C)
    val orange = Color(0xFFFF8A1E)
    val blue = Color(0xFF4F79FF)

    fun forPreset(name: String): Color = when (name) {
        "Yellow" -> yellow
        "Green" -> green
        "Pink" -> pink
        "Orange" -> orange
        "Blue" -> blue
        else -> yellow
    }
}

val LocalInkPalette = staticCompositionLocalOf { LightInkPalette }

val DisplayFamily = FontFamily.Serif
val MonoFamily = FontFamily.Monospace
val BodyFamily = FontFamily.SansSerif

private val InkTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Black,
        fontSize = 46.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.4).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Black,
        fontSize = 38.sp,
        lineHeight = 35.sp,
        letterSpacing = (-1).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Black,
        fontSize = 24.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.7.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.9.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        letterSpacing = 1.1.sp,
    ),
)

private val LightScheme = lightColorScheme(
    primary = LightInkPalette.fg,
    onPrimary = LightInkPalette.sheet,
    secondary = LightInkPalette.acid,
    onSecondary = LightInkPalette.fg,
    tertiary = LightInkPalette.hot,
    onTertiary = LightInkPalette.fg,
    background = LightInkPalette.bg,
    onBackground = LightInkPalette.fg,
    surface = LightInkPalette.sheet,
    onSurface = LightInkPalette.fg,
    surfaceVariant = LightInkPalette.panel,
    onSurfaceVariant = LightInkPalette.fg,
    outline = LightInkPalette.fg,
    outlineVariant = LightInkPalette.dim,
    error = LightInkPalette.red,
    onError = LightInkPalette.sheet,
    scrim = Color.Black,
)

private val DarkScheme = darkColorScheme(
    primary = DarkInkPalette.fg,
    onPrimary = DarkInkPalette.bg,
    secondary = DarkInkPalette.acid,
    onSecondary = Color(0xFF151410),
    tertiary = DarkInkPalette.hot,
    onTertiary = Color(0xFF151410),
    background = DarkInkPalette.bg,
    onBackground = DarkInkPalette.fg,
    surface = DarkInkPalette.sheet,
    onSurface = DarkInkPalette.fg,
    surfaceVariant = DarkInkPalette.panel,
    onSurfaceVariant = DarkInkPalette.fg,
    outline = DarkInkPalette.fg,
    outlineVariant = DarkInkPalette.dim,
    error = DarkInkPalette.red,
    onError = Color(0xFF151410),
    scrim = Color.Black,
)

@Composable
fun InktrailTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalInkPalette provides if (darkTheme) DarkInkPalette else LightInkPalette,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = InkTypography,
            content = content,
        )
    }
}
