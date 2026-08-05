package app.pentastic.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import app.pentastic.MainActivity
import app.pentastic.R
import app.pentastic.data.RepeatFrequency
import app.pentastic.db.PentasticDatabase
import app.pentastic.utils.hasRepeatIntervalPassed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ReminderBroadcastReceiver : BroadcastReceiver(), KoinComponent {
    private val database: PentasticDatabase by inject()
    private val reminderScheduler: ReminderScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val noteUuid = intent.getStringExtra("note_uuid") ?: return
        val noteText = intent.getStringExtra("note_text") ?: "Task reminder"
        val notePageId = intent.getLongExtra("note_page_id", -1L)
        val isSnooze = intent.getBooleanExtra("is_snooze", false)

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        // Reset the task (mark as not done), determine notification title, and schedule next reminder
        // goAsync keeps the process from being killed once onReceive returns, which
        // would race the DB write and next-cycle scheduling
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            var notificationBody = "To-do reminder"

            val note = database.noteDao.getNoteByUuid(noteUuid)
            if (note == null || note.deletedAt > 0) {
                // Cancel the stale alarm for deleted/trashed notes
                reminderScheduler.cancelReminder(noteUuid)
                return@launch
            }

            val isRepeatingTask = note.repeatFrequency > 0
            val frequency = RepeatFrequency.fromOrdinal(note.repeatFrequency)

            // A disabled reminder or a completed one-off task means this alarm is an
            // orphan (scheduled before the reminder was turned off or the task was
            // done): drop it without un-checking or notifying
            if (note.reminderEnabled == 0 || (note.done && !isRepeatingTask)) {
                reminderScheduler.cancelReminder(noteUuid)
                return@launch
            }

            // Already completed within the current cycle (e.g. done at 8 AM before a
            // 9 AM reminder): keep it done and stay quiet, but still advance the
            // reminder chain below so future cycles fire
            val doneThisCycle = isRepeatingTask && note.done &&
                    !note.taskLastDoneAt.hasRepeatIntervalPassed(frequency)

            notificationBody = if (isRepeatingTask)
                "${frequency.label} reminder"
            else
                "To-do reminder"

            if (!isSnooze) {
                val now = Clock.System.now().toEpochMilliseconds()

                // Calculate next reminder time for repeating tasks. Fast-forwarding
                // (not a single +interval step) keeps the chain alive when this
                // alarm was delivered more than one interval late
                val nextReminderAt = if (isRepeatingTask && note.reminderEnabled == 1) {
                    nextFutureReminderTime(note.reminderAt, frequency, now)
                } else {
                    note.reminderAt
                }

                val updatedNote = note.copy(
                    done = if (doneThisCycle) note.done else false,
                    orderAt = if (doneThisCycle) note.orderAt else now,
                    updatedAt = now,
                    // One-offs consume their time on delivery (0) so reschedulers
                    // can't re-fire them on every app start; snooze still works
                    // because it only needs the uuid, not the stored time
                    reminderAt = if (isRepeatingTask) nextReminderAt else 0L
                )
                database.noteDao.updateNote(updatedNote)

                // Schedule the next reminder for repeating tasks
                if (isRepeatingTask && note.reminderEnabled == 1 && nextReminderAt > now) {
                    reminderScheduler.scheduleReminder(updatedNote)
                }
            }

            if (doneThisCycle) return@launch

            // Build and show notification after DB lookup
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to_page_id", notePageId)
                putExtra("note_uuid", noteUuid)
            }

            val contentPendingIntent = PendingIntent.getActivity(
                context,
                noteUuid.hashCode(),
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val markDoneIntent = Intent(context, MarkDoneReceiver::class.java).apply {
                action = "app.pentastic.ACTION_MARK_DONE"
                putExtra("note_uuid", noteUuid)
            }

            val markDonePendingIntent = PendingIntent.getBroadcast(
                context,
                noteUuid.hashCode() + 1,
                markDoneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
                action = "app.pentastic.ACTION_SNOOZE"
                putExtra("note_uuid", noteUuid)
            }

            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                noteUuid.hashCode() + 2,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pentastic_small)
                .setContentTitle(noteText)
                .setContentText(notificationBody)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .addAction(0, "Mark as done", markDonePendingIntent)
                .addAction(0, "Snooze (1 hour)", snoozePendingIntent)
                .build()

            notificationManager.notify(noteUuid.hashCode(), notification)
        }.invokeOnCompletion { pendingResult.finish() }
    }
}
