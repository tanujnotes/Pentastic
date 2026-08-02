@file:OptIn(ExperimentalTime::class)

package app.pentastic.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pentastic.data.Note
import app.pentastic.data.RepeatFrequency
import app.pentastic.data.TimelineBucket
import app.pentastic.data.TimelineSection
import app.pentastic.data.classifyDueDate
import app.pentastic.data.hasDueDate
import app.pentastic.ui.theme.AppTheme
import app.pentastic.ui.viewmodel.MainViewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.Font
import org.koin.compose.viewmodel.koinViewModel
import pentastic.composeapp.generated.resources.Merriweather_Light
import pentastic.composeapp.generated.resources.Res
import kotlin.time.Clock
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
    val today = Clock.System.now().toLocalDateTime(timeZone).date

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
            // Live (non-deleted, non-archived) pages = root pages + their sub-pages + the Timeline page itself
            val livePageIds = buildSet {
                pages.forEach { add(it.id) }
                subPagesByParent.values.forEach { subs -> subs.forEach { add(it.id) } }
                timelinePage?.let { add(it.id) }
            }
            val grouped = notesByPage
                .filterKeys { it in livePageIds }
                .values.flatten()
                .filter { it.hasDueDate && !it.done }
                .groupBy { classifyDueDate(it.dueStartAt, it.dueEndAt, today, timeZone) }

            fun datedSort(notes: List<Note>) =
                notes.sortedWith(compareBy({ it.dueEndAt }, { it.dueStartAt }))

            buildList {
                TimelineSection.entries.filter { it != TimelineSection.SOMEDAY }.forEach { section ->
                    val sectionNotes = datedSort(grouped[TimelineBucket.Section(section)] ?: emptyList())
                    // Overdue only appears when something is actually overdue
                    if (section == TimelineSection.OVERDUE && sectionNotes.isEmpty()) return@forEach
                    add(
                        TimelineSectionUi(
                            key = section.name,
                            label = section.label,
                            notes = sectionNotes,
                            collapsedByDefault = false,
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
                            notes = datedSort(grouped[TimelineBucket.Year(year)] ?: emptyList()),
                            collapsedByDefault = true,
                        )
                    )
                }
                val someday = grouped[TimelineBucket.Section(TimelineSection.SOMEDAY)] ?: emptyList()
                add(
                    TimelineSectionUi(
                        key = TimelineSection.SOMEDAY.name,
                        label = TimelineSection.SOMEDAY.label,
                        notes = someday.sortedByDescending { it.orderAt },
                        collapsedByDefault = false,
                    )
                )

                // Safety nets for tasks that live ON the Timeline page itself (shown only when non-empty)
                val timelineOwnNotes = timelinePage?.id?.let { notesByPage[it] } ?: emptyList()
                val unscheduled = timelineOwnNotes
                    .filter { !it.done && it.dueStartAt == 0L }
                    .sortedByDescending { it.orderAt }
                if (unscheduled.isNotEmpty()) {
                    add(
                        TimelineSectionUi(
                            key = "UNSCHEDULED",
                            label = "Unscheduled",
                            notes = unscheduled,
                            collapsedByDefault = false,
                        )
                    )
                }
                val completed = timelineOwnNotes
                    .filter { it.done }
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

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Tasks are numbered continuously across all sections (collapsed ones keep their numbers)
            var taskNumber = 0
            sections.forEachIndexed { sectionIndex, sectionUi ->
                val isFirst = sectionIndex == 0
                val notes = sectionUi.notes
                val isCollapsed = (sectionUi.key in toggledSections) != sectionUi.collapsedByDefault
                val sectionStart = taskNumber
                taskNumber += notes.size
                item(key = "header_${sectionUi.key}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, top = 12.dp)
                            .clickable {
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
                            fontSize = if (isFirst) 36.sp else 22.sp,
                            fontFamily = FontFamily(Font(Res.font.Merriweather_Light)),
                            color = if (isCollapsed) colors.pageTitle.copy(alpha = 0.45f) else colors.pageTitle,
                        )
                        Spacer(Modifier.width(12.dp))
                        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.divider)
                    }
                }
                if (!isCollapsed) {
                    itemsIndexed(notes, key = { _, note -> note.id }) { index, note ->
                        TimelineNoteRow(
                            note = note,
                            index = sectionStart + index + 1,
                            isDimmed = sectionUi.isDimmed,
                            onToggleDone = { viewModel.toggleNoteDone(note) },
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
                            onSetRepeat = { noteForRepeatDialog = note },
                            onSetReminder = { noteForReminderDialog = note },
                            onSetDueDate = { noteForDueDateDialog = note },
                            onMoveTo = { noteForMoveDialog = note },
                        )
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
            }
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
)

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
    isDimmed: Boolean = false,
) {
    val colors = AppTheme.colors
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(note) {
                    detectTapGestures(
                        onTap = { showMenu = true },
                        onDoubleTap = { onToggleDone() },
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(start = 12.dp).defaultMinSize(minWidth = 28.dp),
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
                    isDimmed -> colors.primaryText.copy(alpha = 0.33f)
                    note.priority == 1 -> colors.priorityText
                    else -> colors.primaryText
                },
                textDecoration = if (isDimmed) TextDecoration.LineThrough else TextDecoration.None,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        NoteActionsMenu(
            note = note,
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
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
