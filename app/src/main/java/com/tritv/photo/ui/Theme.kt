package com.tritv.photo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.lightColorScheme
import com.tritv.photo.R

/**
 * White canvas, deep-navy text and one indigo for everything interactive; the pastel band at the
 * top of the home screen is the only decoration. Photo screens (slideshow, screensaver) are black
 * and use none of this, so the brand never competes with a photo.
 */
object PhotoColors {
    val Canvas = Color(0xFFFFFFFF)
    val CanvasSoft = Color(0xFFF6F9FC)
    val Cream = Color(0xFFF5E9D4)
    val Hairline = Color(0xFFE3E8EE)

    val Ink = Color(0xFF0D253D)
    val InkSecondary = Color(0xFF273951)
    val InkMute = Color(0xFF64748D)

    /** Filled pill buttons, focus rings, links. */
    val Primary = Color(0xFF533AFD)
    val PrimaryDeep = Color(0xFF4434D4)
    val PrimaryPress = Color(0xFF2E2B8C)

    /** Soft tag fill ("3 年前"), with [PrimaryDeep] text. */
    val Subdued = Color(0xFFB9B9F9)

    /** Dark pill buttons. */
    val Navy = Color(0xFF1C1E54)

    val OnPrimary = Color.White
    val Error = Color(0xFFEA2261)

    /** Text over photos. */
    val OnPhoto = Color.White
    val OnPhotoMuted = Color(0xD9FFFFFF)
    val PhotoChip = Color(0x2EFFFFFF)
}

/** Inter (SIL OFL 1.1, see FONT-LICENSE-OFL.txt) for Latin and digits; Chinese falls back to the system font. */
val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

private const val SS01 = "ss01"

/**
 * Type ladder for a 960 x 540 dp screen seen from across the room. Only titles of 32 sp and up use
 * the thin 300 weight with negative tracking; body text and buttons are 400, since thin text at
 * body size goes soft at that distance.
 */
object PhotoType {
    val Display = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-1.2).sp, fontFeatureSettings = SS01)
    val Title = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.64).sp, fontFeatureSettings = SS01)
    val RowTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.22).sp, fontFeatureSettings = SS01)
    val Body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp, fontFeatureSettings = SS01)
    val Button = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 20.sp, fontFeatureSettings = SS01)
    val Caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp, fontFeatureSettings = SS01)
    val Tag = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.4.sp, fontFeatureSettings = SS01)
    val Clock = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 64.sp, lineHeight = 68.sp, letterSpacing = (-1.4).sp, fontFeatureSettings = "$SS01, tnum")
}

/** Tabular (fixed-width) digits, for dates, counts and the clock. */
fun TextStyle.tabular() = copy(fontFeatureSettings = listOfNotNull(fontFeatureSettings, "tnum").joinToString(", "))

private val colors = lightColorScheme(
    primary = PhotoColors.Primary,
    onPrimary = PhotoColors.OnPrimary,
    background = PhotoColors.Canvas,
    onBackground = PhotoColors.Ink,
    surface = PhotoColors.Canvas,
    onSurface = PhotoColors.Ink,
    onSurfaceVariant = PhotoColors.InkMute,
    error = PhotoColors.Error,
)

private val typography = Typography().run {
    copy(
        displaySmall = PhotoType.Display,
        headlineMedium = PhotoType.Title,
        titleLarge = PhotoType.RowTitle,
        bodyLarge = PhotoType.Body,
        bodyMedium = PhotoType.Caption,
        labelLarge = PhotoType.Button,
        labelMedium = PhotoType.Tag,
    )
}

@Composable
fun PhotoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography) {
        CompositionLocalProvider(LocalContentColor provides PhotoColors.Ink, content = content)
    }
}
