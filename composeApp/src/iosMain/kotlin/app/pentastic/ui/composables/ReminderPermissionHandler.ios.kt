package app.pentastic.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.pentastic.notification.PermissionHandler

@Composable
actual fun rememberReminderPermissionHandler(): PermissionHandler {
    return remember { PermissionHandler() }
}
