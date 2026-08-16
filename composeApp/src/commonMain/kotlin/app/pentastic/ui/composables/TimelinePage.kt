@file:OptIn(ExperimentalTime::class)

package app.pentastic.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.pentastic.data.Note
import app.pentastic.data.RepeatFrequency
import app.pentastic.data.TimelineBucket
import app.pentastic.data.TimelineSection
import app.pentastic.data.classifyDueDate
import app.pentastic.data.classifyRepeatTask
import app.pentastic.data.epochMillisToLocalDate
import app.pentastic.data.hasDueDate
import app.pentastic.data.livePageIds
import app.pentastic.data.liveNotes
import app.pentastic.data.timelineSectionDropRange
import app.pentastic.ui.theme.AppTheme
import app.pentastic.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.Font
import org.koin.compose.viewmodel.koinViewModel
import pentastic.composeapp.generated.resources.Merriweather_Light
import pentastic.composeapp.generated.resources.Res
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@Composable
fun TimelinePage(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val viewModel = koinViewModel<MainViewModel>()
    val notesByPage by viewModel.notesByPage.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val subPagesByParent by viewModel.subPagesByParent.collectAsState()
    val timelinePage by viewModel.timelinePage.collectAsState()

    val timeZone = TimeZone.currentSystemDefault()
    // Refreshed on resume and at each local midnight so an open session re-buckets
    // sections and un-checks lapsed repeat tasks as the day rolls over (the resume
    // effect also runs on first composition)
    var today by remember { mutableStateOf(Clock.System.now().toLocalDateTime(timeZone).date) }
    LifecycleResumeEffect(Unit) {
        today = Clock.System.now().toLocalDateTime(timeZone).date
        viewModel.resetRepeatingTasksTodo()
        onPauseOrDispose { }
    }
    // Keyed on today: each firing (or a resume-driven change) schedules the next
    // midnight; the buffer keeps the recomputed date safely past the rollover
    LaunchedEffect(today) {
        val nextMidnight = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
        delay(nextMidnight - Clock.System.now() + 250.milliseconds)
        today = Clock.System.now().toLocalDateTime(timeZone).date
        viewModel.resetRepeatingTasksTodo()
    }

    var noteForDueDateDialog by remember { mutableStateOf<Note?>(null) }
    var noteForRepeatDialog by remember { mutableStateOf<Note?>(null) }
    var noteForReminderDialog by remember { mutableStateOf<Note?>(null) }
    var noteForMoveDialog by remember { mutableStateOf<Note?>(null) }

    // Keys the user toggled away from their section's default collapse state
    var toggledSections by rememberSaveable(
        stateSaver = listSaver(
            save = { it.toList() },
            restore = { it.toSet() }
        )
    ) { mutableStateOf(setOf<String>()) }

    val sections: List<TimelineSectionUi> =
        remember(notesByPage, pages, subPagesByParent, timelinePage, today) {
            val liveNotes = liveNotes(notesByPage, livePageIds(pages, subPagesByParent, timelinePage))
            // Repeat tasks are included even when done: their next occurrence can preview
            // (e.g. a weekly task in Tomorrow the day before it comes back)
            val grouped = liveNotes
                .filter { it.repeatFrequency > 0 || (it.hasDueDate && !it.done) }
                .groupBy {
                    if (it.repeatFrequency > 0) classifyRepeatTask(it, today, timeZone)
                    else classifyDueDate(it.dueStartAt, it.dueEndAt, today, timeZone)
                }

            // Same ordering as task pages: priority tasks first, then dragged order (newest first)
            fun prioritySort(notes: List<Note>) =
                notes.sortedWith(compareByDescending<Note> { it.priority }.thenByDescending { it.orderAt })

            // Sections up to This weekend start expanded, the rest collapsed —
            // except on weekends, when the imminent Next week starts expanded too
            val isWeekendToday = today.dayOfWeek.isoDayNumber >= 6

            buildList {
                TimelineSection.entries.filter { it != TimelineSection.SOMEDAY }.forEach { section ->
                    val sectionNotes = prioritySort(grouped[TimelineBucket.Section(section)] ?: emptyList())
                    // Overdue only appears when something is actually overdue
                    if (section == TimelineSection.OVERDUE && sectionNotes.isEmpty()) return@forEach
                    add(
                        TimelineSectionUi(
                            key = section.name,
                            label = section.label,
                            subtitle = sectionSubtitle(section, today),
                            notes = sectionNotes,
                            collapsedByDefault = when {
                                section.ordinal <= TimelineSection.THIS_WEEKEND.ordinal -> false
                                section == TimelineSection.NEXT_WEEK && isWeekendToday -> false
                                else -> true
                            },
                        )
                    )
                }
                // Year sections: always this year and next, plus any further year with tasks
                val years = buildSet {
                    add(today.year)
                    add(today.year + 1)
                    grouped.keys.filterIsInstance<TimelineBucket.Year>().forEach { add(it.year) }
                }.sorted()
                years.forEach { year ->
                    add(
                        TimelineSectionUi(
                            key = "YEAR_$year",
                            label = year.toString(),
                            notes = prioritySort(grouped[TimelineBucket.Year(year)] ?: emptyList()),
                            collapsedByDefault = true,
                        )
                    )
                }
                val someday = grouped[TimelineBucket.Section(TimelineSection.SOMEDAY)] ?: emptyList()
                add(
                    TimelineSectionUi(
                        key = TimelineSection.SOMEDAY.name,
                        label = TimelineSection.SOMEDAY.label,
                        notes = prioritySort(someday),
                        collapsedByDefault = true,
                    )
                )

                // Safety nets for tasks that live ON the Timeline page itself (shown only when non-empty).
                // Those tasks have no page of their own to fall back on, so every one of
                // them has to be reachable from some section here
                val timelineOwnNotes = timelinePage?.id?.let { notesByPage[it] } ?: emptyList()
                // Repeat tasks surface above only near their next occurrence; the rest
                // wait here instead of vanishing until their window opens
                val repeating = prioritySort(
                    timelineOwnNotes.filter {
                        it.repeatFrequency > 0 && classifyRepeatTask(it, today, timeZone) == null
                    }
                )
                if (repeating.isNotEmpty()) {
                    add(
                        TimelineSectionUi(
                            key = "REPEATING",
                            label = "Repeating",
                            notes = repeating,
                            collapsedByDefault = true,
                        )
                    )
                }
                // The remaining two nets hold non-repeating tasks only: a repeat task is
                // scheduled by its cycle rather than by a due date or a finish
                val unscheduled = prioritySort(
                    timelineOwnNotes.filter { !it.done && it.dueStartAt == 0L && it.repeatFrequency == 0 }
                )
                if (unscheduled.isNotEmpty()) {
                    add(
                        TimelineSectionUi(
                            key = "UNSCHEDULED",
                            label = "Unscheduled",
                            notes = unscheduled,
                            collapsedByDefault = true,
                        )
                    )
                }
                // Completing a task on the timeline drops it out of its dated section, so
                // this section catches it for undo. Tasks that live on the Timeline page
                // stay for good (it is their only home); tasks borrowed from other pages
                // only linger a day, since their own page lists them permanently anyway.
                // Newest first, so a just-completed task lands on top.
                val timelinePageId = timelinePage?.id
                val yesterday = today.minus(1, DateTimeUnit.DAY)
                val completed = liveNotes
                    .filter { note ->
                        if (!note.done || note.repeatFrequency > 0) return@filter false
                        if (note.pageId == timelinePageId) return@filter true
                        if (!note.hasDueDate) return@filter false
                        // taskLastDoneAt is stamped on every completion path; 0 means a
                        // legacy row whose completion date is unknown, so leave it out
                        if (note.taskLastDoneAt <= 0) return@filter false
                        val doneOn = epochMillisToLocalDate(note.taskLastDoneAt, timeZone)
                        doneOn == today || doneOn == yesterday
                    }
                    .sortedByDescending { it.orderAt }
                if (completed.isNotEmpty()) {
                    add(
                        TimelineSectionUi(
                            key = "COMPLETED",
                            label = "Completed",
                            notes = completed,
                            collapsedByDefault = true,
                            isDimmed = true,
                        )
                    )
                }
            }
        }

    val lazyListState = rememberLazyListState()
    // Shadow of the derived sections that drag gestures permute synchronously; resynced on
    // every DB emission (after a drop the DB round-trip lands in the same arrangement)
    var localSections by remember { mutableStateOf(sections) }
    LaunchedEffect(sections) { localSections = sections }
    // A late-loading source (the Timeline page's own notes arrive after the first
    // render) can insert a new first section — usually Overdue — above the top row,
    // and LazyColumn's anchoring keeps the old first header pinned, hiding the new
    // section above the fold. Snap up when the viewport is still sitting on the old
    // first header; a user scrolled anywhere else is left alone.
    var previousFirstSectionKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(localSections.firstOrNull()?.key) {
        val newFirst = localSections.firstOrNull()?.key
        val oldFirst = previousFirstSectionKey
        previousFirstSectionKey = newFirst
        if (oldFirst != null && newFirst != null && oldFirst != newFirst &&
            lazyListState.firstVisibleItemScrollOffset == 0 &&
            lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.key == "header_$oldFirst"
        ) {
            lazyListState.scrollToItem(0)
        }
    }
    var draggingNoteId by remember { mutableStateOf<Long?>(null) }
    var dragOriginSectionKey by remember { mutableStateOf<String?>(null) }
    var dragDidMove by remember { mutableStateOf(false) }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val noteId = from.key as? Long ?: return@rememberReorderableLazyListState
        val src = locateNote(localSections, noteId) ?: return@rememberReorderableLazyListState
        val movingDown = to.index > from.index

        val (destSectionIdx, destNoteIdx) = when (val toKey = to.key) {
            is Long -> {
                val target = locateNote(localSections, toKey) ?: return@rememberReorderableLazyListState
                if (target.sectionIdx == src.sectionIdx) {
                    target.sectionIdx to target.noteIdx
                } else {
                    // Replicates flat-list add(to, removeAt(from)) semantics: moving down
                    // lands after the target row, moving up lands before it
                    target.sectionIdx to (if (movingDown) target.noteIdx + 1 else target.noteIdx)
                }
            }
            // Drop slot under a section header: insert at the top of that section
            is String -> {
                if (!toKey.startsWith("slot_")) return@rememberReorderableLazyListState
                val sectionIdx = localSections.indexOfFirst { it.key == toKey.removePrefix("slot_") }
                if (sectionIdx < 0) return@rememberReorderableLazyListState
                sectionIdx to 0
            }

            else -> return@rememberReorderableLazyListState
        }
        if (destSectionIdx == src.sectionIdx && destNoteIdx == src.noteIdx) return@rememberReorderableLazyListState

        val moved = localSections[src.sectionIdx].notes[src.noteIdx]
        localSections = localSections.mapIndexed { idx, section ->
            when {
                idx == src.sectionIdx && idx == destSectionIdx ->
                    section.copy(notes = section.notes.toMutableList().apply { add(destNoteIdx, removeAt(src.noteIdx)) })

                idx == src.sectionIdx -> section.copy(notes = section.notes.filterNot { it.id == noteId })
                idx == destSectionIdx -> section.copy(notes = section.notes.toMutableList().apply { add(destNoteIdx, moved) })
                else -> section
            }
        }
        dragDidMove = true
    }

    val onRowDragStarted: (Long) -> Unit = { noteId ->
        draggingNoteId = noteId
        dragDidMove = false
        dragOriginSectionKey = locateNote(localSections, noteId)?.let { localSections[it.sectionIdx].key }
    }

    // Persists the drop: new orderAt from destination neighbors (sections sort orderAt DESC),
    // plus the destination's full due block when the section changed
    val onRowDragStopped: (Long) -> Unit = persist@{ noteId ->
        val moved = dragDidMove
        val origin = dragOriginSectionKey
        dragDidMove = false
        dragOriginSectionKey = null
        draggingNoteId = null
        if (!moved) return@persist
        val loc = locateNote(localSections, noteId) ?: return@persist
        val section = localSections[loc.sectionIdx]
        val note = section.notes[loc.noteIdx]
        val sectionChanged = origin != null && origin != section.key
        // Repeat tasks derive their section from the schedule, never from due dates:
        // cross-section drops don't apply to them (snap back)
        if (sectionChanged && note.repeatFrequency > 0) {
            localSections = sections
            return@persist
        }
        val range = timelineSectionDropRange(section.key, today) ?: run {
            localSections = sections // defensive: snap back instead of leaving a stale arrangement
            return@persist
        }
        val above = section.notes.getOrNull(loc.noteIdx - 1)
        val below = section.notes.getOrNull(loc.noteIdx + 1)
        val step = 1_000_000L
        val now = Clock.System.now().toEpochMilliseconds()
        // Sections sort priority-first: adopt the neighbors' priority so the drop
        // position always sticks (same rule as task pages)
        val newPriority = when {
            above == null -> below?.priority ?: note.priority
            below == null || above.priority == below.priority -> above.priority
            else -> note.priority
        }
        val newOrderAt = when {
            above == null && below == null -> now
            // Top insert: must exceed the section max even if it was touched this same ms
            above == null -> maxOf(now, below!!.orderAt + step)
            below == null -> above.orderAt - step
            above.priority == below.priority -> below.orderAt + (above.orderAt - below.orderAt) / 2
            // Dropped on the priority boundary: join whichever band the task's priority matches
            newPriority == above.priority -> above.orderAt - step
            else -> below.orderAt + step
        }
        viewModel.moveTimelineTask(
            note = note,
            dueStartAt = if (sectionChanged) range.first else note.dueStartAt,
            dueEndAt = if (sectionChanged) range.second else note.dueEndAt,
            orderAt = newOrderAt,
            priority = newPriority,
        )
    }

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        if (sections.all { it.notes.isEmpty() }) {
            TimelineEmptyState(today = today)
            return@Column
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), state = lazyListState) {
            // Tasks are numbered continuously across all sections (collapsed ones keep their numbers)
            var taskNumber = 0
            localSections.forEachIndexed { sectionIndex, sectionUi ->
                val isFirst = sectionIndex == 0
                val notes = sectionUi.notes
                val isCollapsed = (sectionUi.key in toggledSections) != sectionUi.collapsedByDefault
                val isDroppable = timelineSectionDropRange(sectionUi.key, today) != null
                val sectionStart = taskNumber
                taskNumber += notes.size
                item(key = "header_${sectionUi.key}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Same title inset as note pages
                            .padding(start = 18.dp, end = 14.dp, top = 12.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                toggledSections = if (sectionUi.key in toggledSections) {
                                    toggledSections - sectionUi.key
                                } else {
                                    toggledSections + sectionUi.key
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sectionUi.label,
                            fontSize = if (isFirst) 36.sp else 18.sp,
                            // Only the top section keeps the serif page-title look;
                            // the rest fall back to the theme's sans (Inter)
                            fontFamily = if (isFirst) FontFamily(Font(Res.font.Merriweather_Light)) else null,
                            color = if (isCollapsed) colors.pageTitle.copy(alpha = 0.45f) else colors.pageTitle,
                            modifier = Modifier.alignByBaseline(),
                        )
                        if (sectionUi.subtitle != null) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "· ${sectionUi.subtitle}",
                                fontSize = 16.sp,
                                color = colors.hint,
                                modifier = Modifier.alignByBaseline(),
                            )
                        }
                    }
                }
                // Always-present drop target so empty and collapsed sections can receive rows
                if (isDroppable) {
                    item(key = "slot_${sectionUi.key}") {
                        ReorderableItem(reorderableState, key = "slot_${sectionUi.key}") {
                            Spacer(Modifier.fillMaxWidth().height(6.dp))
                        }
                    }
                }
                if (!isCollapsed) {
                    itemsIndexed(notes, key = { _, note -> note.id }) { index, note ->
                        TimelineDraggableRow(
                            note = note,
                            number = sectionStart + index + 1,
                            sectionUi = sectionUi,
                            isDroppable = isDroppable,
                            reorderableState = reorderableState,
                            onDragStarted = onRowDragStarted,
                            onDragStopped = onRowDragStopped,
                            viewModel = viewModel,
                            onOpenRepeat = { noteForRepeatDialog = it },
                            onOpenReminder = { noteForReminderDialog = it },
                            onOpenDueDate = { noteForDueDateDialog = it },
                            onOpenMove = { noteForMoveDialog = it },
                        )
                    }
                } else {
                    // Keep the dragged row composed inside a collapsed section so the drag
                    // survives pass-through and can drop here (renders as a live preview)
                    val draggedIdx = notes.indexOfFirst { it.id == draggingNoteId }
                    if (draggedIdx >= 0) {
                        val note = notes[draggedIdx]
                        item(key = note.id) {
                            TimelineDraggableRow(
                                note = note,
                                number = sectionStart + draggedIdx + 1,
                                sectionUi = sectionUi,
                                isDroppable = isDroppable,
                                reorderableState = reorderableState,
                                onDragStarted = onRowDragStarted,
                                onDragStopped = onRowDragStopped,
                                viewModel = viewModel,
                                onOpenRepeat = { noteForRepeatDialog = it },
                                onOpenReminder = { noteForReminderDialog = it },
                                onOpenDueDate = { noteForDueDateDialog = it },
                                onOpenMove = { noteForMoveDialog = it },
                            )
                        }
                    }
                }
            }
        }
    }

    if (noteForDueDateDialog != null) {
        val note = noteForDueDateDialog!!
        DueDateOptionsDialog(
            currentDueStartAt = note.dueStartAt,
            currentDueEndAt = note.dueEndAt,
            onDismiss = { noteForDueDateDialog = null },
            onApply = { dueStartAt, dueEndAt ->
                viewModel.setNoteDueDate(note, dueStartAt, dueEndAt)
                noteForDueDateDialog = null
            },
            showClear = false,
        )
    }

    if (noteForRepeatDialog != null) {
        val note = noteForRepeatDialog!!
        RepeatFrequencyDialog(
            currentFrequency = RepeatFrequency.fromOrdinal(note.repeatFrequency),
            currentStartDate = note.repeatTaskStartFrom,
            currentReminderTime = note.reminderAt,
            isReminderEnabled = note.reminderEnabled == 1 && note.repeatFrequency > 0,
            onDismiss = { noteForRepeatDialog = null },
            onConfirm = { frequency, startDate, reminderTime, reminderEnabled ->
                viewModel.setNoteRepeatFrequency(note, frequency, startDate, reminderTime, reminderEnabled)
                noteForRepeatDialog = null
            }
        )
    }

    if (noteForReminderDialog != null) {
        val note = noteForReminderDialog!!
        val permissionHandler = rememberReminderPermissionHandler()
        var showDatePicker by remember { mutableStateOf(false) }

        // Check if we already have all permissions
        LaunchedEffect(note) {
            if (permissionHandler.hasAllReminderPermissions()) {
                showDatePicker = true
            }
        }

        // Show permission flow if we don't have all permissions
        if (!showDatePicker && !permissionHandler.hasAllReminderPermissions()) {
            ReminderPermissionFlow(
                permissionHandler = permissionHandler,
                onPermissionsGranted = {
                    showDatePicker = true
                },
                onDismiss = {
                    noteForReminderDialog = null
                }
            )
        }

        // Show date picker once permissions are handled
        if (showDatePicker) {
            val now = Clock.System.now()

            // Calculate initial date/time from existing reminder or default to tomorrow 9 AM
            val initialDateTime = remember(note.reminderAt) {
                if (note.reminderAt > 0) {
                    Instant.fromEpochMilliseconds(note.reminderAt)
                        .toLocalDateTime(timeZone)
                } else {
                    val tomorrow = now.toLocalDateTime(timeZone).date
                        .plus(1, DateTimeUnit.DAY)
                    LocalDateTime(tomorrow, LocalTime(9, 0))
                }
            }

            val isRepeatingTask = note.repeatFrequency > 0
            val repeatFrequency = RepeatFrequency.fromOrdinal(note.repeatFrequency)

            DateTimePickerDialog(
                initialDate = initialDateTime.date,
                initialHour = initialDateTime.hour,
                initialMinute = initialDateTime.minute,
                hasExistingReminder = note.reminderAt > 0,
                isRepeatingTask = isRepeatingTask,
                repeatFrequency = repeatFrequency,
                onDismiss = { noteForReminderDialog = null },
                onConfirm = { date, hour, minute ->
                    val reminderTime = LocalDateTime(
                        date,
                        LocalTime(hour, minute)
                    ).toInstant(timeZone).toEpochMilliseconds()
                    viewModel.setNoteReminder(note, reminderTime, true)
                    noteForReminderDialog = null
                },
                onClear = {
                    viewModel.removeNoteReminder(note)
                },
                onOpenRepeatDialog = {
                    noteForRepeatDialog = note
                }
            )
        }
    }

    if (noteForMoveDialog != null) {
        val note = noteForMoveDialog!!
        MoveToDialog(
            currentPageId = note.pageId,
            pages = pages,
            subPagesByParent = subPagesByParent,
            onDismiss = { noteForMoveDialog = null },
            onConfirm = { targetPageId ->
                viewModel.moveNoteToPage(note, targetPageId)
                noteForMoveDialog = null
            }
        )
    }
}

