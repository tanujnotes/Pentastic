@file:OptIn(ExperimentalTime::class)

package app.pentastic.data

import app.pentastic.notification.ReminderScheduler
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Completing a task, in one place. Three callers need identical behaviour — the task
 * list, the widget's tap-to-complete, and the notification's "Mark as done" action —
 * and they used to be three drifting copies (the notification one never cancelled the
 * pending alarm).
 */
class NoteActions(
    private val repository: MyRepository,
    private val reminderScheduler: ReminderScheduler,
) {

    /**
     * Flips [note]'s done state. [isNotesType] preserves `orderAt` so a NOTES page
     * keeps its creation ordering instead of jumping the note to the top.
     */
    suspend fun toggleDone(note: Note, isNotesType: Boolean = false) {
        val now = Clock.System.now().toEpochMilliseconds()
        val newDoneState = !note.done
        val isRepeatingTask = note.repeatFrequency > 0

        // A repeating task keeps its reminder: the chain has to survive completion
        if (newDoneState && note.reminderEnabled == 1 && !isRepeatingTask) {
            reminderScheduler.cancelReminder(note.uuid)
        }

        repository.updateNote(
            note.copy(
                done = newDoneState,
                orderAt = if (isNotesType) note.orderAt else now,
                taskLastDoneAt = if (note.done) note.taskLastDoneAt else now,
                reminderEnabled = if (newDoneState && !isRepeatingTask) 0 else note.reminderEnabled
            )
        )
    }

    /**
     * Marks the note done by uuid, for callers that only hold the notification/widget
     * key. No-op when the note is gone or already done, so a double tap is harmless.
     */
    suspend fun markDone(noteUuid: String) {
        val note = repository.getNoteByUuid(noteUuid) ?: return
        if (note.done) return
        toggleDone(note)
    }
}
