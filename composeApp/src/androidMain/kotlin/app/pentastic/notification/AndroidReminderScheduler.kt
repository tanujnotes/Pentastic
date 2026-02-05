package app.pentastic.notification

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import app.pentastic.data.MyRepository
import app.pentastic.data.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidReminderScheduler(
    private val context: Context,
    private val repository: MyRepository
) : ReminderScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun scheduleReminder(note: Note) {
        if (note.reminderAt <= 0 || note.reminderEnabled == 0) return

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = "app.pentastic.REMINDER_NOTIFICATION"
            putExtra("note_id", note.id)
            putExtra("note_uuid", note.uuid)
            putExtra("note_text", note.text)
            putExtra("note_page_id", note.pageId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            note.uuid.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    note.reminderAt,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    note.reminderAt,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                note.reminderAt,
                pendingIntent
            )
        }
    }

    override suspend fun cancelReminder(noteId: Long, noteUuid: String) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            noteUuid.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    override suspend fun rescheduleAllReminders() = withContext(Dispatchers.IO) {
        val notes = repository.getNotesWithActiveReminders()
        val now = System.currentTimeMillis()
        notes.filter { it.reminderAt > now }.forEach { note ->
            scheduleReminder(note)
        }
    }

    override fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override suspend fun requestNotificationPermission(): Boolean {
        return hasNotificationPermission()
    }
}
