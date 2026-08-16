package app.pentastic

import android.app.Application
import androidx.work.Configuration
import app.pentastic.di.initKoin
import app.pentastic.notification.NotificationHelper
import app.pentastic.widget.WidgetRefreshObserver
import app.pentastic.widget.WidgetRefreshScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Implements [Configuration.Provider] purely to keep WorkManager off the startup path.
 * Glance drags in androidx.work, whose default `androidx.startup` initializer opens a
 * Room database on every cold launch — for a feature that only matters once a widget
 * exists. The manifest removes that initializer; supplying a configuration here means
 * WorkManager builds itself on first actual use instead.
 */
class MyApplication : Application(), Configuration.Provider, KoinComponent {

    // Method form, not the `workManagerConfiguration` property: that arrived in
    // WorkManager 2.9.0 and Glance 1.1.1 pins 2.7.1
    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MyApplication)
        }
        NotificationHelper.createNotificationChannel(this)
        WidgetRefreshObserver.start(this, get())
        // Idempotent: setExact* replaces any alarm with the same PendingIntent
        WidgetRefreshScheduler.schedule(this)
    }
}