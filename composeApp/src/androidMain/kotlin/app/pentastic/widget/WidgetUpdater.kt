package app.pentastic.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/**
 * Repaints every placed widget. Needed because a Glance session — and with it the
 * Flow collection in provideContent — dies with the process, while the RemoteViews
 * on screen survive.
 */
object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        TodayWidget().updateAll(context)
        PageWidget().updateAll(context)
    }
}
