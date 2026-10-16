package app.pentastic.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import app.pentastic.data.AppLanguage
import app.pentastic.data.DataStoreRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object NotificationHelper : KoinComponent {
    const val CHANNEL_ID = "pentastic_reminders"

    fun createNotificationChannel(context: Context) {
        // The channel name follows the saved language. Android updates the channel
        // when it is re-created with the same ID, so a language change in Settings
        // applies to the channel after the next app start.
        val texts = runBlocking {
            NotificationStrings(
                AppLanguage.fromOrdinal(
                    get<DataStoreRepository>().language.first()
                )
            )
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            texts.channelName(),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = texts.channelDescription()
            enableVibration(true)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
