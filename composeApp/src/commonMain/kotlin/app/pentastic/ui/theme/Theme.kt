package app.pentastic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val background: Color,
    val primaryText: Color,
    val pageTitle: Color,
    val priorityText: Color,
    val menuBackground: Color,
    val divider: Color,
    val icon: Color,
    val hint: Color,
    val dragging: Color,
    val cursor: Color,
)

val LightColors = AppColors(
    background = Color(0xFFF9FBFF),
    primaryText = Color(0xFF284283),
    pageTitle = Color(0xFF933A3A),
    priorityText = Color(0xFFD01616),
    menuBackground = Color(0xFFF9FBFF),
    divider = Color(0xFFE9ECEF),
    icon = Color(0xFFD4D8E0),
    hint = Color.LightGray,
    dragging = Color.White.copy(alpha = 0.8f),
    cursor = Color(0x33284283),
)

val DarkColors = AppColors(
    background = Color.Black,
    primaryText = Color(0xFFA0A3AD),
    pageTitle = Color(0xFF93593A),
    priorityText = Color(0xFFFF3D3D),
    menuBackground = Color(0xFF1A1A1A),
    divider = Color(0xFF333333),
    icon = Color(0xFF666666),
    hint = Color(0xCD666666),
    dragging = Color(0xFF1A1A1A),
    cursor = Color(0x66E0E0E0),
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

object AppTheme {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme {
            content()
        }
    }
}
