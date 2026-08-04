@file:OptIn(ExperimentalTime::class)

package app.pentastic.data

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import app.pentastic.utils.nextRepeatDate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Sentinel for dueStartAt/dueEndAt: task is due "Someday" (no concrete dates). */
const val DUE_SOMEDAY = -1L

enum class DueDateOption(val label: String) {
    TODAY("Today"),
    TOMORROW("Tomorrow"),
    THIS_WEEK("This week"),
    NEXT_WEEK("Next week"),
    THIS_MONTH("This month"),
    NEXT_MONTH("Next month"),
    THIS_YEAR("This year"),
    NEXT_YEAR("Next year"),
    SOMEDAY("Someday"),
    CUSTOM("Custom"),
}

/**
 * Resolves a preset to its full calendar block (weeks are Mon-Sun, weekend is Sat-Sun),
 * even if the block started before [today]. Null for SOMEDAY and CUSTOM.
 */
fun DueDateOption.resolveRange(today: LocalDate): Pair<LocalDate, LocalDate>? {
    val weekStart = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    val monthStart = LocalDate(today.year, today.month, 1)
    return when (this) {
        DueDateOption.TODAY -> today to today
        DueDateOption.TOMORROW -> today.plus(1, DateTimeUnit.DAY).let { it to it }
        DueDateOption.THIS_WEEK -> weekStart to weekStart.plus(6, DateTimeUnit.DAY)
        DueDateOption.NEXT_WEEK -> weekStart.plus(7, DateTimeUnit.DAY) to weekStart.plus(13, DateTimeUnit.DAY)
        DueDateOption.THIS_MONTH -> monthStart to monthStart.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        DueDateOption.NEXT_MONTH -> monthStart.plus(1, DateTimeUnit.MONTH) to monthStart.plus(2, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        DueDateOption.THIS_YEAR -> LocalDate(today.year, 1, 1) to LocalDate(today.year, 12, 31)
        DueDateOption.NEXT_YEAR -> LocalDate(today.year + 1, 1, 1) to LocalDate(today.year + 1, 12, 31)
        DueDateOption.SOMEDAY, DueDateOption.CUSTOM -> null
    }
}

fun LocalDate.toStartOfDayMillis(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long =
    atStartOfDayIn(timeZone).toEpochMilliseconds()

fun epochMillisToLocalDate(millis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone).date

val Note.hasDueDate: Boolean get() = dueStartAt != 0L

val Note.isDueSomeday: Boolean get() = dueStartAt == DUE_SOMEDAY

fun Note.isDueOverdue(nowMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): Boolean =
    dueStartAt > 0 && epochMillisToLocalDate(dueEndAt, timeZone) < epochMillisToLocalDate(nowMillis, timeZone)

fun formatDueDateLabel(
    dueStartAt: Long,
    dueEndAt: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    if (dueStartAt == DUE_SOMEDAY) return "Someday"
    val start = epochMillisToLocalDate(dueStartAt, timeZone)
    val end = epochMillisToLocalDate(dueEndAt, timeZone)
    val currentYear = Clock.System.now().toLocalDateTime(timeZone).date.year

    if (start.year != end.year) {
        return "${start.dayOfMonth} ${monthAbbrev(start.month)} ${twoDigitYear(start.year)} – " +
                "${end.dayOfMonth} ${monthAbbrev(end.month)} ${twoDigitYear(end.year)}"
    }

    val yearSuffix = if (start.year != currentYear) " ${start.year}" else ""
    return when {
        start == end -> "${start.dayOfMonth} ${monthAbbrev(start.month)}$yearSuffix"
        start.month == end.month -> "${start.dayOfMonth}–${end.dayOfMonth} ${monthAbbrev(start.month)}$yearSuffix"
        else -> "${start.dayOfMonth} ${monthAbbrev(start.month)} – ${end.dayOfMonth} ${monthAbbrev(end.month)}$yearSuffix"
    }
}

private fun monthAbbrev(month: Month): String =
    month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }

private fun twoDigitYear(year: Int): String =
    (year % 100).toString().padStart(2, '0')

/** Fixed timeline sections in display order (enum order is the on-screen order). */
enum class TimelineSection(val label: String) {
    OVERDUE("Overdue"),
    TODAY("Today"),
    TOMORROW("Tomorrow"),
    THIS_WEEK("This week"),
    THIS_WEEKEND("This weekend"),
    NEXT_WEEK("Next week"),
    THIS_MONTH("This month"),
    NEXT_MONTH("Next month"),
    SOMEDAY("Someday"),
}

/** A timeline bucket: one of the fixed sections, or a year section beyond next month. */
sealed interface TimelineBucket {
    data class Section(val section: TimelineSection) : TimelineBucket
    data class Year(val year: Int) : TimelineBucket
}

/**
 * Buckets a due range by its END date (deadline semantics). Checks run top to
 * bottom, first match wins, so each task lands in exactly one bucket.
 * Ranges ending beyond next month bucket into the end date's year.
 * Returns null when there is no due date.
 */
fun classifyDueDate(
    dueStartAt: Long,
    dueEndAt: Long,
    today: LocalDate,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): TimelineBucket? {
    if (dueStartAt == 0L) return null
    if (dueStartAt == DUE_SOMEDAY) return TimelineBucket.Section(TimelineSection.SOMEDAY)

    val start = epochMillisToLocalDate(dueStartAt, timeZone)
    val end = epochMillisToLocalDate(dueEndAt, timeZone)
    val weekStart = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    val monthStart = LocalDate(today.year, today.month, 1)
    val thisMonthEnd = monthStart.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
    val nextMonthEnd = monthStart.plus(2, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

    if (end < today) return TimelineBucket.Section(TimelineSection.OVERDUE)

    // Whole calendar blocks keep their period's section until they lapse, instead of
    // migrating into Today/Tomorrow as the period closes (e.g. "This week" on a Sunday)
    val isWeekBlock = start.dayOfWeek.isoDayNumber == 1 && end == start.plus(6, DateTimeUnit.DAY)
    val isWeekendBlock = start.dayOfWeek.isoDayNumber == 6 && end == start.plus(1, DateTimeUnit.DAY)
    if (isWeekBlock || isWeekendBlock) {
        val blockWeekStart = if (isWeekBlock) start else start.minus(5, DateTimeUnit.DAY)
        if (blockWeekStart == weekStart) {
            return TimelineBucket.Section(
                if (isWeekendBlock) TimelineSection.THIS_WEEKEND else TimelineSection.THIS_WEEK
            )
        }
        if (blockWeekStart == weekStart.plus(7, DateTimeUnit.DAY)) return TimelineBucket.Section(TimelineSection.NEXT_WEEK)
    }
    val isMonthBlock = start.dayOfMonth == 1 &&
            end == LocalDate(start.year, start.month, 1).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
    if (isMonthBlock) {
        if (start == monthStart) return TimelineBucket.Section(TimelineSection.THIS_MONTH)
        if (start == monthStart.plus(1, DateTimeUnit.MONTH)) return TimelineBucket.Section(TimelineSection.NEXT_MONTH)
    }
    val isYearBlock = start == LocalDate(start.year, 1, 1) && end == LocalDate(start.year, 12, 31)
    if (isYearBlock) return TimelineBucket.Year(end.year)

    return when {
        end == today -> TimelineBucket.Section(TimelineSection.TODAY)
        end == today.plus(1, DateTimeUnit.DAY) -> TimelineBucket.Section(TimelineSection.TOMORROW)
        end <= weekStart.plus(4, DateTimeUnit.DAY) -> TimelineBucket.Section(TimelineSection.THIS_WEEK)
        end <= weekStart.plus(6, DateTimeUnit.DAY) -> TimelineBucket.Section(TimelineSection.THIS_WEEKEND)
        end <= weekStart.plus(13, DateTimeUnit.DAY) -> TimelineBucket.Section(TimelineSection.NEXT_WEEK)
        end <= thisMonthEnd -> TimelineBucket.Section(TimelineSection.THIS_MONTH)
        end <= nextMonthEnd -> TimelineBucket.Section(TimelineSection.NEXT_MONTH)
        else -> TimelineBucket.Year(end.year)
    }
}

/**
 * The date a repeat task's current cycle is (or will be) due. For a pending task this
 * is the day it came due — its start date if never completed, otherwise one interval
 * after the last completion. For a completed task it is the upcoming comeback date.
 * A future "start from" acts as a floor so an edited schedule waits for its start.
 */
fun Note.repeatOccurrenceDate(
    today: LocalDate,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): LocalDate? {
    val frequency = RepeatFrequency.fromOrdinal(repeatFrequency)
    if (frequency == RepeatFrequency.NONE) return null
    val base = when {
        taskLastDoneAt > 0 -> nextRepeatDate(epochMillisToLocalDate(taskLastDoneAt, timeZone), frequency)
        repeatTaskStartFrom > 0 -> epochMillisToLocalDate(repeatTaskStartFrom, timeZone)
        else -> today
    }
    val startFrom = if (repeatTaskStartFrom > 0) epochMillisToLocalDate(repeatTaskStartFrom, timeZone) else null
    return if (startFrom != null && startFrom > base) startFrom else base
}

/**
 * Buckets a repeat task by its virtual occurrence instead of due dates (the two are
 * mutually exclusive). Repeat tasks surface only inside their frequency's lead window:
 * daily on the day itself, weekly/monthly/quarterly one day ahead, yearly once the
 * occurrence's calendar week begins. A pending task past its day goes to Overdue.
 * Returns null while the task is outside its window (hidden from the timeline).
 */
fun classifyRepeatTask(
    note: Note,
    today: LocalDate,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): TimelineBucket? {
    val frequency = RepeatFrequency.fromOrdinal(note.repeatFrequency)
    val occurrence = note.repeatOccurrenceDate(today, timeZone) ?: return null
    if (!note.done && occurrence < today) return TimelineBucket.Section(TimelineSection.OVERDUE)
    val horizon = when (frequency) {
        RepeatFrequency.NONE -> return null
        RepeatFrequency.DAILY -> today
        RepeatFrequency.WEEKLY,
        RepeatFrequency.MONTHLY,
        RepeatFrequency.QUARTERLY -> today.plus(1, DateTimeUnit.DAY)

        RepeatFrequency.YEARLY -> today.plus(7 - today.dayOfWeek.isoDayNumber, DateTimeUnit.DAY)
    }
    if (occurrence > horizon) return null
    // A done task whose comeback date is not in the future just means the reset
    // hasn't flipped it yet — it belongs in Today
    val effective = if (occurrence < today) today else occurrence
    val effectiveMillis = effective.toStartOfDayMillis(timeZone)
    return classifyDueDate(effectiveMillis, effectiveMillis, today, timeZone)
}

/**
 * The full due-date block written when a task is dropped into a timeline section.
 * Keys match TimelinePage's section keys: TimelineSection names plus "YEAR_<y>".
 * Returns null for sections that are not drop targets (Overdue and the
 * timeline-page-only Unscheduled/Completed safety nets).
 */
fun timelineSectionDropRange(
    sectionKey: String,
    today: LocalDate,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Pair<Long, Long>? {
    if (sectionKey == TimelineSection.SOMEDAY.name) return DUE_SOMEDAY to DUE_SOMEDAY
    if (sectionKey.startsWith("YEAR_")) {
        val year = sectionKey.removePrefix("YEAR_").toIntOrNull() ?: return null
        return LocalDate(year, 1, 1).toStartOfDayMillis(timeZone) to
                LocalDate(year, 12, 31).toStartOfDayMillis(timeZone)
    }
    val section = TimelineSection.entries.firstOrNull { it.name == sectionKey } ?: return null
    val range = when (section) {
        TimelineSection.TODAY -> DueDateOption.TODAY.resolveRange(today)!!
        TimelineSection.TOMORROW -> DueDateOption.TOMORROW.resolveRange(today)!!
        TimelineSection.THIS_WEEK -> DueDateOption.THIS_WEEK.resolveRange(today)!!
        // No DueDateOption for the weekend: Sat-Sun of the current week
        TimelineSection.THIS_WEEKEND -> {
            val weekStart = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
            weekStart.plus(5, DateTimeUnit.DAY) to weekStart.plus(6, DateTimeUnit.DAY)
        }

        TimelineSection.NEXT_WEEK -> DueDateOption.NEXT_WEEK.resolveRange(today)!!
        TimelineSection.THIS_MONTH -> DueDateOption.THIS_MONTH.resolveRange(today)!!
        TimelineSection.NEXT_MONTH -> DueDateOption.NEXT_MONTH.resolveRange(today)!!
        TimelineSection.OVERDUE, TimelineSection.SOMEDAY -> return null
    }
    return range.first.toStartOfDayMillis(timeZone) to range.second.toStartOfDayMillis(timeZone)
}
