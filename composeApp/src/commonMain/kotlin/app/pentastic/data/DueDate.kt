@file:OptIn(ExperimentalTime::class)

package app.pentastic.data

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import app.pentastic.utils.nextRepeatDate
import app.pentastic.data.WeekStart
import app.pentastic.utils.DateTexts
import app.pentastic.utils.DuePeriods
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
 * Resolves a preset to its full calendar block (weeks run from [weekStart], and the
 * weekend is that week's last two days), even if the block started before [today].
 * Null for SOMEDAY and CUSTOM.
 */
fun DueDateOption.resolveRange(
    today: LocalDate,
    calendar: AppCalendar = AppCalendar.GREGORIAN,
    weekStart: WeekStart = WeekStart.MONDAY,
): Pair<LocalDate, LocalDate>? {
    val weekStartDay = today.minus(
        (today.dayOfWeek.isoDayNumber - weekStart.isoWeekday + 7) % 7,
        DateTimeUnit.DAY
    )
    val periods = DuePeriods(calendar, weekStart)
    val todayDays = today.toEpochDays().toInt()
    return when (this) {
        DueDateOption.TODAY -> today to today
        DueDateOption.TOMORROW -> today.plus(1, DateTimeUnit.DAY).let { it to it }
        DueDateOption.THIS_WEEK -> weekStartDay to weekStartDay.plus(6, DateTimeUnit.DAY)
        DueDateOption.NEXT_WEEK -> weekStartDay.plus(7, DateTimeUnit.DAY) to weekStartDay.plus(13, DateTimeUnit.DAY)
        DueDateOption.THIS_MONTH -> periods.monthRange(todayDays, 0).toDates()
        DueDateOption.NEXT_MONTH -> periods.monthRange(todayDays, 1).toDates()
        DueDateOption.THIS_YEAR -> periods.yearRange(todayDays, 0).toDates()
        DueDateOption.NEXT_YEAR -> periods.yearRange(todayDays, 1).toDates()
        DueDateOption.SOMEDAY, DueDateOption.CUSTOM -> null
    }
}

private fun Pair<Int, Int>.toDates(): Pair<LocalDate, LocalDate> =
    LocalDate.fromEpochDays(first) to LocalDate.fromEpochDays(second)

/**
 * Due dates are stored as epoch days: a plain calendar date that never shifts when
 * the device timezone changes. Values above [LEGACY_DUE_MILLIS_THRESHOLD] were
 * written by pre-release builds as local start-of-day epoch millis and are decoded
 * in the current zone.
 */
fun LocalDate.toDueValue(): Long = toEpochDays().toLong()

// Unambiguous split: absurd as epoch days (~year 275000) yet earlier than any
// real millis timestamp (1970-01-02)
private const val LEGACY_DUE_MILLIS_THRESHOLD = 100_000_000L

fun dueValueToLocalDate(value: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    if (value > LEGACY_DUE_MILLIS_THRESHOLD) epochMillisToLocalDate(value, timeZone)
    else LocalDate.fromEpochDays(value.toInt())

fun epochMillisToLocalDate(millis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone).date

val Note.hasDueDate: Boolean get() = dueStartAt != 0L

val Note.isDueSomeday: Boolean get() = dueStartAt == DUE_SOMEDAY

fun Note.isDueOverdue(nowMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): Boolean =
    dueStartAt > 0 && dueValueToLocalDate(dueEndAt, timeZone) < epochMillisToLocalDate(nowMillis, timeZone)

fun formatDueDateLabel(
    dueStartAt: Long,
    dueEndAt: Long,
    texts: DateTexts,
    somedayLabel: String,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    if (dueStartAt == DUE_SOMEDAY) return somedayLabel
    val start = dueValueToLocalDate(dueStartAt, timeZone).toEpochDays().toInt()
    val end = dueValueToLocalDate(dueEndAt, timeZone).toEpochDays().toInt()
    val today = Clock.System.now().toLocalDateTime(timeZone).date.toEpochDays().toInt()
    return texts.dueRangeLabel(start, end, today)
}

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
    calendar: AppCalendar = AppCalendar.GREGORIAN,
    weekStart: WeekStart = WeekStart.MONDAY,
): TimelineBucket? {
    if (dueStartAt == 0L) return null
    if (dueStartAt == DUE_SOMEDAY) return TimelineBucket.Section(TimelineSection.SOMEDAY)

    val start = dueValueToLocalDate(dueStartAt, timeZone)
    val end = dueValueToLocalDate(dueEndAt, timeZone)
    return DuePeriods(calendar, weekStart).classify(
        startDays = start.toEpochDays().toInt(),
        endDays = end.toEpochDays().toInt(),
        todayDays = today.toEpochDays().toInt(),
    )
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
    calendar: AppCalendar = AppCalendar.GREGORIAN,
): LocalDate? {
    val frequency = RepeatFrequency.fromOrdinal(repeatFrequency)
    if (frequency == RepeatFrequency.NONE) return null
    val base = when {
        taskLastDoneAt > 0 -> nextRepeatDate(epochMillisToLocalDate(taskLastDoneAt, timeZone), frequency, calendar)
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
    calendar: AppCalendar = AppCalendar.GREGORIAN,
    weekStart: WeekStart = WeekStart.MONDAY,
): TimelineBucket? {
    val frequency = RepeatFrequency.fromOrdinal(note.repeatFrequency)
    val occurrence = note.repeatOccurrenceDate(today, timeZone, calendar) ?: return null
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
    val effectiveValue = effective.toDueValue()
    return classifyDueDate(effectiveValue, effectiveValue, today, timeZone, calendar, weekStart)
}

/**
 * The pending tasks the timeline shows under Overdue or Today — what the index row
 * counts. Mirrors TimelinePage's bucketing (repeat tasks by schedule, everything
 * else by due date) but drops completed ones, so the count matches how other index
 * rows count their pages.
 */
fun timelineUrgentNotes(
    notes: List<Note>,
    today: LocalDate,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    calendar: AppCalendar = AppCalendar.GREGORIAN,
): List<Note> = notes.filter { note ->
    if (note.done) return@filter false
    val bucket = when {
        note.repeatFrequency > 0 -> classifyRepeatTask(note, today, timeZone, calendar)
        note.hasDueDate -> classifyDueDate(note.dueStartAt, note.dueEndAt, today, timeZone, calendar)
        else -> null
    }
    bucket == TimelineBucket.Section(TimelineSection.TODAY) ||
            bucket == TimelineBucket.Section(TimelineSection.OVERDUE)
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
    calendar: AppCalendar = AppCalendar.GREGORIAN,
    weekStart: WeekStart = WeekStart.MONDAY,
): Pair<Long, Long>? {
    if (sectionKey == TimelineSection.SOMEDAY.name) return DUE_SOMEDAY to DUE_SOMEDAY
    if (sectionKey.startsWith("YEAR_")) {
        val year = sectionKey.removePrefix("YEAR_").toIntOrNull() ?: return null
        val (first, last) = DuePeriods(calendar, weekStart).yearBlock(year)
        return first.toLong() to last.toLong()
    }
    val section = TimelineSection.entries.firstOrNull { it.name == sectionKey } ?: return null
    val range = when (section) {
        TimelineSection.TODAY -> DueDateOption.TODAY.resolveRange(today)!!
        TimelineSection.TOMORROW -> DueDateOption.TOMORROW.resolveRange(today)!!
        TimelineSection.THIS_WEEK -> DueDateOption.THIS_WEEK.resolveRange(today, weekStart = weekStart)!!
        // No DueDateOption for the weekend: the last two days of the current week
        TimelineSection.THIS_WEEKEND -> {
            val weekStartDay = today.minus(
                (today.dayOfWeek.isoDayNumber - weekStart.isoWeekday + 7) % 7,
                DateTimeUnit.DAY
            )
            weekStartDay.plus(5, DateTimeUnit.DAY) to weekStartDay.plus(6, DateTimeUnit.DAY)
        }

        TimelineSection.NEXT_WEEK -> DueDateOption.NEXT_WEEK.resolveRange(today, weekStart = weekStart)!!
        TimelineSection.THIS_MONTH -> DueDateOption.THIS_MONTH.resolveRange(today, calendar)!!
        TimelineSection.NEXT_MONTH -> DueDateOption.NEXT_MONTH.resolveRange(today, calendar)!!
        TimelineSection.OVERDUE, TimelineSection.SOMEDAY -> return null
    }
    return range.first.toDueValue() to range.second.toDueValue()
}