private data class TimelineSectionUi(
    val key: String,
    val label: String,
    val notes: List<Note>,
    val collapsedByDefault: Boolean,
    val isDimmed: Boolean = false,
    val subtitle: String? = null,
)

/** Replaces the section scaffold while nothing is scheduled anywhere. */
@Composable
private fun TimelineEmptyState(today: LocalDate) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Same title inset and rhythm as the top section header
            .padding(start = 18.dp, end = 14.dp, top = 12.dp)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = TimelineSection.TODAY.label,
            fontSize = 36.sp,
            fontFamily = FontFamily(Font(Res.font.Merriweather_Light)),
            color = colors.pageTitle,
            modifier = Modifier.alignByBaseline(),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "· ${dayDateLabel(today)}",
            fontSize = 16.sp,
            color = colors.hint,
            modifier = Modifier.alignByBaseline(),
        )
    }
    Text(
        text = "Nothing scheduled yet.\n\nAdd a task below, or open any task's menu and set a due date — it will show up here.",
        color = colors.hint,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
    )
    // Preview the structure ahead with the next two section headers
    listOf(TimelineSection.TOMORROW, TimelineSection.THIS_WEEK).forEach { section ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 14.dp, top = 12.dp)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.label,
                fontSize = 18.sp,
                color = colors.pageTitle,
                modifier = Modifier.alignByBaseline(),
            )
            sectionSubtitle(section, today)?.let { subtitle ->
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "· $subtitle",
                    fontSize = 16.sp,
                    color = colors.hint,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

/** Light-grey date context shown next to a section title, e.g. "Tue, 4 Aug" or "3–9 Aug". */
private fun sectionSubtitle(section: TimelineSection, today: LocalDate): String? {
    val weekStart = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    return when (section) {
        TimelineSection.TODAY -> dayDateLabel(today)
        TimelineSection.TOMORROW -> dayDateLabel(today.plus(1, DateTimeUnit.DAY))
        TimelineSection.THIS_WEEK ->
            rangeLabel(weekStart, weekStart.plus(6, DateTimeUnit.DAY))

        TimelineSection.THIS_WEEKEND ->
            rangeLabel(weekStart.plus(5, DateTimeUnit.DAY), weekStart.plus(6, DateTimeUnit.DAY))

        TimelineSection.NEXT_WEEK ->
            rangeLabel(weekStart.plus(7, DateTimeUnit.DAY), weekStart.plus(13, DateTimeUnit.DAY))

        TimelineSection.THIS_MONTH -> monthFullLabel(today.month)
        TimelineSection.NEXT_MONTH ->
            monthFullLabel(LocalDate(today.year, today.month, 1).plus(1, DateTimeUnit.MONTH).month)

        TimelineSection.OVERDUE, TimelineSection.SOMEDAY -> null
    }
}

private fun dayDateLabel(date: LocalDate): String =
    "${dayAbbrev(date)}, ${date.dayOfMonth} ${monthAbbrev(date.month)}"

private fun rangeLabel(start: LocalDate, end: LocalDate): String =
    if (start.month == end.month) "${start.dayOfMonth}–${end.dayOfMonth} ${monthAbbrev(start.month)}"
    else "${start.dayOfMonth} ${monthAbbrev(start.month)} – ${end.dayOfMonth} ${monthAbbrev(end.month)}"

private fun dayAbbrev(date: LocalDate): String =
    date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }

