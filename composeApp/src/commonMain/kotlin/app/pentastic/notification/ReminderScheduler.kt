package app.pentastic.notification

import app.pentastic.data.Note

interface ReminderScheduler {
    /**
     * Schedule a reminder notification for a note.
     * @param note The note to schedule reminder for
     */
    suspend fun scheduleReminder(note: Note)

    /**
     * Cancel a scheduled reminder for a note.
     * @param noteUuid The UUID of the note (used as unique identifier for the notification)
     */
    suspend fun cancelReminder(noteUuid: String)

    /**
     * Reschedule all active reminders (called on app start/reboot)
     */
    suspend fun rescheduleAllReminders()

    /**
     * Check if notifications are permitted on this platform
     */
    fun hasNotificationPermission(): Boolean

    /**
     * Request notification permission (platform-specific implementation)
     * @return true if permission granted, false otherwise
     */
    suspend fun requestNotificationPermission(): Boolean
}
