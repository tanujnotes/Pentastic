package app.pentastic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.pentastic.data.ThemeMode
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class AppColors(
    val background: Color,
    // Wide layout only: sets the index pane apart from the page next to it
    val indexBackground: Color,
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
    indexBackground = Color(0xFFF2F4F8),
    primaryText = Color(0xFF284283),
    pageTitle = Color(0xFF933A3A),
    priorityText = Color(0xFFD01616),
    menuBackground = Color(0xFFF3F5FA),
    divider = Color(0xFFE9ECEF),
    icon = Color(0xFFA8AFC2),
    hint = Color.LightGray,
    dragging = Color.White.copy(alpha = 0.8f),
    cursor = Color(0x33284283),
)

val DarkColors = AppColors(
    background = Color.Black,
    indexBackground = Color(0xFF141414),
    primaryText = Color(0xFFDBDEE7),
    pageTitle = Color(0xFF93593A),
    priorityText = Color(0xFFFF1111),
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

/**
 * M3 control colors drawn from [AppColors]. No colorScheme is set on MaterialTheme,
 * so any M3 component left with its defaults renders in the light-scheme purple —
 * wrong against this palette and barely visible on the dark menu background.
 */
@Composable
fun appRadioButtonColors(): RadioButtonColors {
    val colors = AppTheme.colors
    return RadioButtonDefaults.colors(
        selectedColor = colors.primaryText,
        unselectedColor = colors.hint,
        disabledSelectedColor = colors.primaryText.copy(alpha = 0.38f),
        disabledUnselectedColor = colors.hint.copy(alpha = 0.38f),
    )
}

@Composable
fun appSwitchColors(): SwitchColors {
    val colors = AppTheme.colors
    return SwitchDefaults.colors(
        checkedThumbColor = colors.background,
        checkedTrackColor = colors.primaryText,
        checkedBorderColor = colors.primaryText,
        uncheckedThumbColor = colors.hint,
        uncheckedTrackColor = colors.background,
        uncheckedBorderColor = colors.hint,
    )
}

@OptIn(ExperimentalTime::class)
@Composable
fun isDayNightDark(): Boolean {
    val currentHour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
    return currentHour !in 6..<18
}

@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.DAY_NIGHT,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DAY_NIGHT -> isDayNightDark()
    }

    val colors = if (darkTheme) DarkColors else LightColors

    // Update system bar style based on theme
    SystemBarEffect(isDarkTheme = darkTheme)

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            typography = interTypography(),
        ) {
            content()
        }
    }
}
