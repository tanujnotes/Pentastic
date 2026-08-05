package app.pentastic.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceiver : BroadcastReceiver(), KoinComponent {
    private val reminderScheduler: ReminderScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // goAsync keeps the process from being killed once onReceive returns,
            // which would race the DB query and alarm re-registration
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                reminderScheduler.rescheduleAllReminders()
            }.invokeOnCompletion { pendingResult.finish() }
        }
    }
}
