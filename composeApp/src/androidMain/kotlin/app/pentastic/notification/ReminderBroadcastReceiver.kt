package app.pentastic.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import app.pentastic.MainActivity
import app.pentastic.R

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteUuid = intent.getStringExtra("note_uuid") ?: return
        val noteText = intent.getStringExtra("note_text") ?: "Task reminder"
        val notePageId = intent.getLongExtra("note_page_id", -1L)

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        // Create intent to open the app at the specific page
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_page_id", notePageId)
            putExtra("note_uuid", noteUuid)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            noteUuid.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create intent for "Mark as done" action
        val markDoneIntent = Intent(context, MarkDoneReceiver::class.java).apply {
            action = "app.pentastic.ACTION_MARK_DONE"
            putExtra("note_uuid", noteUuid)
        }

        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            noteUuid.hashCode() + 1, // Different request code to avoid conflict
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Reminder")
            .setContentText(noteText.take(100))
            .setStyle(NotificationCompat.BigTextStyle().bigText(noteText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                0, // No icon for action button (text only)
                "Mark as done",
                markDonePendingIntent
            )
            .build()

        notificationManager.notify(noteUuid.hashCode(), notification)
    }
}
