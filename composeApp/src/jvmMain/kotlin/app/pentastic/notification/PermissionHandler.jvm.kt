package app.pentastic.notification

actual class PermissionHandler {
    actual fun hasNotificationPermission(): Boolean = true
    actual fun hasExactAlarmPermission(): Boolean = true
    actual fun hasAllReminderPermissions(): Boolean = true
    actual fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        onResult(true)
    }
    actual fun openAppSettings() {}
    actual fun openExactAlarmSettings() {}
}
