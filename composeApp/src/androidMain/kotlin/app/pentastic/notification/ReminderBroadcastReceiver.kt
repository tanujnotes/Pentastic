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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
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

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        // Reset the task (mark as not done), determine notification title, and schedule next reminder
        CoroutineScope(Dispatchers.IO).launch {
            var notificationBody = "To-do reminder"

            val note = database.noteDao.getNoteByUuid(noteUuid)
            if (note == null || note.deletedAt > 0) {
                // Cancel the stale alarm for deleted/trashed notes
                reminderScheduler.cancelReminder(noteUuid)
                return@launch
            }

            val now = Clock.System.now().toEpochMilliseconds()
            val isRepeatingTask = note.repeatFrequency > 0
            val frequency = RepeatFrequency.fromOrdinal(note.repeatFrequency)

            notificationBody = if (isRepeatingTask)
                "${frequency.label} reminder"
            else
                "To-do reminder"

            // Calculate next reminder time for repeating tasks
            val nextReminderAt = if (isRepeatingTask && note.reminderEnabled == 1) {
                calculateNextReminderTime(note.reminderAt, frequency)
            } else {
                note.reminderAt
            }

            val updatedNote = note.copy(
                done = false,
                orderAt = now,
                updatedAt = now,
                reminderAt = nextReminderAt
            )
            database.noteDao.updateNote(updatedNote)

            // Schedule the next reminder for repeating tasks
            if (isRepeatingTask && note.reminderEnabled == 1 && nextReminderAt > now) {
                reminderScheduler.scheduleReminder(updatedNote)
            }

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

            val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pentastic_small)
                .setContentTitle(noteText)
                .setContentText(notificationBody)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .addAction(
                    0,
                    "Mark as done",
                    markDonePendingIntent
                )
                .build()

            notificationManager.notify(noteUuid.hashCode(), notification)
        }
    }

    private fun calculateNextReminderTime(currentReminderAt: Long, frequency: RepeatFrequency): Long {
        val timeZone = TimeZone.currentSystemDefault()
        val currentDateTime = Instant.fromEpochMilliseconds(currentReminderAt).toLocalDateTime(timeZone)
        val reminderTime = LocalTime(currentDateTime.hour, currentDateTime.minute)

        val nextDate = when (frequency) {
            RepeatFrequency.NONE -> currentDateTime.date
            RepeatFrequency.DAILY -> currentDateTime.date.plus(1, DateTimeUnit.DAY)
            RepeatFrequency.WEEKLY -> currentDateTime.date.plus(7, DateTimeUnit.DAY)
            RepeatFrequency.MONTHLY -> currentDateTime.date.plus(1, DateTimeUnit.MONTH)
            RepeatFrequency.QUARTERLY -> currentDateTime.date.plus(3, DateTimeUnit.MONTH)
            RepeatFrequency.YEARLY -> currentDateTime.date.plus(1, DateTimeUnit.YEAR)
        }

        return LocalDateTime(nextDate, reminderTime).toInstant(timeZone).toEpochMilliseconds()
    }
}
