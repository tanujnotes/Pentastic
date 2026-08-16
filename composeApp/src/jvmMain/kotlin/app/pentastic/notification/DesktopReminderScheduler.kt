package app.pentastic.notification

import app.pentastic.data.MyRepository
import app.pentastic.data.Note
import app.pentastic.data.RepeatFrequency
import app.pentastic.utils.hasRepeatIntervalPassed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * In-process reminder scheduler for Desktop: one delay job per pending
 * reminder, re-armed on every app start via [rescheduleAllReminders], so
 * reminders only fire while the app is running. On macOS they surface as
 * Notification Center banners (via osascript); elsewhere delivery is skipped
 * but the reminder chain still advances.
 */
@OptIn(ExperimentalTime::class)
class DesktopReminderScheduler(
    private val repository: MyRepository
) : ReminderScheduler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pendingJobs = ConcurrentHashMap<String, Job>()

    override suspend fun scheduleReminder(note: Note) {
        if (note.reminderAt <= 0 || note.reminderEnabled == 0) return
        pendingJobs.remove(note.uuid)?.cancel()

        val delayMs = note.reminderAt - Clock.System.now().toEpochMilliseconds()
        if (delayMs <= 0) return

        pendingJobs[note.uuid] = scope.launch {
            delay(delayMs)
            pendingJobs.remove(note.uuid)
            fire(note.id)
        }
    }

    override suspend fun cancelReminder(noteUuid: String) {
        pendingJobs.remove(noteUuid)?.cancel()
    }

    override suspend fun rescheduleAllReminders() {
        val now = Clock.System.now().toEpochMilliseconds()
        repository.getNotesWithActiveReminders().forEach { note ->
            val frequency = RepeatFrequency.fromOrdinal(note.repeatFrequency)
            val nextAt = nextFutureReminderTime(note.reminderAt, frequency, now)
            if (nextAt > now) {
                // Fast-forward missed repeating chains and persist, mirroring Android
                val toSchedule = if (nextAt != note.reminderAt) {
                    note.copy(reminderAt = nextAt, updatedAt = now)
                        .also { repository.updateNote(it) }
                } else note
                scheduleReminder(toSchedule)
            }
        }
    }

    override fun hasNotificationPermission(): Boolean = true

    override suspend fun requestNotificationPermission(): Boolean = true

    // Mirrors ReminderBroadcastReceiver: re-read fresh state, drop orphaned
    // reminders, un-check the task, advance/consume the reminder time, notify.
    private suspend fun fire(noteId: Long) {
        val note = repository.getNoteById(noteId) ?: return
        if (note.deletedAt > 0) return

        val isRepeatingTask = note.repeatFrequency > 0
        val frequency = RepeatFrequency.fromOrdinal(note.repeatFrequency)

        // A disabled reminder or a completed one-off task means this job is an
        // orphan: drop it without un-checking or notifying
        if (note.reminderEnabled == 0 || (note.done && !isRepeatingTask)) return

        // Already completed within the current cycle: keep it done and stay
        // quiet, but still advance the reminder chain so future cycles fire
        val doneThisCycle = isRepeatingTask && note.done &&
                !note.taskLastDoneAt.hasRepeatIntervalPassed(frequency)

        val now = Clock.System.now().toEpochMilliseconds()
        val nextReminderAt = if (isRepeatingTask) {
            nextFutureReminderTime(note.reminderAt, frequency, now)
        } else {
            note.reminderAt
        }

        val updatedNote = note.copy(
            done = if (doneThisCycle) note.done else false,
            orderAt = if (doneThisCycle) note.orderAt else now,
            updatedAt = now,
            // One-offs consume their time on delivery (0) so reschedulers
            // can't re-fire them on every app start
            reminderAt = if (isRepeatingTask) nextReminderAt else 0L
        )
        repository.updateNote(updatedNote)

        if (isRepeatingTask && nextReminderAt > now) {
            scheduleReminder(updatedNote)
        }

        if (doneThisCycle) return

        val body = if (isRepeatingTask) "${frequency.label} reminder" else "To-do reminder"
        showNotification(title = note.text.take(100), body = body)
    }

    private fun showNotification(title: String, body: String) {
        val os = System.getProperty("os.name").lowercase()
        if (!os.contains("mac")) return
        try {
            ProcessBuilder(
                "osascript", "-e",
                "display notification \"${body.sanitized()}\" with title \"${title.sanitized()}\""
            ).start()
        } catch (e: Exception) {
            println("Error showing notification: ${e.message}")
        }
    }

    // AppleScript string literals: strip escapes, turn quotes into apostrophes
    private fun String.sanitized() = replace("\\", "").replace("\"", "'")
}
