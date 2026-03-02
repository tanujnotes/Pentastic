package app.pentastic.notification

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import app.pentastic.db.PentasticDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SnoozeReceiver : BroadcastReceiver(), KoinComponent {

    private val database: PentasticDatabase by inject()

    companion object {
        const val SNOOZE_DURATION_MS = 60 * 60 * 1000L // 15 minutes
    }

    override fun onReceive(context: Context, intent: Intent) {
        val noteUuid = intent.getStringExtra("note_uuid") ?: return

        // Dismiss the current notification
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(noteUuid.hashCode())

        CoroutineScope(Dispatchers.IO).launch {
            val note = database.noteDao.getNoteByUuid(noteUuid)
            if (note == null || note.deletedAt > 0) return@launch

            val snoozeTime = Clock.System.now().toEpochMilliseconds() + SNOOZE_DURATION_MS
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val alarmIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = "app.pentastic.REMINDER_NOTIFICATION"
                putExtra("note_uuid", note.uuid)
                putExtra("note_text", note.text)
                putExtra("note_page_id", note.pageId)
                putExtra("is_snooze", true)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                noteUuid.hashCode() + 2,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent
                )
            }
        }
    }
}
