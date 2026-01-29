@file:OptIn(ExperimentalTime::class)

package app.pentastic.utils

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


fun Long.hasBeenDays(days: Int): Boolean =
    ((Clock.System.now().toEpochMilliseconds() - this) / Constant.ONE_DAY_IN_MILLIS) >= days

fun Long.hasBeenHours(hours: Int): Boolean =
    ((Clock.System.now().toEpochMilliseconds() - this) / Constant.ONE_HOUR_IN_MILLIS) >= hours

fun Long.hasBeenMinutes(minutes: Int): Boolean =
    ((Clock.System.now().toEpochMilliseconds() - this) / Constant.ONE_MINUTE_IN_MILLIS) >= minutes

fun Long.calendarDaysSince(): Int {
    val timeZone = TimeZone.currentSystemDefault()
    val startDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone).date
    val nowDate = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds()).toLocalDateTime(timeZone).date
    return (nowDate.toEpochDays() - startDate.toEpochDays()).toInt()
}