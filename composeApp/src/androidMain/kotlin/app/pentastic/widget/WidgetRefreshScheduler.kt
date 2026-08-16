@file:OptIn(ExperimentalTime::class)

package app.pentastic.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Wakes the widgets at the moments their content changes without any data changing.
 *
 * Two such moments, both handled by one self-rescheduling alarm chain:
 * - **midnight**, when Overdue/Today/Tomorrow all shift by a day
 * - **06:00 and 18:00**, the boundaries of the app's clock-based DAY_NIGHT theme
 *   (see [isWidgetDark]); without these the widget would sit in the wrong palette
 *   until something else repainted it
 */
object WidgetRefreshScheduler {

    const val ACTION_REFRESH = "app.pentastic.WIDGET_REFRESH"

    /** Local hours at which the widget must repaint. Midnight is handled separately. */
    private val THEME_BOUNDARY_HOURS = listOf(6, 18)

    /** Past the boundary, mirroring TimelinePage's 250ms rollover buffer. */
    private const val BOUNDARY_BUFFER_MS = 1_000L

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = nextBoundary(Clock.System.now().toEpochMilliseconds())

        // Distinct action from the reminder alarms, so sharing request code 0 cannot
        // collide: PendingIntent lookup matches on Intent.filterEquals
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, WidgetUpdateReceiver::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Inexact is fine here: a widget repainting a few minutes late is cosmetic,
            // unlike a reminder that must fire on time
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, triggerAt, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, triggerAt, pendingIntent)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, WidgetUpdateReceiver::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let { alarmManager.cancel(it) }
    }

    /**
     * The soonest of today's remaining theme boundaries and tomorrow's midnight,
     * plus a small buffer so the woken code reads the new day/hour rather than
     * landing exactly on the edge.
     */
    internal fun nextBoundary(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Long {
        val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
        val candidates = THEME_BOUNDARY_HOURS.map { hour ->
            LocalDateTime(today, LocalTime(hour, 0)).toInstant(timeZone).toEpochMilliseconds()
        } + today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds()

        // Tomorrow's midnight is always ahead, so this can never come back empty
        return candidates.filter { it > nowMillis }.min() + BOUNDARY_BUFFER_MS
    }
}
