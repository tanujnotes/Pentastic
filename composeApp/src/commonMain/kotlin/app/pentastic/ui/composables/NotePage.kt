@file:OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)

package app.pentastic.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.pentastic.data.AppCalendar
import app.pentastic.data.Note
import app.pentastic.data.Page
import app.pentastic.data.PageType
import app.pentastic.data.RepeatFrequency
import app.pentastic.data.formatDueDateLabel
import app.pentastic.data.hasDueDate
import app.pentastic.data.isDueOverdue
import app.pentastic.ui.theme.AppTheme
import app.pentastic.ui.theme.titleFontFamily
import app.pentastic.ui.theme.appRadioButtonColors
import app.pentastic.ui.theme.appSwitchColors
import app.pentastic.utils.JalaliCalendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun NotePage(
    notes: List<Note>,
    notesByPage: Map<Long, List<Note>>,
    onUpdateNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    toggleNoteDone: (Note) -> Unit,
    page: Page,
    subPages: List<Page>,
    selectedSubPageId: Long?,
    onSelectedSubPageChange: (Long?) -> Unit,
    setEditingNote: (Note?) -> Unit,
    onSetRepeatFrequency: (Note, RepeatFrequency, Long, Long?, Boolean) -> Unit,
    onSetReminder: (Note, Long, Boolean) -> Unit,
    onRemoveReminder: (Note) -> Unit,
    onSetDueDate: (Note, Long, Long) -> Unit,
    allPages: List<Page> = emptyList(),
    allSubPagesByParent: Map<Long, List<Page>> = emptyMap(),
    onMoveNote: (Note, Long) -> Unit = { _, _ -> },
    pageType: PageType = PageType.TASKS,
    showCompletedTasks: Boolean = false,
    onToggleShowCompleted: () -> Unit = {},
    onDeleteCompletedTasks: (List<Note>) -> Unit = {},
) {
    val strings = AppTheme.strings
    val isNotesType = pageType == PageType.NOTES
    val noteMovedToIndex = remember { mutableStateOf(-1) }
    var noteForRepeatDialog by remember { mutableStateOf<Note?>(null) }
    var noteForReminderDialog by remember { mutableStateOf<Note?>(null) }
    var noteForMoveDialog by remember { mutableStateOf<Note?>(null) }
    var noteForDueDateDialog by remember { mutableStateOf<Note?>(null) }
    var showCompletedMenu by remember { mutableStateOf(false) }
    var showDeleteCompletedDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val allDisplayedNotes = remember(notes, selectedSubPageId, notesByPage) {
        if (selectedSubPageId == null)
            notes
        else
            notesByPage[selectedSubPageId] ?: emptyList()
    }

    val completedTasks = remember(allDisplayedNotes, isNotesType) {
        if (!isNotesType) allDisplayedNotes.filter { it.done } else emptyList()
    }

    val displayedNotes = remember(allDisplayedNotes, isNotesType) {
        if (!isNotesType) allDisplayedNotes.filter { !it.done } else allDisplayedNotes
    }

    var list by remember { mutableStateOf(displayedNotes) }
    val lazyListState = rememberLazyListState()
    val reorderableLazyColumnState = rememberReorderableLazyListState(lazyListState) { from, to ->
        list = list.toMutableList().apply {
            add(to.index, removeAt(from.index))
            noteMovedToIndex.value = to.index
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                focusManager.clearFocus(true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Ids from the previous emission. Only the effect below reads it, so writing it
    // there costs no recomposition.
    val previousIds = remember { mutableStateOf(emptyList<Long>()) }

    LaunchedEffect(displayedNotes) {
        list = displayedNotes
        val ids = displayedNotes.map { it.id }
        val previous = previousIds.value
        previousIds.value = ids

        // Jump to the top only when a task actually turns up there: a new one, one
        // coming back from the completed section, or one bumped up the order. Edits,
        // reminders, due dates, deletions and drag-reorders leave the scroll alone.
        val previousSet = previous.toSet()
        val arrived = ids.any { it !in previousSet }
        val bumpedUp = ids.size == previous.size &&
                ids.firstOrNull()?.let { previous.indexOf(it) > 0 } == true
        if (ids.isNotEmpty() && (arrived || bumpedUp)) {
            lazyListState.animateScrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 16.dp, bottom = if (subPages.isEmpty()) 16.dp else 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = page.name,
                style = TextStyle(
                    color = AppTheme.colors.pageTitle,
                    fontSize = 36.sp,
                    fontFamily = titleFontFamily()
                )
            )
        }

        if (subPages.isNotEmpty()) {
            SubPageTabs(
                subPages = subPages,
                selectedSubPageId = selectedSubPageId,
                onSubPageClick = onSelectedSubPageChange
            )
            Spacer(Modifier.height(14.dp))
        }

        // Pre-compute date strings for notes-type grouping (outside LazyColumn scope)
        val dateTexts = AppTheme.dateTexts
        val noteDateStrings = remember(list, isNotesType, dateTexts) {
            if (isNotesType) {
                val timeZone = TimeZone.currentSystemDefault()
                list.map { note ->
                    val dt = Instant.fromEpochMilliseconds(note.createdAt)
                        .toLocalDateTime(timeZone)
                    dateTexts.dayMonthYear(dt.date.toEpochDays().toInt())
                }
            } else emptyList()
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                reverseLayout = isNotesType,
            ) {
                if (isNotesType) {
                    // Notes type: chronological, no drag-to-reorder, date headers, time to the right
                    val timeZone = TimeZone.currentSystemDefault()

                    itemsIndexed(list, key = { _, it -> it.id }) { index, note ->
                        var showMenu by remember { mutableStateOf(false) }
                        val colors = AppTheme.colors
                        val styledText = remember(note.text, note.done, note.priority, colors) {
                            mutableStateOf(
                                buildAnnotatedString {
                                    val style = if (note.done) {
                                        SpanStyle(textDecoration = TextDecoration.LineThrough)
                                    } else if (note.priority == 1) {
                                        SpanStyle(color = colors.priorityText)
                                    } else {
                                        SpanStyle()
                                    }
                                    withStyle(style) {
                                        append(note.text)
                                    }
                                }
                            )
                        }

                        val timeLabel = remember(note.createdAt, dateTexts) {
                            val dateTime = Instant.fromEpochMilliseconds(note.createdAt)
                                .toLocalDateTime(timeZone)
                            dateTexts.time12(dateTime.hour, dateTime.minute)
                        }


                        // With reverseLayout + descending sort: index 0 = newest at bottom.
                        // Date header shows above the last note of each day group.
                        // In reversed layout, the visually "above" neighbor is index + 1.
                        val showDateHeader = index == list.lastIndex ||
                                noteDateStrings[index] != noteDateStrings[index + 1]

                        Column(modifier = Modifier.animateItem()) {
                            if (showDateHeader) {
                                Text(
                                    text = noteDateStrings[index],
                                    color = colors.hint,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = if (index == list.lastIndex) 4.dp else 16.dp, bottom = 8.dp),
                                    textAlign = TextAlign.Center,
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(note) {
                                        detectTapGestures(
                                            onTap = {
                                                focusManager.clearFocus()
                                                showMenu = true
                                            },
                                            onDoubleTap = {
                                                handleToggleDone(
                                                    note,
                                                    toggleNoteDone,
                                                    scope,
                                                    styledText,
                                                    if (note.priority == 1) colors.priorityText else colors.primaryText
                                                )
                                            },
                                        )
                                    },
                            ) {
                                NoteTextWithTime(
                                    text = styledText.value,
                                    textColor = colors.primaryText.copy(alpha = if (note.done) 0.33f else 1f),
                                    timeLabel = timeLabel,
                                    timeColor = colors.hint,
                                    maxLines = if (showMenu) Int.MAX_VALUE else 10,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                                )
                                val clipboardManager = LocalClipboardManager.current
                                NoteActionsMenu(
                                    note = note,
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    onDelete = { onDeleteNote(note) },
                                    onCopy = { clipboardManager.setText(AnnotatedString(note.text)) },
                                    onToggleDone = {
                                        handleToggleDone(
                                            note,
                                            toggleNoteDone,
                                            scope,
                                            styledText,
                                            if (note.priority == 1) colors.priorityText else colors.primaryText
                                        )
                                    },
                                    onSetPriority = {
                                        onUpdateNote(
                                            note.copy(
                                                priority = if (note.priority == 0) 1 else 0,
                                                done = false,
                                            )
                                        )
                                    },
                                    onEdit = { setEditingNote(note) },
                                    onSetRepeat = { noteForRepeatDialog = note },
                                    onSetReminder = { noteForReminderDialog = note },
                                    onSetDueDate = { noteForDueDateDialog = note },
                                    onMoveTo = { noteForMoveDialog = note }
                                )
                            }
                        }
                    }
                } else {
                    // Tasks type: numbered list with drag-to-reorder (existing behavior)
                    itemsIndexed(list, key = { _, it -> it.id }) { index, note ->
                        ReorderableItem(reorderableLazyColumnState, note.id) { isDragging ->
                            val interactionSource = remember { MutableInteractionSource() }
                            var showMenu by remember { mutableStateOf(false) }
                            // A truncated task takes two taps: the first reveals the rest of
                            // the text, the second opens the menu. Untruncated rows open the
                            // menu on the first tap as before.
                            var isExpanded by remember(note.text, note.done) { mutableStateOf(false) }
                            var isTruncated by remember { mutableStateOf(false) }
                            val colors = AppTheme.colors
                            val styledText = remember(note.text, note.done, note.priority, isDragging, colors) {
                                mutableStateOf(
                                    buildAnnotatedString {
                                        val style = if (isDragging) {
                                            SpanStyle(color = colors.hint)
                                        } else if (note.done) {
                                            SpanStyle(textDecoration = TextDecoration.LineThrough)
                                        } else if (note.priority == 1) {
                                            SpanStyle(color = colors.priorityText)
                                        } else {
                                            SpanStyle() // Default style
                                        }
                                        withStyle(style) {
                                            append(note.text)
                                        }
                                    }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .pointerInput(note) {
                                        detectTapGestures(
                                            onTap = {
                                                focusManager.clearFocus()
                                                if (isTruncated && !isExpanded) isExpanded = true
                                                else showMenu = true
                                            },
                                            onDoubleTap = {
                                                handleToggleDone(
                                                    note,
                                                    toggleNoteDone,
                                                    scope,
                                                    styledText,
                                                    if (note.priority == 1) colors.priorityText else colors.primaryText
                                                )
                                            },
                                        )
                                    }
                                    .longPressDraggableHandle(
                                        onDragStarted = {
                                        },
                                        onDragStopped = {
                                            val movedIndex = noteMovedToIndex.value
                                            if (movedIndex == -1) return@longPressDraggableHandle
                                            val step = 1_000_000L
                                            val above = list.getOrNull(movedIndex - 1)
                                            val below = list.getOrNull(movedIndex + 1)
                                            val newPriority = when {
                                                above == null -> below?.priority ?: list[movedIndex].priority
                                                below == null || above.priority == below.priority -> above.priority
                                                else -> list[movedIndex].priority
                                            }

                                            val newOrderAt = when {
                                                above == null -> Clock.System.now().toEpochMilliseconds()
                                                below == null -> above.orderAt - step
                                                above.priority == below.priority -> below.orderAt + (above.orderAt - below.orderAt) / 2
                                                newPriority == above.priority -> above.orderAt - step
                                                else -> below.orderAt + step
                                            }
                                            onUpdateNote(list[movedIndex].copy(orderAt = newOrderAt, priority = newPriority))
                                            noteMovedToIndex.value = -1
                                        },
                                        interactionSource = interactionSource,
                                    )
                                    // Toggling done moves a row between this list and the
                                    // completed section, which are keyed differently, so Compose
                                    // sees a delete rather than a move. A fade-out would keep
                                    // drawing the old row at its old offset while the rest of the
                                    // list reflows around it, stranding a copy of the text.
                                    .animateItem(fadeOutSpec = null),
                            ) {
                                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
                                    Text(
                                        modifier = Modifier.padding(start = 12.dp, top = 6.dp).defaultMinSize(minWidth = 28.dp),
                                        fontFamily = titleFontFamily(),
                                        text = (index + 1).toString() + ".",
                                        // color = if (note.priority == 1 && note.done.not()) colors.priorityText.copy(alpha = 0.7f) else colors.primaryText.copy(alpha = 0.33f),
                                        color = colors.primaryText.copy(alpha = 0.33f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 16.sp,
                                        lineHeight = 20.sp
                                    )
                                    Text(
                                        modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 5.dp, bottom = 5.dp).weight(1f),
                                        text = styledText.value,
                                        color = colors.primaryText.copy(alpha = if (note.done) 0.33f else 1f),
                                        fontSize = 18.sp,
                                        lineHeight = 20.sp,
                                        letterSpacing = 0.5.sp,
                                        maxLines = if (isExpanded) Int.MAX_VALUE else if (note.done) 1 else 3,
                                        overflow = TextOverflow.Ellipsis,
                                        // Skipped while expanded, where overflow is always false
                                        onTextLayout = { if (!isExpanded) isTruncated = it.hasVisualOverflow },
                                    )
                                    // Icons for reminder, repeat and due date
                                    val isRepeating = note.repeatFrequency > 0
                                    val nowMillis = Clock.System.now().toEpochMilliseconds()
                                    val hasUpcomingReminder = note.reminderAt > nowMillis && note.reminderEnabled == 1
                                    // Repeat and due date are mutually exclusive; hide any stale due date
                                    val hasDueDate = note.hasDueDate && !isRepeating
                                    if (isRepeating || hasUpcomingReminder || hasDueDate) {
                                        Row(
                                            modifier = Modifier.padding(top = 7.dp, end = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            if (isRepeating) {
                                                Icon(
                                                    imageVector = Icons.Filled.Repeat,
                                                    contentDescription = strings.repeatingTask,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = colors.primaryText.copy(alpha = if (note.done) 0.33f else 0.4f)
                                                )
                                            }
                                            if (hasUpcomingReminder) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Notifications,
                                                    contentDescription = strings.upcomingReminder,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = colors.primaryText.copy(alpha = if (note.done) 0.33f else 0.4f)
                                                )
                                            }
                                            if (hasDueDate) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CalendarMonth,
                                                    contentDescription = strings.dueDate,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = when {
                                                        note.done -> colors.primaryText.copy(alpha = 0.33f)
                                                        note.isDueOverdue(nowMillis) -> colors.priorityText.copy(alpha = 0.7f)
                                                        else -> colors.primaryText.copy(alpha = 0.4f)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                val clipboardManager = LocalClipboardManager.current
                                NoteActionsMenu(
                                    note = note,
                                    expanded = showMenu,
                                    // Every way out of the menu routes through here, so the
                                    // row also collapses after picking an action
                                    onDismissRequest = { showMenu = false; isExpanded = false },
                                    onDelete = { onDeleteNote(note) },
                                    onCopy = { clipboardManager.setText(AnnotatedString(note.text)) },
                                    onToggleDone = {
                                        handleToggleDone(
                                            note,
                                            toggleNoteDone,
                                            scope,
                                            styledText,
                                            if (note.priority == 1) colors.priorityText else colors.primaryText
                                        )
                                    },
                                    onSetPriority = {
                                        onUpdateNote(
                                            note.copy(
                                                priority = if (note.priority == 0) 1 else 0,
                                                done = false,
                                                orderAt = Clock.System.now().toEpochMilliseconds()
                                            )
                                        )
                                    },
                                    onEdit = { setEditingNote(note) },
                                    onSetRepeat = { noteForRepeatDialog = note },
                                    onSetReminder = { noteForReminderDialog = note },
                                    onSetDueDate = { noteForDueDateDialog = note },
                                    onMoveTo = { noteForMoveDialog = note }
                                )
                            }
                        }
                    }

                    if (completedTasks.isNotEmpty()) {
                        item(key = "completed_toggle") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // Without this the header snaps to its new position while the
                                    // completed rows below animate into theirs, and the two overlap
                                    .animateItem()
                                    // 10.dp puts the overflow icon's centre 28.dp from the right
                                    // edge, in line with the add button in the input bar below
                                    .padding(start = 14.dp, end = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        // No ripple: the chevron already answers the tap
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { onToggleShowCompleted() }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (showCompletedTasks) Icons.Default.KeyboardArrowDown
                                        else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = AppTheme.colors.primaryText.copy(alpha = 0.33f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(18.dp))
                                    Text(
                                        text = strings.completedTasks(completedTasks.size),
                                        fontSize = 18.sp,
                                        color = AppTheme.colors.primaryText.copy(alpha = 0.33f),
                                    )
                                }

                                Box {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = strings.moreOptions,
                                        tint = AppTheme.colors.primaryText.copy(alpha = 0.33f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(percent = 50))
                                            .clickable { showCompletedMenu = true }
                                            .padding(8.dp)
                                            .size(20.dp)
                                    )
                                    DropdownMenu(
                                        modifier = Modifier.background(color = AppTheme.colors.menuBackground),
                                        expanded = showCompletedMenu,
                                        onDismissRequest = { showCompletedMenu = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = strings.deleteCompleted,
                                                    color = AppTheme.colors.primaryText,
                                                    fontSize = 16.sp,
                                                )
                                            },
                                            onClick = {
                                                showCompletedMenu = false
                                                showDeleteCompletedDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (showCompletedTasks) {
                            itemsIndexed(completedTasks, key = { _, it -> "done_${it.id}" }) { index, note ->
                                var showMenu by remember { mutableStateOf(false) }
                                // First tap expands a truncated row, second one opens the menu
                                var isExpanded by remember(note.text) { mutableStateOf(false) }
                                var isTruncated by remember { mutableStateOf(false) }
                                val colors = AppTheme.colors
                                val styledText = remember(note.text, note.done, note.priority, colors) {
                                    mutableStateOf(
                                        buildAnnotatedString {
                                            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                                append(note.text)
                                            }
                                        }
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .pointerInput(note) {
                                            detectTapGestures(
                                                onTap = {
                                                    focusManager.clearFocus()
                                                    if (isTruncated && !isExpanded) isExpanded = true
                                                    else showMenu = true
                                                },
                                                onDoubleTap = {
                                                    handleToggleDone(
                                                        note,
                                                        toggleNoteDone,
                                                        scope,
                                                        styledText,
                                                        if (note.priority == 1) colors.priorityText else colors.primaryText
                                                    )
                                                },
                                            )
                                        }
                                        // No fade-out: see the active list above
                                        .animateItem(fadeOutSpec = null),
                                ) {
                                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
                                        Text(
                                            modifier = Modifier.padding(start = 12.dp, top = 6.dp).defaultMinSize(minWidth = 28.dp),
                                            fontFamily = titleFontFamily(FontWeight.Normal),
                                            text = (list.size + index + 1).toString() + ".",
                                            color = colors.primaryText.copy(alpha = 0.33f),
                                            textAlign = TextAlign.Center,
                                            fontSize = 16.sp,
                                            lineHeight = 20.sp
                                        )
                                        Text(
                                            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 5.dp, bottom = 5.dp).weight(1f),
                                            text = styledText.value,
                                            color = colors.primaryText.copy(alpha = 0.33f),
                                            fontSize = 18.sp,
                                            lineHeight = 20.sp,
                                            letterSpacing = 0.5.sp,
                                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                                            overflow = TextOverflow.Ellipsis,
                                            onTextLayout = { if (!isExpanded) isTruncated = it.hasVisualOverflow },
                                        )
                                        val isRepeating = note.repeatFrequency > 0
                                        val nowMillis = Clock.System.now().toEpochMilliseconds()
                                        val hasUpcomingReminder = note.reminderAt > nowMillis && note.reminderEnabled == 1
                                        val hasDueDate = note.hasDueDate
                                        if (isRepeating || hasUpcomingReminder || hasDueDate) {
                                            Row(
                                                modifier = Modifier.padding(top = 7.dp, end = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                if (isRepeating) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Repeat,
                                                        contentDescription = strings.repeatingTask,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = colors.primaryText.copy(alpha = 0.33f)
                                                    )
                                                }
                                                if (hasUpcomingReminder) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Notifications,
                                                        contentDescription = strings.upcomingReminder,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = colors.primaryText.copy(alpha = 0.33f)
                                                    )
                                                }
                                                if (hasDueDate) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.CalendarMonth,
                                                        contentDescription = strings.dueDate,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = colors.primaryText.copy(alpha = 0.33f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    val clipboardManager = LocalClipboardManager.current
                                    NoteActionsMenu(
                                        note = note,
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false; isExpanded = false },
                                        onDelete = { onDeleteNote(note) },
                                        onCopy = { clipboardManager.setText(AnnotatedString(note.text)) },
                                        onToggleDone = {
                                            handleToggleDone(
                                                note,
                                                toggleNoteDone,
                                                scope,
                                                styledText,
                                                if (note.priority == 1) colors.priorityText else colors.primaryText
                                            )
                                        },
                                        onSetPriority = {
                                            onUpdateNote(
                                                note.copy(
                                                    priority = if (note.priority == 0) 1 else 0,
                                                    done = false,
                                                    orderAt = Clock.System.now().toEpochMilliseconds()
                                                )
                                            )
                                        },
                                        onEdit = { setEditingNote(note) },
                                        onSetRepeat = { noteForRepeatDialog = note },
                                        onSetReminder = { noteForReminderDialog = note },
                                        onSetDueDate = { noteForDueDateDialog = note },
                                        onMoveTo = { noteForMoveDialog = note }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top fade-to-edge gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(AppTheme.colors.background, Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {}

            // Bottom fade-to-edge gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, AppTheme.colors.background)
                        )
                    ).align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center
            ) {}

            if (noteForRepeatDialog != null) {
                val note = noteForRepeatDialog!!
                RepeatFrequencyDialog(
                    currentFrequency = RepeatFrequency.fromOrdinal(note.repeatFrequency),
                    currentStartDate = note.repeatTaskStartFrom,
                    currentReminderTime = note.reminderAt,
                    isReminderEnabled = note.reminderEnabled == 1 && note.repeatFrequency > 0,
                    onDismiss = { noteForRepeatDialog = null },
                    onConfirm = { frequency, startDate, reminderTime, reminderEnabled ->
                        onSetRepeatFrequency(note, frequency, startDate, reminderTime, reminderEnabled)
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
                    val timeZone = TimeZone.currentSystemDefault()
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
                            onSetReminder(note, reminderTime, true)
                            noteForReminderDialog = null
                        },
                        onClear = {
                            onRemoveReminder(note)
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
                    pages = allPages,
                    subPagesByParent = allSubPagesByParent,
                    onDismiss = { noteForMoveDialog = null },
                    onConfirm = { targetPageId ->
                        onMoveNote(note, targetPageId)
                        noteForMoveDialog = null
                    }
                )
            }

            if (noteForDueDateDialog != null) {
                val note = noteForDueDateDialog!!
                DueDateOptionsDialog(
                    currentDueStartAt = note.dueStartAt,
                    currentDueEndAt = note.dueEndAt,
                    onDismiss = { noteForDueDateDialog = null },
                    onApply = { dueStartAt, dueEndAt ->
                        onSetDueDate(note, dueStartAt, dueEndAt)
                        noteForDueDateDialog = null
                    }
                )
            }

            if (showDeleteCompletedDialog) {
                ConfirmationDialog(
                    title = strings.deleteCompleted,
                    message = strings.moveCompletedMessage(completedTasks.size),
                    confirmText = strings.moveToTrash,
                    onDismiss = { showDeleteCompletedDialog = false },
                    onConfirm = {
                        onDeleteCompletedTasks(completedTasks)
                        showDeleteCompletedDialog = false
                    }
                )
            }
        }
    }
}


/**
 * Text with timestamp placed after the last line if space permits,
 * or below the text right-aligned if not. Text gets full width.
 */
@Composable
private fun NoteTextWithTime(
    text: AnnotatedString,
    textColor: Color,
    timeLabel: String,
    timeColor: Color,
    maxLines: Int,
    modifier: Modifier = Modifier,
) {
    // Plain object to pass layout info from onTextLayout (fires during measure)
    // to the placement phase without triggering recomposition.
    // [lastLineRight, lastBaseline, lineCount]
    val info = remember { FloatArray(3) }

    Layout(
        modifier = modifier,
        content = {
            Text(
                text = text,
                color = textColor,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.5.sp,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result ->
                    val last = result.lineCount - 1
                    info[0] = result.getLineRight(last)
                    info[1] = result.lastBaseline
                    info[2] = result.lineCount.toFloat()
                },
            )
            Text(
                text = timeLabel,
                color = timeColor,
                fontSize = 10.sp,
            )
        }
    ) { measurables, constraints ->
        val textPlaceable = measurables[0].measure(constraints)
        val timePlaceable = measurables[1].measure(constraints.copy(minWidth = 0))

        val lastLineRight = info[0].toInt()
        val textLastBaseline = info[1].toInt()
        val lineCount = info[2].toInt()
        val spacing = (8 * density).toInt()
        val fitsOnLastLine = lastLineRight + spacing + timePlaceable.width <= constraints.maxWidth
        val timeBaseline = timePlaceable[LastBaseline]
        val isMultiLine = lineCount > 1

        val totalHeight = if (fitsOnLastLine) {
            textPlaceable.height
        } else {
            textPlaceable.height + timePlaceable.height
        }

        layout(constraints.maxWidth, totalHeight) {
            textPlaceable.place(0, 0)
            if (fitsOnLastLine) {
                val timeY = textLastBaseline - timeBaseline
                val timeX = if (isMultiLine) {
                    // Multi-line: push time to the right edge
                    constraints.maxWidth - timePlaceable.width
                } else {
                    // Single line: right after the text
                    lastLineRight + spacing
                }
                timePlaceable.place(timeX, timeY)
            } else {
                timePlaceable.place(
                    x = constraints.maxWidth - timePlaceable.width,
                    y = textPlaceable.height
                )
            }
        }
    }
}

@Composable
private fun SubPageTabs(
    subPages: List<Page>,
    selectedSubPageId: Long?,
    onSubPageClick: (Long?) -> Unit,
) {
    val colors = AppTheme.colors

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(subPages, key = { it.id }) { subPage ->
            val isSelected = selectedSubPageId == subPage.id
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) colors.primaryText.copy(alpha = 0.15f) else Color.Transparent)
                    .border(
                        width = 0.5.dp,
                        color = colors.primaryText.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onSubPageClick(if (isSelected) null else subPage.id)
                    }
            ) {
                Text(
                    text = subPage.name,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = TextStyle(
                        color = colors.primaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun NoteActionsMenu(
    note: Note,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onToggleDone: () -> Unit,
    onSetPriority: () -> Unit,
    onEdit: () -> Unit,
    onSetRepeat: () -> Unit,
    onSetReminder: () -> Unit,
    onSetDueDate: () -> Unit,
    onMoveTo: () -> Unit,
) {
    val colors = AppTheme.colors
    val strings = AppTheme.strings
    val currentFrequency = RepeatFrequency.fromOrdinal(note.repeatFrequency)
    val nowMillis = Clock.System.now().toEpochMilliseconds()
    val hasActiveReminder = note.reminderAt > nowMillis && note.reminderEnabled == 1

    // Format reminder time for display (only if reminder is in the future)
    val dateTexts = AppTheme.dateTexts
    val reminderLabel = if (hasActiveReminder) {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(timeZone)
        val reminderDateTime = Instant.fromEpochMilliseconds(note.reminderAt)
            .toLocalDateTime(timeZone)
        dateTexts.reminderLabel(
            reminderEpochDays = reminderDateTime.date.toEpochDays().toInt(),
            hour = reminderDateTime.hour,
            minute = reminderDateTime.minute,
            todayEpochDays = now.date.toEpochDays().toInt(),
        )
    } else {
        strings.reminder
    }

    // Repeat and due date are mutually exclusive: repeating tasks schedule themselves
    val isRepeating = currentFrequency != RepeatFrequency.NONE
    val dueDateLabel = if (note.hasDueDate && !isRepeating) {
    formatDueDateLabel(note.dueStartAt, note.dueEndAt, AppTheme.dateTexts, strings.someday)
    } else {
        strings.dueDate
    }

    data class MenuAction(
        val label: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val tint: Color,
        val onClick: () -> Unit,
    )

    val actions = listOf(
        MenuAction(
            label = if (note.done) strings.markTodo else strings.markDone,
            icon = Icons.Default.Check,
            tint = colors.primaryText,
            onClick = { onToggleDone(); onDismissRequest() }
        ),
        MenuAction(
            label = strings.priority,
            icon = if (note.priority == 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            tint = colors.primaryText,
            onClick = { onSetPriority(); onDismissRequest() }
        ),
        MenuAction(
            label = strings.copy,
            icon = Icons.Default.ContentCopy,
            tint = colors.primaryText,
            onClick = { onCopy(); onDismissRequest() }
        ),
        MenuAction(
            label = reminderLabel,
            icon = Icons.Outlined.Notifications,
            tint = colors.primaryText,
            onClick = { onSetReminder(); onDismissRequest() }
        ),
        MenuAction(
            label = strings.edit,
            icon = Icons.Outlined.EditNote,
            tint = colors.primaryText,
            onClick = { onEdit(); onDismissRequest() }
        ),
        MenuAction(
            label = if (currentFrequency == RepeatFrequency.NONE) strings.repeatLabel else strings.repeatFrequency(currentFrequency),
            icon = Icons.Default.Repeat,
            tint = colors.primaryText,
            onClick = { onSetRepeat(); onDismissRequest() }
        ),
        MenuAction(
            label = strings.moveTo,
            icon = Icons.Outlined.ArrowOutward,
            tint = colors.primaryText,
            onClick = { onMoveTo(); onDismissRequest() },
        ),
        MenuAction(
            label = dueDateLabel,
            icon = Icons.Outlined.CalendarMonth,
            tint = if (isRepeating) colors.primaryText.copy(alpha = 0.33f) else colors.primaryText,
            onClick = if (isRepeating) ({}) else ({ onSetDueDate(); onDismissRequest() })
        ),
        MenuAction(
            label = strings.delete,
            icon = Icons.Default.Delete,
            tint = colors.primaryText,
            onClick = { onDelete(); onDismissRequest() }
        ),
    )

    DropdownMenu(
        modifier = Modifier.background(color = colors.menuBackground),
        expanded = expanded,
        offset = DpOffset(x = 40.dp, y = 0.dp),
        onDismissRequest = onDismissRequest,
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            actions.chunked(2).forEach { rowActions ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    rowActions.forEach { action ->
                        Box(
                            modifier = Modifier
                                .size(width = 80.dp, height = 64.dp)
                                .clickable(onClick = action.onClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    modifier = Modifier.size(22.dp),
                                    imageVector = action.icon,
                                    contentDescription = action.label,
                                    tint = action.tint
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = action.label,
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        color = action.tint,
                                        textAlign = TextAlign.Center
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun handleToggleDone(
    note: Note,
    toggleNoteDone: (Note) -> Unit,
    scope: CoroutineScope,
    styledText: MutableState<AnnotatedString>,
    color: Color,
) {
    if (note.done) {
        toggleNoteDone(note)
        styledText.value = AnnotatedString(note.text)
    } else {
        scope.launch {
            val delayMillis = if (note.text.length > 20) 5L else 20L
            run loop@{
                note.text.forEachIndexed { index, _ ->
                    if (index > 80) return@loop
                    delay(delayMillis)
                    styledText.value = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                textDecoration = TextDecoration.LineThrough,
                                color = color
                            )
                        ) {
                            append(note.text.take(index + 1))
                        }
                        withStyle(
                            style = SpanStyle(
                                color = color
                            )
                        ) {
                            append(note.text.substring(index + 1))
                        }
                    }
                }
            }
            toggleNoteDone(note)
        }
    }
}

@Composable
internal fun RepeatFrequencyDialog(
    currentFrequency: RepeatFrequency,
    currentStartDate: Long,
    currentReminderTime: Long,
    isReminderEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (frequency: RepeatFrequency, startDate: Long, reminderTime: Long?, reminderEnabled: Boolean) -> Unit,
) {
    var selectedFrequency by remember { mutableStateOf(currentFrequency) }
    var reminderEnabled by remember { mutableStateOf(isReminderEnabled) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    val colors = AppTheme.colors
    val strings = AppTheme.strings
    val timeZone = TimeZone.currentSystemDefault()
    val now = Clock.System.now()

    // Initialize start date from current value or today
    val initialStartDate = remember {
        if (currentStartDate > 0) {
            Instant.fromEpochMilliseconds(currentStartDate).toLocalDateTime(timeZone).date
        } else {
            now.toLocalDateTime(timeZone).date
        }
    }
    var selectedStartDate by remember { mutableStateOf(initialStartDate) }

    // Initialize reminder time (just the time component, default to 9:00 AM)
    val initialReminderTime = remember {
        if (currentReminderTime > 0) {
            val dt = Instant.fromEpochMilliseconds(currentReminderTime).toLocalDateTime(timeZone)
            LocalTime(dt.hour, dt.minute)
        } else {
            LocalTime(9, 0)
        }
    }
    var selectedReminderHour by remember { mutableStateOf(initialReminderTime.hour) }
    var selectedReminderMinute by remember { mutableStateOf(initialReminderTime.minute) }

    val dateTexts = AppTheme.dateTexts

    // Format selected start date for display
    val startDateLabel = remember(selectedStartDate, dateTexts) {
        dateTexts.dayMonthYear(selectedStartDate.toEpochDays().toInt())
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.menuBackground,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(strings.repeatTask, color = colors.primaryText, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))

                // 2 column grid for frequency options
                RepeatFrequency.entries.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowItems.forEach { frequency ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { selectedFrequency = frequency }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedFrequency == frequency,
                                    onClick = { selectedFrequency = frequency },
                                    colors = appRadioButtonColors(),
                                )
                                Text(text = strings.repeatFrequency(frequency), color = colors.primaryText)
                            }
                        }
                        // Add empty space if odd number of items in last row
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Start date selector (only show when not NONE)
                if (selectedFrequency != RepeatFrequency.NONE) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = colors.divider)
                    Spacer(Modifier.height(16.dp))

                    // Start date row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showStartDatePicker = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.startFrom, color = colors.primaryText, fontSize = 15.sp)
                        Text(
                            text = startDateLabel,
                            color = colors.primaryText.copy(alpha = 0.7f),
                            fontSize = 15.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = colors.divider)
                    Spacer(Modifier.height(16.dp))

                    // Reminder toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { reminderEnabled = !reminderEnabled },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.remindMe, color = colors.primaryText, fontSize = 15.sp)
                        androidx.compose.material3.Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            colors = appSwitchColors(),
                        )
                    }

                    // Time picker (only show when reminder is enabled)
                    if (reminderEnabled) {
                        Spacer(Modifier.height(16.dp))
                        RepeatReminderTimePicker(
                            selectedHour = selectedReminderHour,
                            selectedMinute = selectedReminderMinute,
                            onTimeSelected = { hour, minute ->
                                selectedReminderHour = hour
                                selectedReminderMinute = minute
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clear button (only shown if there's an existing repeat setting)
                    if (currentFrequency != RepeatFrequency.NONE) {
                        TextButton(onClick = {
                            onConfirm(RepeatFrequency.NONE, 0L, null, false)
                        }) {
                            Text(strings.clear, color = colors.primaryText.copy(alpha = 0.6f))
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) {
                            Text(strings.cancel, color = colors.primaryText)
                        }
                        Button(
                            onClick = {
                                val startDateMillis = LocalDateTime(selectedStartDate, LocalTime(0, 0))
                                    .toInstant(timeZone).toEpochMilliseconds()
                                val reminderTimeMillis = if (reminderEnabled && selectedFrequency != RepeatFrequency.NONE) {
                                    // Calculate the actual future reminder timestamp
                                    // Use start date with the selected reminder time
                                    val reminderDateTime = LocalDateTime(
                                        selectedStartDate,
                                        LocalTime(selectedReminderHour, selectedReminderMinute)
                                    )
                                    var reminderTimestamp = reminderDateTime.toInstant(timeZone).toEpochMilliseconds()

                                    // If the reminder time is in the past, schedule for the next occurrence
                                    val nowMillis = now.toEpochMilliseconds()
                                    if (reminderTimestamp <= nowMillis) {
                                        // Add the frequency interval to get the next future occurrence
                                        val nextDate = when (selectedFrequency) {
                                            RepeatFrequency.DAILY -> selectedStartDate.plus(1, DateTimeUnit.DAY)
                                            RepeatFrequency.WEEKLY -> selectedStartDate.plus(7, DateTimeUnit.DAY)
                                            RepeatFrequency.MONTHLY -> selectedStartDate.plus(1, DateTimeUnit.MONTH)
                                            RepeatFrequency.QUARTERLY -> selectedStartDate.plus(3, DateTimeUnit.MONTH)
                                            RepeatFrequency.YEARLY -> selectedStartDate.plus(1, DateTimeUnit.YEAR)
                                            RepeatFrequency.NONE -> selectedStartDate
                                        }
                                        reminderTimestamp = LocalDateTime(
                                            nextDate,
                                            LocalTime(selectedReminderHour, selectedReminderMinute)
                                        ).toInstant(timeZone).toEpochMilliseconds()
                                    }
                                    reminderTimestamp
                                } else null
                                onConfirm(
                                    selectedFrequency,
                                    startDateMillis,
                                    reminderTimeMillis,
                                    reminderEnabled && selectedFrequency != RepeatFrequency.NONE
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primaryText,
                                contentColor = colors.menuBackground
                            )
                        ) {
                            Text(strings.save)
                        }
                    }
                }
            }
        }
    }

    // Start date picker dialog
    if (showStartDatePicker) {
        StartDatePickerDialog(
            initialDate = selectedStartDate,
            onDismiss = { showStartDatePicker = false },
            onConfirm = { date ->
                selectedStartDate = date
                showStartDatePicker = false
            }
        )
    }
}

@Composable
private fun RepeatReminderTimePicker(
    selectedHour: Int,
    selectedMinute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
) {
    val colors = AppTheme.colors
    val dateTexts = AppTheme.dateTexts

    // Convert 24-hour to 12-hour format
    val isAm = selectedHour < 12
    val hour12 = when {
        selectedHour == 0 -> 12
        selectedHour > 12 -> selectedHour - 12
        else -> selectedHour
    }

    fun toHour24(hour12: Int, isAm: Boolean): Int {
        return when {
            hour12 == 12 && isAm -> 0
            hour12 == 12 && !isAm -> 12
            isAm -> hour12
            else -> hour12 + 12
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Digits read left-to-right even in Farsi, so the time keeps its hour : minute
            // order in both languages; only the AM/PM marker moves, matching how the
            // formatted labels render («ب.ظ ۳:۰۵»). Under an RTL layout direction the
            // Row mirrors child order, so there the children are declared minute-first.
            val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

            val hourWheel: @Composable () -> Unit = {
                // Hour picker (1-12)
                CompactWheelPicker(
                    items = (1..12).toList(),
                    selectedItem = hour12,
                    onItemSelected = { newHour12 ->
                        onTimeSelected(toHour24(newHour12, isAm), selectedMinute)
                    },
                    format = { dateTexts.num2(it) }
                )
            }
            val colon: @Composable () -> Unit = {
                Text(
                    text = ":",
                    color = colors.icon,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            val minuteWheel: @Composable () -> Unit = {
                // Minute picker
                CompactWheelPicker(
                    items = (0..59).toList(),
                    selectedItem = selectedMinute,
                    onItemSelected = { onTimeSelected(selectedHour, it) },
                    format = { dateTexts.num2(it) }
                )
            }
            val amPmWheel: @Composable () -> Unit = {
                Spacer(Modifier.width(8.dp))

                // AM/PM picker
                CompactWheelPickerGeneric(
                    items = listOf(true, false),
                    selectedItem = isAm,
                    onItemSelected = { newIsAm ->
                        onTimeSelected(toHour24(hour12, newIsAm), selectedMinute)
                    },
                    format = { dateTexts.amPm(it) },
                    itemWidth = 52.dp
                )
            }

            if (rtl) {
                minuteWheel()
                colon()
                hourWheel()
                amPmWheel()
            } else {
                hourWheel()
                colon()
                minuteWheel()
                amPmWheel()
            }
        }
    }
}

@Composable
private fun CompactWheelPicker(
    items: List<Int>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    format: (Int) -> String = { it.toString() },
) {
    val colors = AppTheme.colors
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val itemHeight = 36.dp
    val visibleItems = 3
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    LaunchedEffect(Unit) {
        val index = items.indexOf(selectedItem)
        if (index >= 0) {
            listState.scrollToItem(index)
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val firstVisibleOffset = listState.firstVisibleItemScrollOffset

            val centerIndex = if (firstVisibleOffset > itemHeightPx / 2) {
                firstVisibleIndex + 1
            } else {
                firstVisibleIndex
            }

            if (centerIndex in items.indices && items[centerIndex] != selectedItem) {
                onItemSelected(items[centerIndex])
            }
            listState.animateScrollToItem(centerIndex)
        }
    }

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(itemHeight * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = itemHeight),
            thickness = 1.dp,
            color = colors.primaryText.copy(alpha = 0.2f)
        )

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = itemHeight),
            thickness = 1.dp,
            color = colors.primaryText.copy(alpha = 0.2f)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * visibleItems),
            contentPadding = PaddingValues(vertical = itemHeight),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            items(items) { item ->
                val isSelected = item == selectedItem

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val index = items.indexOf(item)
                            onItemSelected(item)
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(item),
                        color = if (isSelected) colors.primaryText else colors.primaryText.copy(alpha = 0.4f),
                        fontSize = if (isSelected) 18.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> CompactWheelPickerGeneric(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    format: (T) -> String,
    itemWidth: Dp = 48.dp,
) {
    val colors = AppTheme.colors
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val itemHeight = 36.dp
    val visibleItems = 3
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    LaunchedEffect(Unit) {
        val index = items.indexOf(selectedItem)
        if (index >= 0) {
            listState.scrollToItem(index)
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val firstVisibleOffset = listState.firstVisibleItemScrollOffset

            val centerIndex = if (firstVisibleOffset > itemHeightPx / 2) {
                firstVisibleIndex + 1
            } else {
                firstVisibleIndex
            }

            if (centerIndex in items.indices && items[centerIndex] != selectedItem) {
                onItemSelected(items[centerIndex])
            }
            listState.animateScrollToItem(centerIndex)
        }
    }

    Box(
        modifier = Modifier
            .width(itemWidth)
            .height(itemHeight * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = itemHeight),
            thickness = 1.dp,
            color = colors.primaryText.copy(alpha = 0.2f)
        )

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = itemHeight),
            thickness = 1.dp,
            color = colors.primaryText.copy(alpha = 0.2f)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * visibleItems),
            contentPadding = PaddingValues(vertical = itemHeight),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isSelected = item == selectedItem

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onItemSelected(item)
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(item),
                        color = if (isSelected) colors.primaryText else colors.primaryText.copy(alpha = 0.4f),
                        fontSize = if (isSelected) 16.sp else 12.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
internal fun StartDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    title: String = AppTheme.strings.selectStartDate,
) {
    val colors = AppTheme.colors
    val strings = AppTheme.strings
    val dateTexts = AppTheme.dateTexts
    val isJalali = AppTheme.calendar == AppCalendar.JALALI
    var selectedDate by remember { mutableStateOf(initialDate) }
    val timeZone = TimeZone.currentSystemDefault()
    val today = remember { Clock.System.now().toLocalDateTime(timeZone).date }

    // Today and the selected date, broken down in the active calendar
    val todayParts = dateTexts.parts(today.toEpochDays().toInt())
    val selectedParts = dateTexts.parts(selectedDate.toEpochDays().toInt())

    // Generate year range (last year to +10 years) in the active calendar
    val years = remember(todayParts.year) { ((todayParts.year - 1)..(todayParts.year + 10)).toList() }
    val months = remember { (1..12).toList() }

    var selectedYear by remember { mutableStateOf(selectedParts.year) }
    var selectedMonth by remember { mutableStateOf(selectedParts.month) }
    var selectedDay by remember { mutableStateOf(selectedParts.day) }

    // Calculate days in the selected month, in the active calendar
    val daysInMonth = remember(selectedYear, selectedMonth, isJalali) {
        if (isJalali) {
            JalaliCalendar.monthLength(selectedYear, selectedMonth)
        } else {
            val gregorianMonth = kotlinx.datetime.Month.entries[selectedMonth - 1]
            when (gregorianMonth) {
                kotlinx.datetime.Month.JANUARY, kotlinx.datetime.Month.MARCH, kotlinx.datetime.Month.MAY,
                kotlinx.datetime.Month.JULY, kotlinx.datetime.Month.AUGUST, kotlinx.datetime.Month.OCTOBER,
                kotlinx.datetime.Month.DECEMBER,
                    -> 31

                kotlinx.datetime.Month.APRIL, kotlinx.datetime.Month.JUNE, kotlinx.datetime.Month.SEPTEMBER,
                kotlinx.datetime.Month.NOVEMBER,
                    -> 30

                kotlinx.datetime.Month.FEBRUARY -> if ((selectedYear % 4 == 0 && selectedYear % 100 != 0) || (selectedYear % 400 == 0)) 29 else 28
            }
        }
    }
    val days = remember(daysInMonth) { (1..daysInMonth).toList() }

    LaunchedEffect(daysInMonth) {
        if (selectedDay > daysInMonth) {
            selectedDay = daysInMonth
        }
    }

    LaunchedEffect(selectedYear, selectedMonth, selectedDay) {
        val day = selectedDay.coerceAtMost(daysInMonth)
        val newDate = if (isJalali) {
            LocalDate.fromEpochDays(JalaliCalendar.toEpochDays(selectedYear, selectedMonth, day))
        } else {
            LocalDate(selectedYear, selectedMonth, day)
        }
        if (newDate != selectedDate) {
            selectedDate = newDate
        }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.menuBackground,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    color = colors.primaryText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Digits and month names read left-to-right even in Farsi, so the wheels keep
                    // their year | month | day left-to-right order in both languages. Under an RTL
                    // layout direction the Row mirrors child order, so there the wheels are
                    // declared day-first to cancel the mirroring and match the date labels.
                    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

                    val yearWheel: @Composable () -> Unit = {
                        // Year picker
                        StartDateWheelPicker(
                            items = years,
                            selectedItem = selectedYear,
                            onItemSelected = { selectedYear = it },
                            format = { dateTexts.num(it) },
                            itemWidth = 64.dp
                        )
                    }
                    val monthWheel: @Composable () -> Unit = {
                        // Month picker
                        StartDateWheelPicker(
                            items = months,
                            selectedItem = selectedMonth,
                            onItemSelected = { selectedMonth = it },
                            format = { dateTexts.monthAbbrev(it) },
                            itemWidth = 92.dp
                        )
                    }
                    val dayWheel: @Composable () -> Unit = {
                        // Day picker
                        StartDateWheelPicker(
                            items = days,
                            selectedItem = selectedDay,
                            onItemSelected = { selectedDay = it },
                            format = { dateTexts.num(it) }
                        )
                    }

                    if (rtl) {
                        dayWheel()
                        Spacer(Modifier.width(8.dp))
                        monthWheel()
                        Spacer(Modifier.width(8.dp))
                        yearWheel()
                    } else {
                        yearWheel()
                        Spacer(Modifier.width(8.dp))
                        monthWheel()
                        Spacer(Modifier.width(8.dp))
                        dayWheel()
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel, color = colors.primaryText)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedDate) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryText,
                            contentColor = colors.menuBackground
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(strings.done)
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> StartDateWheelPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    format: (T) -> String,
    itemWidth: Dp = 56.dp,
) {
    val colors = AppTheme.colors
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val itemHeight = 40.dp
    val visibleItems = 3
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    LaunchedEffect(Unit) {
        val index = items.indexOf(selectedItem)
        if (index >= 0) {
            listState.scrollToItem(index)
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val firstVisibleOffset = listState.firstVisibleItemScrollOffset

            val centerIndex = if (firstVisibleOffset > itemHeightPx / 2) {
                firstVisibleIndex + 1
            } else {
                firstVisibleIndex
            }

            if (centerIndex in items.indices && items[centerIndex] != selectedItem) {
                onItemSelected(items[centerIndex])
            }
            listState.animateScrollToItem(centerIndex)
        }
    }

    Box(
        modifier = Modifier
            .width(itemWidth)
            .height(itemHeight * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = itemHeight),
            thickness = 1.dp,
            color = colors.primaryText.copy(alpha = 0.2f)
        )

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = itemHeight),
            thickness = 1.dp,
            color = colors.primaryText.copy(alpha = 0.2f)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * visibleItems),
            contentPadding = PaddingValues(vertical = itemHeight),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isSelected = item == selectedItem

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onItemSelected(item)
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(item),
                        color = if (isSelected) colors.primaryText else colors.primaryText.copy(alpha = 0.4f),
                        fontSize = if (isSelected) 18.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
internal fun MoveToDialog(
    currentPageId: Long,
    pages: List<Page>,
    subPagesByParent: Map<Long, List<Page>>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val colors = AppTheme.colors
    val strings = AppTheme.strings

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.menuBackground,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    strings.moveTo,
                    color = colors.primaryText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pages.forEach { page ->
                        val subPages = subPagesByParent[page.id] ?: emptyList()
                        val isCurrentPage = page.id == currentPageId

                        // Parent page item
                        item(key = "page_${page.id}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isCurrentPage) colors.primaryText.copy(alpha = 0.1f)
                                        else Color.Transparent
                                    )
                                    .clickable(enabled = !isCurrentPage) { onConfirm(page.id) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = page.name,
                                    color = if (isCurrentPage) colors.primaryText.copy(alpha = 0.5f)
                                    else colors.primaryText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (isCurrentPage) {
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = strings.current,
                                        color = colors.primaryText.copy(alpha = 0.4f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Sub-pages
                        items(subPages, key = { "subpage_${it.id}" }) { subPage ->
                            val isCurrentSubPage = subPage.id == currentPageId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 24.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isCurrentSubPage) colors.primaryText.copy(alpha = 0.1f)
                                        else Color.Transparent
                                    )
                                    .clickable(enabled = !isCurrentSubPage) { onConfirm(subPage.id) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subPage.name,
                                    color = if (isCurrentSubPage) colors.primaryText.copy(alpha = 0.5f)
                                    else colors.primaryText,
                                    fontSize = 14.sp
                                )
                                if (isCurrentSubPage) {
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = strings.current,
                                        color = colors.primaryText.copy(alpha = 0.4f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel, color = colors.primaryText)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = AppTheme.colors
    val strings = AppTheme.strings
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.menuBackground,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(title, color = colors.primaryText, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                Text(message, color = colors.primaryText)
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel, color = colors.primaryText)
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryText,
                            contentColor = colors.menuBackground
                        )
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}