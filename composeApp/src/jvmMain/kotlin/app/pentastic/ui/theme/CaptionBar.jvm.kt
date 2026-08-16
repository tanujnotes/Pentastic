package app.pentastic.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Content extends under the macOS title bar (fullWindowContent in
// SystemBarEffect), so screens pad by the standard title bar height to clear it
actual fun captionBarHeight(): Dp =
    if (System.getProperty("os.name").lowercase().contains("mac")) 28.dp else 0.dp
