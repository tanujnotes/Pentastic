package app.pentastic.ui.composables

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.pentastic.notification.PermissionHandler

@Composable
actual fun rememberReminderPermissionHandler(): PermissionHandler {
    val context = LocalContext.current
    var pendingCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        pendingCallback?.invoke(isGranted)
        pendingCallback = null
    }

    return remember(launcher) {
        PermissionHandler(
            context = context,
            permissionLauncher = { callback ->
                pendingCallback = callback
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        )
    }
}