private fun monthAbbrev(month: Month): String =
    month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }

private fun monthFullLabel(month: Month): String =
    month.name.lowercase().replaceFirstChar { it.uppercase() }

private data class NoteLocation(val sectionIdx: Int, val noteIdx: Int)

private fun locateNote(sections: List<TimelineSectionUi>, noteId: Long): NoteLocation? {
    sections.forEachIndexed { sectionIdx, section ->
        val noteIdx = section.notes.indexOfFirst { it.id == noteId }
        if (noteIdx >= 0) return NoteLocation(sectionIdx, noteIdx)
    }
    return null
}

@Composable
private fun LazyItemScope.TimelineDraggableRow(
    note: Note,
    number: Int,
    sectionUi: TimelineSectionUi,
    isDroppable: Boolean,
    reorderableState: ReorderableLazyListState,
    onDragStarted: (Long) -> Unit,
    onDragStopped: (Long) -> Unit,
    viewModel: MainViewModel,
    onOpenRepeat: (Note) -> Unit,
    onOpenReminder: (Note) -> Unit,
    onOpenDueDate: (Note) -> Unit,
    onOpenMove: (Note) -> Unit,
) {
    // enabled=false keeps rows in non-droppable sections (Overdue, Unscheduled) from
    // receiving other rows, while their own handle still lets them be dragged out
    ReorderableItem(reorderableState, key = note.id, enabled = isDroppable) { isDragging ->
        TimelineNoteRow(
            note = note,
            index = number,
            isDimmed = sectionUi.isDimmed,
            isDragging = isDragging,
            // Completed rows are inert; everything else long-press drags
            dragHandleModifier = if (sectionUi.isDimmed) Modifier else Modifier.longPressDraggableHandle(
                onDragStarted = { onDragStarted(note.id) },
                onDragStopped = { onDragStopped(note.id) },
            ),
            onToggleDone = {
                // A done repeating task on the timeline is a preview of its next
                // occurrence: toggling completes that cycle early instead of un-completing
                if (note.done && note.repeatFrequency > 0) viewModel.completeRepeatingTaskEarly(note)
                else viewModel.toggleNoteDone(note)
            },
            onDelete = { viewModel.deleteNote(note) },
            onSetPriority = {
                viewModel.updateNote(
                    note.copy(
                        priority = if (note.priority == 0) 1 else 0,
                        done = false,
                        orderAt = Clock.System.now().toEpochMilliseconds()
                    )
                )
            },
            onEdit = { viewModel.setEditingNote(note) },
            onSetRepeat = { onOpenRepeat(note) },
            onSetReminder = { onOpenReminder(note) },
            onSetDueDate = { onOpenDueDate(note) },
            onMoveTo = { onOpenMove(note) },
        )
    }
}

