package de.thw.cbrn.strahler.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// THW corporate blue + the monochrome e-paper aesthetic of the physical device.
val ThwBlue = Color(0xFF003399)
val ThwBlueLight = Color(0xFF335CAD)
val RadYellow = Color(0xFFFFD400)
val EpaperBlack = Color(0xFF111111)
val EpaperWhite = Color(0xFFFAFAFA)

private val LightColors = lightColorScheme(
    primary = ThwBlue,
    onPrimary = Color.White,
    secondary = ThwBlueLight,
    onSecondary = Color.White,
    tertiary = RadYellow,
    onTertiary = EpaperBlack,
    background = EpaperWhite,
    onBackground = EpaperBlack,
    surface = Color.White,
    onSurface = EpaperBlack,
    surfaceVariant = Color(0xFFE9ECF2),
    onSurfaceVariant = Color(0xFF3A3F47),
)

private val DarkColors = darkColorScheme(
    primary = ThwBlueLight,
    onPrimary = Color.White,
    secondary = ThwBlueLight,
    tertiary = RadYellow,
    onTertiary = EpaperBlack,
    background = Color(0xFF101216),
    surface = Color(0xFF181B20),
)

/** Sans-serif, bold-leaning type to echo the device's FreeSans/FreeSansBold display fonts. */
private val StrahlerTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
    ),
)

@Composable
fun StrahlerTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        typography = StrahlerTypography,
        content = content,
    )
}
