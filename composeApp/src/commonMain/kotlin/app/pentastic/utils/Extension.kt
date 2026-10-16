@file:OptIn(ExperimentalTime::class)

package app.pentastic.utils

import app.pentastic.data.AppCalendar
import app.pentastic.data.RepeatFrequency
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
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
fun Long.hasRepeatIntervalPassed(
    frequency: RepeatFrequency,
    calendar: AppCalendar = AppCalendar.GREGORIAN,
): Boolean {
    val timeZone = TimeZone.currentSystemDefault()
    val startDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone).date
    val nowDate = Clock.System.now().toLocalDateTime(timeZone).date

    // Month/year arithmetic must run in the task's calendar: a "monthly" repeat
    // anchored on a Jalali date must come back on the same Jalali day, not the
    // same Gregorian day. Same clamp rule as the Gregorian branches below.
    if (calendar == AppCalendar.JALALI) {
        val start = JalaliCalendar.fromEpochDays(startDate.toEpochDays().toInt())
        val now = JalaliCalendar.fromEpochDays(nowDate.toEpochDays().toInt())
        val dayDueNow = now.day >= minOf(start.day, JalaliCalendar.monthLength(now.year, now.month))
        return when (frequency) {
            RepeatFrequency.MONTHLY -> {
                val monthsPassed = (now.year - start.year) * 12 + (now.month - start.month)
                monthsPassed > 1 || (monthsPassed == 1 && dayDueNow)
            }
            RepeatFrequency.QUARTERLY -> {
                val monthsPassed = (now.year - start.year) * 12 + (now.month - start.month)
                monthsPassed > 3 || (monthsPassed == 3 && dayDueNow)
            }
            RepeatFrequency.YEARLY -> {
                val yearsPassed = now.year - start.year
                yearsPassed > 1 || (yearsPassed == 1 && now.month >= start.month &&
                        (now.month > start.month || dayDueNow))
            }
            else -> false
        }
    }

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

/**
 * The first calendar date on which [hasRepeatIntervalPassed] becomes true for a task
 * last done (or starting) on [from]. kotlinx-datetime clamps month/year arithmetic the
 * same way as the predicate (e.g. Jan 31 + 1 month = Feb 28), keeping timeline
 * placement and the repeat reset in agreement.
 */
fun nextRepeatDate(
    from: LocalDate,
    frequency: RepeatFrequency,
    calendar: AppCalendar = AppCalendar.GREGORIAN,
): LocalDate {
    // In Jalali mode month/year steps are taken in the Jalali calendar so the
    // next occurrence lands on the same Jalali day (clamped to the Jalali
    // month length, e.g. 31st of a long month -> 30th of the short one).
    if (calendar == AppCalendar.JALALI) {
        val monthsToAdd = when (frequency) {
            RepeatFrequency.MONTHLY -> 1
            RepeatFrequency.QUARTERLY -> 3
            RepeatFrequency.YEARLY -> 12
            else -> 0
        }
        if (monthsToAdd > 0) {
            val j = JalaliCalendar.fromEpochDays(from.toEpochDays().toInt())
            val totalMonths = j.year * 12 + (j.month - 1) + monthsToAdd
            val ny = totalMonths / 12
            val nm = totalMonths % 12 + 1
            val nd = minOf(j.day, JalaliCalendar.monthLength(ny, nm))
            return LocalDate.fromEpochDays(JalaliCalendar.toEpochDays(ny, nm, nd))
        }
    }
    return when (frequency) {
        RepeatFrequency.NONE -> from
        RepeatFrequency.DAILY -> from.plus(1, DateTimeUnit.DAY)
        RepeatFrequency.WEEKLY -> from.plus(7, DateTimeUnit.DAY)
        RepeatFrequency.MONTHLY -> from.plus(1, DateTimeUnit.MONTH)
        RepeatFrequency.QUARTERLY -> from.plus(3, DateTimeUnit.MONTH)
        RepeatFrequency.YEARLY -> from.plus(1, DateTimeUnit.YEAR)
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