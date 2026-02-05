package app.pentastic.notification

expect class PermissionHandler {
    fun hasNotificationPermission(): Boolean
    fun hasExactAlarmPermission(): Boolean
    fun hasAllReminderPermissions(): Boolean
    fun requestNotificationPermission(onResult: (Boolean) -> Unit)
    fun openAppSettings()
    fun openExactAlarmSettings()
}
