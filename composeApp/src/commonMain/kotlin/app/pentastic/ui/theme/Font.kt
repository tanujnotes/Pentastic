package app.pentastic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.pentastic.data.AppLanguage
import org.jetbrains.compose.resources.Font
import pentastic.composeapp.generated.resources.Res
import pentastic.composeapp.generated.resources.Merriweather_Light
import pentastic.composeapp.generated.resources.Merriweather_Regular
import pentastic.composeapp.generated.resources.Inter_Thin
import pentastic.composeapp.generated.resources.Inter_Light
import pentastic.composeapp.generated.resources.Inter_Regular
import pentastic.composeapp.generated.resources.Inter_Medium
import pentastic.composeapp.generated.resources.Inter_SemiBold
import pentastic.composeapp.generated.resources.Inter_Bold
import pentastic.composeapp.generated.resources.Inter_Black
import pentastic.composeapp.generated.resources.Vazirmatn_Thin
import pentastic.composeapp.generated.resources.Vazirmatn_Light
import pentastic.composeapp.generated.resources.Vazirmatn_Regular
import pentastic.composeapp.generated.resources.Vazirmatn_Medium
import pentastic.composeapp.generated.resources.Vazirmatn_SemiBold
import pentastic.composeapp.generated.resources.Vazirmatn_Bold
import pentastic.composeapp.generated.resources.Vazirmatn_Black

@Composable
private fun interFontFamily() = FontFamily(
    Font(Res.font.Inter_Thin, weight = FontWeight.Thin),
    Font(Res.font.Inter_Light, weight = FontWeight.Light),
    Font(Res.font.Inter_Regular, weight = FontWeight.Normal),
    Font(Res.font.Inter_Medium, weight = FontWeight.Medium),
    Font(Res.font.Inter_SemiBold, weight = FontWeight.SemiBold),
    Font(Res.font.Inter_Bold, weight = FontWeight.Bold),
    Font(Res.font.Inter_Black, weight = FontWeight.Black)
)

@Composable
private fun vazirmatnFontFamily() = FontFamily(
    Font(Res.font.Vazirmatn_Thin, weight = FontWeight.Thin),
    Font(Res.font.Vazirmatn_Light, weight = FontWeight.Light),
    Font(Res.font.Vazirmatn_Regular, weight = FontWeight.Normal),
    Font(Res.font.Vazirmatn_Medium, weight = FontWeight.Medium),
    Font(Res.font.Vazirmatn_SemiBold, weight = FontWeight.SemiBold),
    Font(Res.font.Vazirmatn_Bold, weight = FontWeight.Bold),
    Font(Res.font.Vazirmatn_Black, weight = FontWeight.Black)
)

/**
 * Font family for the big page titles. Merriweather only covers Latin, so in
 * Farsi it silently falls back to the platform font; there Vazirmatn is used
 * instead. The Latin path keeps Merriweather's Light/Regular faces as before.
 */
@Composable
fun titleFontFamily(weight: FontWeight = FontWeight.Light): FontFamily {
    if (AppTheme.dateTexts.language == AppLanguage.FARSI) return vazirmatnFontFamily()
    return when (weight) {
        FontWeight.Normal -> FontFamily(Font(Res.font.Merriweather_Regular, weight))
        else -> FontFamily(Font(Res.font.Merriweather_Light, weight))
    }
}

@Composable
fun appTypography(isFarsi: Boolean) = Typography().run {

    val fontFamily = if (isFarsi) vazirmatnFontFamily() else interFontFamily()
    copy(
        displayLarge = displayLarge.copy(fontFamily = fontFamily),
        displayMedium = displayMedium.copy(fontFamily = fontFamily),
        displaySmall = displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = titleLarge.copy(fontFamily = fontFamily),
        titleMedium = titleMedium.copy(fontFamily = fontFamily),
        titleSmall = titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = bodySmall.copy(fontFamily = fontFamily),
        labelLarge = labelLarge.copy(fontFamily = fontFamily),
        labelMedium = labelMedium.copy(fontFamily = fontFamily),
        labelSmall = labelSmall.copy(fontFamily = fontFamily)
    )
}