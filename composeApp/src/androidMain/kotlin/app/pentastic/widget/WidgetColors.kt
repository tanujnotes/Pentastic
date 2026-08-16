@file:OptIn(ExperimentalTime::class)

package app.pentastic.widget

import android.content.Context
import android.content.res.Configuration
import app.pentastic.data.ThemeMode
import app.pentastic.ui.theme.AppColors
import app.pentastic.ui.theme.DarkColors
import app.pentastic.ui.theme.LightColors
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The palette the widget renders with, resolved against the app's own [ThemeMode]
 * rather than Glance's day/night ColorProvider. The app's default mode is clock-based
 * (see AppTheme.isDayNightDark), so deferring to system dark mode would leave the
 * widget disagreeing with the app for most of the day. [WidgetRefreshScheduler] fires
 * at the light/dark boundaries to repaint.
 *
 * Colors come from the same [LightColors]/[DarkColors] the app uses — they are plain
 * top-level values, so the hex codes stay single-sourced even though Glance cannot
 * reach the AppTheme CompositionLocal.
 */
fun widgetColors(context: Context, themeMode: ThemeMode): AppColors =
    if (isWidgetDark(context, themeMode)) DarkColors else LightColors

fun isWidgetDark(context: Context, themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemDark(context)
    ThemeMode.DAY_NIGHT -> isDayNightDark()
}

private fun isSystemDark(context: Context): Boolean =
    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

/** Mirrors AppTheme.isDayNightDark: dark outside 06:00-18:00 local. */
private fun isDayNightDark(): Boolean {
    val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
    return hour !in 6..<18
}
