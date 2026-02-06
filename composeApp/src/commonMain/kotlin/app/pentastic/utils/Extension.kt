@file:OptIn(ExperimentalTime::class)

package app.pentastic.utils

import app.pentastic.data.RepeatFrequency
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

/**
 * Checks if enough calendar time has passed since this timestamp for the given repeat frequency.
 * Uses actual calendar-based logic for monthly/quarterly/yearly repeats to handle edge cases
 * like a task set on the 31st of a month.
 */
fun Long.hasRepeatIntervalPassed(frequency: RepeatFrequency): Boolean {
    val timeZone = TimeZone.currentSystemDefault()
    val startDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone).date
    val nowDate = Clock.System.now().toLocalDateTime(timeZone).date

    return when (frequency) {
        RepeatFrequency.NONE -> false
        RepeatFrequency.DAILY -> {
            nowDate > startDate
        }

        RepeatFrequency.WEEKLY -> {
            val daysPassed = nowDate.toEpochDays() - startDate.toEpochDays()
            daysPassed >= 7
        }

        RepeatFrequency.MONTHLY -> {
            val monthsPassed = (nowDate.year - startDate.year) * 12 + (nowDate.monthNumber - startDate.monthNumber)
            if (monthsPassed > 1) {
                true
            } else if (monthsPassed == 1) {
                nowDate.dayOfMonth >= minOf(startDate.dayOfMonth, daysInMonth(nowDate.month, nowDate.year))
            } else {
                false
            }
        }

        RepeatFrequency.QUARTERLY -> {
            val monthsPassed = (nowDate.year - startDate.year) * 12 + (nowDate.monthNumber - startDate.monthNumber)
            if (monthsPassed > 3) {
                true
            } else if (monthsPassed == 3) {
                nowDate.dayOfMonth >= minOf(startDate.dayOfMonth, daysInMonth(nowDate.month, nowDate.year))
            } else {
                false
            }
        }

        RepeatFrequency.YEARLY -> {
            val yearsPassed = nowDate.year - startDate.year
            if (yearsPassed > 1) {
                true
            } else if (yearsPassed == 1) {
                if (nowDate.monthNumber > startDate.monthNumber) {
                    true
                } else if (nowDate.monthNumber == startDate.monthNumber) {
                    nowDate.dayOfMonth >= minOf(startDate.dayOfMonth, daysInMonth(nowDate.month, nowDate.year))
                } else {
                    false
                }
            } else {
                false
            }
        }
    }
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

private fun daysInMonth(month: kotlinx.datetime.Month, year: Int): Int {
    return when (month) {
        kotlinx.datetime.Month.JANUARY -> 31
        kotlinx.datetime.Month.FEBRUARY -> if (isLeapYear(year)) 29 else 28
        kotlinx.datetime.Month.MARCH -> 31
        kotlinx.datetime.Month.APRIL -> 30
        kotlinx.datetime.Month.MAY -> 31
        kotlinx.datetime.Month.JUNE -> 30
        kotlinx.datetime.Month.JULY -> 31
        kotlinx.datetime.Month.AUGUST -> 31
        kotlinx.datetime.Month.SEPTEMBER -> 30
        kotlinx.datetime.Month.OCTOBER -> 31
        kotlinx.datetime.Month.NOVEMBER -> 30
        kotlinx.datetime.Month.DECEMBER -> 31
    }
}