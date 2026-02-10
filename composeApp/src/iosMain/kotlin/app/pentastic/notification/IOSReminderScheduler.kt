package app.pentastic.notification

import app.pentastic.data.MyRepository
import app.pentastic.data.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IOSReminderScheduler(
    private val repository: MyRepository
) : ReminderScheduler {

    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun scheduleReminder(note: Note) {
        if (note.reminderAt <= 0 || note.reminderEnabled == 0) return

        val content = UNMutableNotificationContent().apply {
            setTitle("Reminder")
            setBody(note.text.take(100))
            setSound(platform.UserNotifications.UNNotificationSound.defaultSound)
        }

        // Calculate time interval from now
        val now = NSDate().timeIntervalSince1970 * 1000
        val timeInterval = (note.reminderAt - now) / 1000.0

        if (timeInterval <= 0) return

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval,
            repeats = false
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            note.uuid,
            content,
            trigger
        )

        notificationCenter.addNotificationRequest(request) { error ->
            error?.let { println("Error scheduling notification: ${it.localizedDescription}") }
        }
    }

    override suspend fun cancelReminder(noteUuid: String) {
        notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(noteUuid))
    }

    override suspend fun rescheduleAllReminders() = withContext(Dispatchers.IO) {
        val notes = repository.getNotesWithActiveReminders()
        val now = NSDate().timeIntervalSince1970 * 1000
        notes.filter { it.reminderAt > now.toLong() }.forEach { note ->
            scheduleReminder(note)
        }
    }

    override fun hasNotificationPermission(): Boolean {
        var hasPermission = false
        notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
            hasPermission = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
        }
        return hasPermission
    }

    override suspend fun requestNotificationPermission(): Boolean = suspendCoroutine { continuation ->
        notificationCenter.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { granted, _ ->
            continuation.resume(granted)
        }
    }
}
