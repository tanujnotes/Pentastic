package app.pentastic.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.pentastic.notification.PermissionHandler

enum class ReminderPermissionState {
    CHECKING,
    ALL_GRANTED,
    NEEDS_EXPLANATION,
    REQUESTING,
    DENIED
}

@Composable
fun ReminderPermissionFlow(
    permissionHandler: PermissionHandler,
    onPermissionsGranted: () -> Unit,
    onDismiss: () -> Unit
) {
    var state by remember { mutableStateOf(ReminderPermissionState.CHECKING) }

    LaunchedEffect(Unit) {
        state = if (permissionHandler.hasAllReminderPermissions()) {
            ReminderPermissionState.ALL_GRANTED
        } else {
            ReminderPermissionState.NEEDS_EXPLANATION
        }
    }

    LaunchedEffect(state) {
        if (state == ReminderPermissionState.ALL_GRANTED) {
            onPermissionsGranted()
        }
    }

    when (state) {
        ReminderPermissionState.CHECKING -> {
            // Loading state - could show a spinner
        }

        ReminderPermissionState.ALL_GRANTED -> {
            // Permissions granted, callback already triggered
        }

        ReminderPermissionState.NEEDS_EXPLANATION -> {
            ExplanationDialog(
                onConfirm = {
                    state = ReminderPermissionState.REQUESTING
                },
                onDismiss = onDismiss
            )
        }

        ReminderPermissionState.REQUESTING -> {
            LaunchedEffect(Unit) {
                if (!permissionHandler.hasNotificationPermission()) {
                    permissionHandler.requestNotificationPermission { granted ->
                        if (granted) {
                            // Now check exact alarm permission
                            if (permissionHandler.hasExactAlarmPermission()) {
                                state = ReminderPermissionState.ALL_GRANTED
                            } else {
                                // Open exact alarm settings directly
                                permissionHandler.openExactAlarmSettings()
                                state = ReminderPermissionState.DENIED
                            }
                        } else {
                            // Notification denied, open app settings
                            permissionHandler.openAppSettings()
                            state = ReminderPermissionState.DENIED
                        }
                    }
                } else if (!permissionHandler.hasExactAlarmPermission()) {
                    // Already have notification permission, need exact alarm
                    permissionHandler.openExactAlarmSettings()
                    state = ReminderPermissionState.DENIED
                } else {
                    state = ReminderPermissionState.ALL_GRANTED
                }
            }
        }

        ReminderPermissionState.DENIED -> {
            // User was sent to settings, dismiss the flow
            LaunchedEffect(Unit) {
                onDismiss()
            }
        }
    }
}

@Composable
private fun ExplanationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Permission Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "To set reminders, we need permission to send notifications and schedule alarms. This allows us to remind you about your tasks at the right time.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = onConfirm) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}
