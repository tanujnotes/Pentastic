package app.pentastic.notification

import app.pentastic.data.Note
import app.pentastic.data.RepeatFrequency
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

/**
 * The next occurrence of a repeating reminder strictly after [now], stepping whole
 * intervals from the stored [reminderAt] so the clock time is preserved. A fire is
 * normally the only thing that advances a reminder, so a missed fire (device off at
 * the time, alarm delivered late) would otherwise leave [reminderAt] in the past and
 * kill the chain: reschedulers skip past-due times. Non-repeating reminders and
 * times already in the future are returned unchanged.
 */
@OptIn(ExperimentalTime::class)
fun nextFutureReminderTime(
    reminderAt: Long,
    frequency: RepeatFrequency,
    now: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Long {
    if (reminderAt <= 0 || reminderAt > now || frequency == RepeatFrequency.NONE) return reminderAt
    val stored = Instant.fromEpochMilliseconds(reminderAt).toLocalDateTime(timeZone)
    val clockTime = LocalTime(stored.hour, stored.minute)
    var date = stored.date
    var next = reminderAt
    while (next <= now) {
        date = when (frequency) {
            RepeatFrequency.NONE -> return reminderAt
            RepeatFrequency.DAILY -> date.plus(1, DateTimeUnit.DAY)
            RepeatFrequency.WEEKLY -> date.plus(7, DateTimeUnit.DAY)
            RepeatFrequency.MONTHLY -> date.plus(1, DateTimeUnit.MONTH)
            RepeatFrequency.QUARTERLY -> date.plus(3, DateTimeUnit.MONTH)
            RepeatFrequency.YEARLY -> date.plus(1, DateTimeUnit.YEAR)
        }
        next = LocalDateTime(date, clockTime).toInstant(timeZone).toEpochMilliseconds()
    }
    return next
}

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
