package com.tritv.photo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.withStyle
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

/**
 * Every number in the app (clock, dates, counts, settings values) uses Inter's tabular lining
 * digits, so digits line up and look the same on every screen.
 */
private const val DIGITS = "tnum, lnum"

private val CenteredLine = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both)

/**
 * Type ladder for a 960 x 540 dp screen seen from across the room. Only titles of 32 sp and up use
 * the thin 300 weight with negative tracking; body text and buttons are 400, since thin text at
 * body size goes soft at that distance.
 */
object PhotoType {
    val Display = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-1.2).sp, fontFeatureSettings = DIGITS)
    val Title = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.64).sp, fontFeatureSettings = DIGITS)
    val RowTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.22).sp, fontFeatureSettings = DIGITS)
    val Body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp, fontFeatureSettings = DIGITS)
    /**
     * Pill labels are mostly Chinese, so they use the system font: mixing Inter's line metrics
     * with the system's Chinese glyphs drew them low in the pill. The line is centred and trimmed
     * so the label sits in the middle of the pill whatever the font.
     */
    val Button = TextStyle(fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 24.sp, lineHeightStyle = CenteredLine)
    val Caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp, fontFeatureSettings = DIGITS)
    val Tag = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.4.sp, lineHeightStyle = CenteredLine, fontFeatureSettings = DIGITS)
    /** Section labels inside a settings card and eyebrows above titles. */
    val Label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.4.sp, fontFeatureSettings = DIGITS)
    /** The date under a photo. */
    val PhotoDate = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp, fontFeatureSettings = DIGITS)
    val Clock = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp, fontFeatureSettings = DIGITS)
}

/**
 * [text] with its digits set in Inter, for the system-font styles ([PhotoType.Tag]): the line
 * metrics stay the system font's, so the label stays centred, but "3 年前" uses the same digits
 * as the rest of the app.
 */
fun interDigits(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        var j = i
        val digits = text[i].isDigit()
        while (j < text.length && text[j].isDigit() == digits) j++
        if (digits) withStyle(SpanStyle(fontFamily = Inter)) { append(text, i, j) } else append(text, i, j)
        i = j
    }
}

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
