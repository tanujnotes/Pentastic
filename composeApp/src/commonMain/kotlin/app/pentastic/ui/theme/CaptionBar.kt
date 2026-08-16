package app.pentastic.ui.theme

import androidx.compose.ui.unit.Dp

/**
 * Height of the window chrome strip that overlays app content, for platforms
 * where the app draws underneath the title bar (macOS desktop, where the
 * title bar is transparent and content fills the window). Zero where content
 * never extends under the chrome — Android and iOS handle their system bars
 * through window insets instead.
 */
expect fun captionBarHeight(): Dp
