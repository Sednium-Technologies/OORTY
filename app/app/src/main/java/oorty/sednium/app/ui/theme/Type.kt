package oorty.sednium.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import oorty.sednium.app.R

/**
 * The web app loads Google Fonts "Source Serif 4" (variable weight 200-900,
 * italic + roman) and sets it as the sole `font-sans` family:
 *
 *   fontFamily: { sans: ['"Source Serif 4"', 'serif'] }
 *
 * On Android we pull the same family from a bundled variable font (place
 * source_serif_4.ttf in res/font/) and fall back to the system serif if
 * it isn't bundled, so the app never silently reverts to a sans-serif look.
 */
val CrimsonProFont: FontFamily = try {
    FontFamily(
        Font(R.font.crimson_pro, FontWeight.Normal),
        Font(R.font.crimson_pro, FontWeight.Medium),
        Font(R.font.crimson_pro, FontWeight.Bold)
    )
} catch (e: Exception) {
    FontFamily.Serif
}

val SuperWarmingFont: FontFamily = try {
    FontFamily(Font(R.font.super_warming, FontWeight.Normal))
} catch (e: Exception) {
    FontFamily.Serif
}

val SourceSerif4: FontFamily = CrimsonProFont

fun getSedniumTypography(useSerif: Boolean = true): Typography {
    val font = if (useSerif) CrimsonProFont else FontFamily.Default
    return Typography(
        titleLarge = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            letterSpacing = (-0.2).sp
        ),
        titleMedium = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = (-0.1).sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            letterSpacing = 0.6.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 20.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodySmall = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    )
}

val SedniumTypography = getSedniumTypography(useSerif = true)
