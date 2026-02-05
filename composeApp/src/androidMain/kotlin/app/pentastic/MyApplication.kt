package app.pentastic

import android.app.Application
import app.pentastic.di.initKoin
import app.pentastic.notification.NotificationHelper
import org.koin.android.ext.koin.androidContext

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MyApplication)
        }
        NotificationHelper.createNotificationChannel(this)
    }
}