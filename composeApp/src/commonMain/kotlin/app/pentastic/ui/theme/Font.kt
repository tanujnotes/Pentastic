package app.pentastic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import pentastic.composeapp.generated.resources.Inter_Black
import pentastic.composeapp.generated.resources.Inter_Bold
import pentastic.composeapp.generated.resources.Inter_Light
import pentastic.composeapp.generated.resources.Inter_Medium
import pentastic.composeapp.generated.resources.Inter_Regular
import pentastic.composeapp.generated.resources.Inter_SemiBold
import pentastic.composeapp.generated.resources.Inter_Thin
import pentastic.composeapp.generated.resources.Merriweather_Bold
import pentastic.composeapp.generated.resources.Merriweather_Light
import pentastic.composeapp.generated.resources.Merriweather_Medium
import pentastic.composeapp.generated.resources.Merriweather_Regular
import pentastic.composeapp.generated.resources.Res

@Composable
private fun merriweatherFontFamily() = FontFamily(
    Font(Res.font.Merriweather_Light, weight = FontWeight.Light),
    Font(Res.font.Merriweather_Regular, weight = FontWeight.Normal),
    Font(Res.font.Merriweather_Medium, weight = FontWeight.Medium),
    Font(Res.font.Merriweather_Bold, weight = FontWeight.Bold),
)


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
fun interTypography() = Typography().run {

    val fontFamily = interFontFamily()
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