@Composable
private fun TimelineNoteRow(
    note: Note,
    index: Int,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    onSetPriority: () -> Unit,
    onEdit: () -> Unit,
    onSetRepeat: () -> Unit,
    onSetReminder: () -> Unit,
    onSetDueDate: () -> Unit,
    onMoveTo: () -> Unit,
    modifier: Modifier = Modifier,
    isDimmed: Boolean = false,
    isDragging: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    var showMenu by remember { mutableStateOf(false) }
    // A truncated row takes two taps: the first reveals the rest of the text, the
    // second opens the menu. Untruncated rows open the menu on the first tap.
    var isExpanded by remember(note.text, isDimmed) { mutableStateOf(false) }
    var isTruncated by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            // 6.dp per row: sections already add vertical rhythm, so rows sit 4.dp
            // tighter than on note pages
            .padding(vertical = 6.dp)
            .pointerInput(note) {
                detectTapGestures(
                    onTap = {
                        focusManager.clearFocus()
                        if (isTruncated && !isExpanded) isExpanded = true
                        else showMenu = true
                    },
                    onDoubleTap = { onToggleDone() },
                )
            }
            .then(dragHandleModifier),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
            Text(
                modifier = Modifier.padding(start = 12.dp, top = 6.dp).defaultMinSize(minWidth = 28.dp),
                fontFamily = FontFamily(Font(Res.font.Merriweather_Light)),
                text = "$index.",
                color = colors.primaryText.copy(alpha = 0.33f),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                lineHeight = 20.sp
            )
            Text(
                modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 5.dp, bottom = 5.dp).weight(1f),
                text = note.text,
                color = when {
                    isDragging -> colors.hint
                    isDimmed -> colors.primaryText.copy(alpha = 0.33f)
                    note.priority == 1 -> colors.priorityText
                    else -> colors.primaryText
                },
                textDecoration = if (isDimmed) TextDecoration.LineThrough else TextDecoration.None,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.5.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else if (isDimmed) 1 else 3,
                overflow = TextOverflow.Ellipsis,
                // Skipped while expanded, where overflow is always false
                onTextLayout = { if (!isExpanded) isTruncated = it.hasVisualOverflow },
            )
            if (note.repeatFrequency > 0) {
                Icon(
                    imageVector = Icons.Filled.Repeat,
                    contentDescription = "Repeating task",
                    modifier = Modifier.padding(top = 7.dp, end = 8.dp).size(16.dp),
                    tint = colors.primaryText.copy(alpha = if (isDimmed) 0.33f else 0.4f)
                )
            }
        }

        NoteActionsMenu(
            note = note,
            expanded = showMenu,
            // Every way out of the menu routes through here, so the row also
            // collapses after picking an action
            onDismissRequest = { showMenu = false; isExpanded = false },
            onDelete = onDelete,
            onCopy = { clipboardManager.setText(AnnotatedString(note.text)) },
            onToggleDone = onToggleDone,
            onSetPriority = onSetPriority,
            onEdit = onEdit,
            onSetRepeat = onSetRepeat,
            onSetReminder = onSetReminder,
            onSetDueDate = onSetDueDate,
            onMoveTo = onMoveTo,
        )
    }
}
