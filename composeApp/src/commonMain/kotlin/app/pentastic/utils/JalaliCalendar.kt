package app.pentastic.utils

/** A date in the Persian (Jalali / Solar Hijri) calendar. [month] is 1 (Farvardin) to 12 (Esfand). */
data class JalaliDate(val year: Int, val month: Int, val day: Int)

/**
 * Persian (Jalali / Solar Hijri) calendar arithmetic on plain epoch days
 * (days since 1970-01-01, the same unit as kotlinx-datetime's `LocalDate.toEpochDays()`).
 *
 * This is a Kotlin port of the Borkowski algorithm as implemented in jalaali-js
 * (https://github.com/jalaali/jalaali-js), which follows Kazimierz M. Borkowski,
 * "The Persian calendar for 3000 years" (1996). It is exact for Jalali years
 * [MIN_YEAR]..[MAX_YEAR].
 *
 * jalaali-js is MIT licensed, Copyright (c) 2020 Behrang Norouzinia.
 *
 * Out-of-range inputs are clamped instead of throwing, so a corrupt stored value
 * can never crash the UI that displays it.
 */
object JalaliCalendar {
    const val MIN_YEAR = -61
    const val MAX_YEAR = 3177

    /** Jalali years that begin a new 33-year leap cycle. */
    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
        1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
    )

    /** Julian Day Number of 1970-01-01, the day that epoch day 0 refers to. */
    private const val EPOCH_JDN = 2440588

    private val FIRST_JDN = jalaliToJdn(MIN_YEAR, 1, 1)
    private val LAST_JDN = jalaliToJdn(MAX_YEAR, 12, monthLength(MAX_YEAR, 12))

    fun isLeapYear(year: Int): Boolean {
        val c = cycle(year)
        return leapFromCycle(c.jump, c.n) == 0
    }

    fun monthLength(year: Int, month: Int): Int = when {
        month <= 6 -> 31
        month <= 11 -> 30
        else -> if (isLeapYear(year)) 30 else 29
    }

    fun isValid(year: Int, month: Int, day: Int): Boolean =
        year in MIN_YEAR..MAX_YEAR &&
                month in 1..12 &&
                day in 1..monthLength(year, month)

    /** Epoch days of the given Jalali date. The date is assumed valid. */
    fun toEpochDays(year: Int, month: Int, day: Int): Int =
        jalaliToJdn(year, month, day) - EPOCH_JDN

    fun toEpochDays(date: JalaliDate): Int = toEpochDays(date.year, date.month, date.day)

    fun fromEpochDays(epochDays: Int): JalaliDate {
        val jdn = (epochDays + EPOCH_JDN).coerceIn(FIRST_JDN, LAST_JDN)

        // Walk to the Gregorian year, then to its Jalali counterpart.
        var jy = minOf(jdnToGregorianYear(jdn) - 621, MAX_YEAR)
        val c = cycle(jy)
        val firstOfYear = gregorianToJdn(c.gy, 3, c.march)

        // Days since 1 Farvardin.
        var k = jdn - firstOfYear
        if (k >= 0) {
            if (k <= 185) {
                // The first six months have 31 days each.
                return JalaliDate(jy, 1 + k / 31, k % 31 + 1)
            }
            // The remaining months have 30 days each.
            k -= 186
        } else {
            // The day belongs to the previous Jalali year.
            jy -= 1
            k += 179
            if (leapFromCycle(c.jump, c.n) == 1) k += 1
        }
        return JalaliDate(jy, 7 + k / 30, k % 30 + 1)
    }

    /**
     * Weekday of an epoch day, counted from Saturday, the first day of the Persian
     * week: 0 = Saturday, 1 = Sunday, ... 6 = Friday. 1970-01-01 was a Thursday.
     */
    fun weekdayFromSaturday(epochDays: Int): Int = ((epochDays + 5) % 7 + 7) % 7

    // ---------------------------------------------------------------------
    // Internals (integer division and % truncate toward zero, as in the original)
    // ---------------------------------------------------------------------

    private class Cycle(val gy: Int, val march: Int, val jump: Int, val n: Int)

    /** Locates [jy] in the cycle table and computes the Gregorian date of 1 Farvardin. */
    private fun cycle(jy: Int): Cycle {
        val y = jy.coerceIn(MIN_YEAR, MAX_YEAR)
        val gy = y + 621
        var leapJ = -14
        var jp = BREAKS[0]
        var jump = 0

        for (i in 1 until BREAKS.size) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (y < jm) break
            leapJ += jump / 33 * 8 + (jump % 33) / 4
            jp = jm
        }
        val n = y - jp

        // Persian leap years from AD 621 to the beginning of year y.
        leapJ += n / 33 * 8 + (n % 33 + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1

        // The same count for the Gregorian calendar up to year gy.
        val leapG = gy / 4 - ((gy / 100 + 1) * 3) / 4 - 150

        return Cycle(gy, 20 + leapJ - leapG, jump, n)
    }

    /** Years since the last leap year (0..4); 0 means the year is a leap year. */
    private fun leapFromCycle(jump: Int, n: Int): Int {
        var adjusted = n
        if (jump - n < 6) adjusted = n - jump + (jump + 4) / 33 * 33
        var leap = ((adjusted + 1) % 33 - 1) % 4
        if (leap == -1) leap = 4
        return leap
    }

    private fun jalaliToJdn(jy: Int, jm: Int, jd: Int): Int {
        val c = cycle(jy)
        return gregorianToJdn(c.gy, 3, c.march) + (jm - 1) * 31 - jm / 7 * (jm - 7) + jd - 1
    }

    private fun gregorianToJdn(gy: Int, gm: Int, gd: Int): Int {
        var d = (gy + (gm - 8) / 6 + 100100) * 1461 / 4 +
                (153 * ((gm + 9) % 12) + 2) / 5 +
                gd - 34840408
        d = d - ((gy + 100100 + (gm - 8) / 6) / 100 * 3) / 4 + 752
        return d
    }

    private fun jdnToGregorianYear(jdn: Int): Int {
        var j = 4 * jdn + 139361631
        j += ((4 * jdn + 183187720) / 146097 * 3) / 4 * 4 - 3908
        val i = (j % 1461) / 4 * 5 + 308
        val gm = (i / 153) % 12 + 1
        return j / 1461 - 100100 + (8 - gm) / 6
    }
}