package app.pentastic.notification

import app.pentastic.data.Note

/**
 * No-op implementation for Desktop.
 * Reminders are not supported on Desktop platform.
 */
class DesktopReminderScheduler : ReminderScheduler {

    override suspend fun scheduleReminder(note: Note) {
        // No-op: Desktop doesn't support reminders
    }

    override suspend fun cancelReminder(noteUuid: String) {
        // No-op: Desktop doesn't support reminders
    }

    override suspend fun rescheduleAllReminders() {
        // No-op: Desktop doesn't support reminders
    }

    override fun hasNotificationPermission(): Boolean {
        // Desktop doesn't need permissions, but since we don't support reminders, return false
        return false
    }

    override suspend fun requestNotificationPermission(): Boolean {
        // Desktop doesn't support reminders
        return false
    }
}
