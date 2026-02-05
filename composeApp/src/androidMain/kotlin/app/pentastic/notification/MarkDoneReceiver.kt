package app.pentastic.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.pentastic.db.PentasticDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class MarkDoneReceiver : BroadcastReceiver(), KoinComponent {

    private val database: PentasticDatabase by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val noteUuid = intent.getStringExtra("note_uuid") ?: return

        // Dismiss the notification
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(noteUuid.hashCode())

        // Mark the note as done in the database
        CoroutineScope(Dispatchers.IO).launch {
            val note = database.noteDao.getNoteByUuid(noteUuid)
            if (note != null && !note.done) {
                val updatedNote = note.copy(
                    done = true,
                    taskLastDoneAt = Clock.System.now().toEpochMilliseconds(),
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                    orderAt = Clock.System.now().toEpochMilliseconds(),
                    // Disable reminder after marking done (unless it's a repeating task)
                    reminderEnabled = if (note.repeatFrequency > 0) note.reminderEnabled else 0
                )
                database.noteDao.updateNote(updatedNote)
            }
        }
    }
}
