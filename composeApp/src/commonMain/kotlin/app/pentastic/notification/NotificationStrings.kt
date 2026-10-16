package app.pentastic.notification

import app.pentastic.data.AppLanguage
import app.pentastic.data.RepeatFrequency

/**
 * User-facing strings for reminders and notifications.
 *
 * Lives outside the UI string table (AppStrings/LocalStrings) because receivers
 * and schedulers deliver notifications without a Compose composition, so they
 * cannot read AppTheme.strings; they look the active language up in DataStore
 * and hand it here instead.
 */
class NotificationStrings(private val language: AppLanguage) {
    private val isFarsi = language == AppLanguage.FARSI

    /** Body of a one-off task reminder: "To-do reminder" / "یادآور کار". */
    fun todoReminder(): String = if (isFarsi) "یادآور کار" else "To-do reminder"

    /** Body of a repeating task reminder: "Daily reminder" / "یادآور روزانه". */
    fun repeatReminder(frequency: RepeatFrequency): String =
        if (!isFarsi) {
            "${frequency.label} reminder"
        } else when (frequency) {
            RepeatFrequency.DAILY -> "یادآور روزانه"
            RepeatFrequency.WEEKLY -> "یادآور هفتگی"
            RepeatFrequency.MONTHLY -> "یادآور ماهانه"
            RepeatFrequency.QUARTERLY -> "یادآور سه‌ماهه"
            RepeatFrequency.YEARLY -> "یادآور سالانه"
            RepeatFrequency.NONE -> todoReminder()
        }

    /** "Mark as done" notification action / "انجام شد" (matches the app's markDone string). */
    fun markAsDone(): String = if (isFarsi) "انجام شد" else "Mark as done"

    /** "Snooze (1 hour)" notification action / "یادآوری ۱ ساعت دیگر". */
    fun snoozeOneHour(): String = if (isFarsi) "یادآوری ۱ ساعت دیگر" else "Snooze (1 hour)"

    /** Notification channel name / "یادآورها". */
    fun channelName(): String = if (isFarsi) "یادآورها" else "Reminders"

    /** Notification channel description / "اعلان‌های یادآوری کارها". */
    fun channelDescription(): String =
        if (isFarsi) "اعلان‌های یادآوری کارها" else "Notifications for task reminders"
}
