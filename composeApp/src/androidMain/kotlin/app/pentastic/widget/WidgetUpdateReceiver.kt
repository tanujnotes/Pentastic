package app.pentastic.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.pentastic.data.MyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Fires on the [WidgetRefreshScheduler] boundaries, and whenever the system clock or
 * time zone moves. Re-arms itself, so the chain continues from wherever it lands.
 */
class WidgetUpdateReceiver : BroadcastReceiver(), KoinComponent {

    private val repository: MyRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            // Repeat tasks completed on a previous day only come back through this
            // reset, which otherwise runs solely from the UI — without it a daily task
            // would stay invisible in the widget until the app was next opened
            repository.resetRepeatingTasksTodo()
            WidgetUpdater.updateAll(context.applicationContext)
            WidgetRefreshScheduler.schedule(context.applicationContext)
        }.invokeOnCompletion { pendingResult.finish() }
    }
}
