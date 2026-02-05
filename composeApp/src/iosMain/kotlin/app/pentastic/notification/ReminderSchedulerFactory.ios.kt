package app.pentastic.notification

import app.pentastic.data.MyRepository

actual class ReminderSchedulerFactory(
    private val repository: MyRepository
) {
    actual fun create(): ReminderScheduler = IOSReminderScheduler(repository)
}
