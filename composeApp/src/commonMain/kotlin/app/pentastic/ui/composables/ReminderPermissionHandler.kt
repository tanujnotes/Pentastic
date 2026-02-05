package app.pentastic.ui.composables

import androidx.compose.runtime.Composable
import app.pentastic.notification.PermissionHandler

@Composable
expect fun rememberReminderPermissionHandler(): PermissionHandler
