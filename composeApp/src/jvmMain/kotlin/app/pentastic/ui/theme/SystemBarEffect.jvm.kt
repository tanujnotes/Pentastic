package app.pentastic.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import javax.swing.JFrame

/**
 * The desktop equivalent of a system bar is the macOS title bar: make it
 * transparent and paint the window background in the theme color, so the page
 * blends into the chrome with no separate title bar color or divider line.
 * No-op on other desktop platforms.
 */
@Composable
actual fun SystemBarEffect(isDarkTheme: Boolean) {
    LaunchedEffect(isDarkTheme) {
        if (!System.getProperty("os.name").lowercase().contains("mac")) return@LaunchedEffect
        val background = (if (isDarkTheme) DarkColors else LightColors).background
        for (window in java.awt.Window.getWindows()) {
            (window as? JFrame)?.rootPane?.apply {
                putClientProperty("apple.awt.transparentTitleBar", true)
                // Content fills the whole window; screens clear the title bar
                // strip themselves via captionBarHeight()
                putClientProperty("apple.awt.fullWindowContent", true)
                // Best effort (honored by JetBrains Runtime): title text and
                // traffic lights follow the app theme, not the OS appearance
                putClientProperty(
                    "apple.awt.windowAppearance",
                    if (isDarkTheme) "NSAppearanceNameVibrantDark" else "NSAppearanceNameVibrantLight"
                )
            }
            window.background = java.awt.Color(background.toArgb())
        }
    }
}
