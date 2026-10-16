package app.pentastic.utils

import app.pentastic.data.AppCalendar
import app.pentastic.data.TimelineBucket
import app.pentastic.data.TimelineSection
import app.pentastic.data.WeekStart

/**
 * Month and year logic for due dates, in either calendar, on plain epoch days
 * (days since 1970-01-01). Weeks stay Monday to Sunday in both calendars.
 *
 * Everything here is pure Int arithmetic so it can be tested without a date library.
 * Ranges are returned as (first day, last day), both inclusive.
 *
 * Weeks run from [weekStart] (Monday by default, Saturday for a Farsi week), and
 * the weekend is the LAST TWO days of that week — so a Saturday-start week has a
 * Thu/Fri weekend, a Monday-start week a Sat/Sun one.
 */
class DuePeriods(calendar: AppCalendar, weekStart: WeekStart = WeekStart.MONDAY) {
    private val jalali = calendar == AppCalendar.JALALI

    /** ISO weekday (Mon=1..Sun=7) the user's week starts on. */
    private val weekStartIso = weekStart.isoWeekday

    /** ISO weekday of the first weekend day: the 6th day of the user's week. */
    private val weekendStartIso = (weekStartIso + 4) % 7 + 1

    private fun parts(epochDays: Int): CalendarDate =
        if (jalali) {
            val d = JalaliCalendar.fromEpochDays(epochDays)
            CalendarDate(d.year, d.month, d.day)
        } else {
            gregorianFromEpochDays(epochDays)
        }

    private fun monthLength(year: Int, month: Int): Int =
        if (jalali) JalaliCalendar.monthLength(year, month) else gregorianMonthLength(year, month)

    private fun epochOf(year: Int, month: Int, day: Int): Int =
        if (jalali) JalaliCalendar.toEpochDays(year, month, day) else gregorianToEpochDays(year, month, day)

    /** The year of [epochDays] in this calendar. */
    fun yearOf(epochDays: Int): Int = parts(epochDays).year

    /** First and last day of a month (1..12). */
    fun monthBlock(year: Int, month: Int): Pair<Int, Int> {
        val first = epochOf(year, month, 1)
        return first to first + monthLength(year, month) - 1
    }

    /** First and last day of a year. */
    fun yearBlock(year: Int): Pair<Int, Int> =
        epochOf(year, 1, 1) to monthBlock(year, 12).second

    /** The month containing [todayEpoch], moved by [offsetMonths] (0 = this month, 1 = next). */
    fun monthRange(todayEpoch: Int, offsetMonths: Int): Pair<Int, Int> {
        val p = parts(todayEpoch)
        val index = p.year * 12 + (p.month - 1) + offsetMonths
        return monthBlock(index.floorDiv(12), index.mod(12) + 1)
    }

    /** The year containing [todayEpoch], moved by [offsetYears]. */
    fun yearRange(todayEpoch: Int, offsetYears: Int): Pair<Int, Int> =
        yearBlock(parts(todayEpoch).year + offsetYears)

    /** True when the range is exactly one whole month. */
    fun isMonthBlock(start: Int, end: Int): Boolean {
        val s = parts(start)
        return s.day == 1 && end == monthBlock(s.year, s.month).second
    }

    /** True when the range is exactly one whole year. */
    fun isYearBlock(start: Int, end: Int): Boolean {
        val s = parts(start)
        return s.month == 1 && s.day == 1 && end == yearBlock(s.year).second
    }

    /**
     * Buckets a due range by its END date (deadline semantics). Checks run top to bottom,
     * first match wins. Whole week/weekend/month/year blocks keep their section until
     * they lapse. Ranges ending beyond next month bucket into the end date's year.
     */
    fun classify(startDays: Int, endDays: Int, todayDays: Int): TimelineBucket {
        val weekStart = todayDays - ((isoWeekday(todayDays) - weekStartIso + 7) % 7)
        val (monthStart, thisMonthEnd) = monthRange(todayDays, 0)
        val (nextMonthStart, nextMonthEnd) = monthRange(todayDays, 1)

        if (endDays < todayDays) return TimelineBucket.Section(TimelineSection.OVERDUE)

        val isWeekBlock = isoWeekday(startDays) == weekStartIso && endDays == startDays + 6
        val isWeekendBlock = isoWeekday(startDays) == weekendStartIso && endDays == startDays + 1
        if (isWeekBlock || isWeekendBlock) {
            val blockWeekStart = if (isWeekBlock) startDays else startDays - 5
            if (blockWeekStart == weekStart) {
                return TimelineBucket.Section(
                    if (isWeekendBlock) TimelineSection.THIS_WEEKEND else TimelineSection.THIS_WEEK
                )
            }
            if (blockWeekStart == weekStart + 7) return TimelineBucket.Section(TimelineSection.NEXT_WEEK)
        }
        if (isMonthBlock(startDays, endDays)) {
            if (startDays == monthStart) return TimelineBucket.Section(TimelineSection.THIS_MONTH)
            if (startDays == nextMonthStart) return TimelineBucket.Section(TimelineSection.NEXT_MONTH)
        }
        if (isYearBlock(startDays, endDays)) return TimelineBucket.Year(yearOf(endDays))

        return when {
            endDays == todayDays -> TimelineBucket.Section(TimelineSection.TODAY)
            endDays == todayDays + 1 -> TimelineBucket.Section(TimelineSection.TOMORROW)
            endDays <= weekStart + 4 -> TimelineBucket.Section(TimelineSection.THIS_WEEK)
            endDays <= weekStart + 6 -> TimelineBucket.Section(TimelineSection.THIS_WEEKEND)
            endDays <= weekStart + 13 -> TimelineBucket.Section(TimelineSection.NEXT_WEEK)
            endDays <= thisMonthEnd -> TimelineBucket.Section(TimelineSection.THIS_MONTH)
            endDays <= nextMonthEnd -> TimelineBucket.Section(TimelineSection.NEXT_MONTH)
            else -> TimelineBucket.Year(yearOf(endDays))
        }
    }

    private companion object {
        /** ISO weekday of an epoch day: 1 = Monday ... 7 = Sunday. 1970-01-01 was a Thursday. */
        fun isoWeekday(epochDays: Int): Int = ((epochDays + 3) % 7 + 7) % 7 + 1
    }
}

internal fun gregorianMonthLength(year: Int, month: Int): Int = when (month) {
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

/** Epoch day of a Gregorian date (Howard Hinnant's days-from-civil algorithm). */
internal fun gregorianToEpochDays(year: Int, month: Int, day: Int): Int {
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val mp = if (month > 2) month - 3 else month + 9
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}
