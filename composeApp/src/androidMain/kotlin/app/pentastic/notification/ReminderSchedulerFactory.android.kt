package app.pentastic.notification

import android.content.Context
import app.pentastic.data.MyRepository

actual class ReminderSchedulerFactory(
    private val context: Context,
    private val repository: MyRepository
) {
    actual fun create(): ReminderScheduler = AndroidReminderScheduler(context, repository)
}
