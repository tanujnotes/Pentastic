package app.pentastic.notification

actual class ReminderSchedulerFactory {
    actual fun create(): ReminderScheduler = DesktopReminderScheduler()
}
