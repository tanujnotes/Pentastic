package app.pentastic.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.pentastic.data.NoteActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MarkDoneReceiver : BroadcastReceiver(), KoinComponent {

    private val noteActions: NoteActions by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val noteUuid = intent.getStringExtra("note_uuid") ?: return

        // Dismiss the notification
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(noteUuid.hashCode())

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            noteActions.markDone(noteUuid)
        }.invokeOnCompletion { pendingResult.finish() }
    }
}
