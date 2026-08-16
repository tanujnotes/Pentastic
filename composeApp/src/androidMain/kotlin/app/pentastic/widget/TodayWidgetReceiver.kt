package app.pentastic.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = TodayWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshScheduler.schedule(context.applicationContext)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Last widget of this type removed; the chain has nothing left to repaint
        WidgetRefreshScheduler.cancel(context.applicationContext)
    }
}
