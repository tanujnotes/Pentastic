@file:OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)

package app.pentastic.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.pentastic.data.Note
import app.pentastic.data.Page
import app.pentastic.data.RepeatFrequency
import app.pentastic.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.Font
import pentastic.composeapp.generated.resources.Merriweather_Light
import pentastic.composeapp.generated.resources.Merriweather_Regular
import pentastic.composeapp.generated.resources.Res
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
    onSetRepeatFrequency: (Note, RepeatFrequency) -> Unit,
    onSetReminder: (Note, Long, Boolean) -> Unit,
    onRemoveReminder: (Note) -> Unit,
) {
    val noteMovedToIndex = remember { mutableStateOf(-1) }
    var noteForRepeatDialog by remember { mutableStateOf<Note?>(null) }
    var noteForReminderDialog by remember { mutableStateOf<Note?>(null) }
    val focusManager = LocalFocusManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val displayedNotes = remember(notes, selectedSubPageId, notesByPage) {
        if (selectedSubPageId == null)
            notes
        else
            notesByPage[selectedSubPageId] ?: emptyList()
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

    LaunchedEffect(displayedNotes) {
        list = displayedNotes
        if (displayedNotes.isNotEmpty()) {
            lazyListState.animateScrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 16.dp, bottom = if (subPages.isEmpty()) 24.dp else 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = page.name,
                style = TextStyle(
                    color = AppTheme.colors.pageTitle,
                    fontSize = 36.sp,
                    fontFamily = FontFamily(Font(Res.font.Merriweather_Light))
                )
            )
        }

        if (subPages.isNotEmpty()) {
            SubPageTabs(
                subPages = subPages,
                selectedSubPageId = selectedSubPageId,
                onSubPageClick = onSelectedSubPageChange
            )
            Spacer(Modifier.height(18.dp))
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
            ) {
                itemsIndexed(list, key = { _, it -> it.id }) { index, note ->
                    if (note.done) Spacer(modifier = Modifier.height(14.dp))

                    ReorderableItem(reorderableLazyColumnState, note.id) { isDragging ->
                        val interactionSource = remember { MutableInteractionSource() }
                        var showMenu by remember { mutableStateOf(false) }
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
                                }
                                .longPressDraggableHandle(
                                    onDragStarted = {
                                    },
                                    onDragStopped = {
                                        if (noteMovedToIndex.value == -1) return@longPressDraggableHandle
                                        if (noteMovedToIndex.value == 0)
                                            onUpdateNote(
                                                list[0].copy(
                                                    orderAt = Clock.System.now().toEpochMilliseconds(),
                                                    done = false,
                                                    priority = list[1].priority
                                                )
                                            )
                                        else if (noteMovedToIndex.value > 0) {
                                            onUpdateNote(
                                                list[noteMovedToIndex.value].copy(
                                                    orderAt = list[noteMovedToIndex.value - 1].orderAt - 1,
                                                    done = list[noteMovedToIndex.value - 1].done
                                                )
                                            )
                                        }
                                        noteMovedToIndex.value = -1
                                    },
                                    interactionSource = interactionSource,
                                ).animateItem(),
                        ) {
                            Row(Modifier.fillMaxSize()) {
                                Text(
                                    modifier = Modifier.padding(start = 12.dp, top = 5.dp).defaultMinSize(minWidth = 28.dp),
                                    fontFamily = FontFamily(Font(Res.font.Merriweather_Regular)),
                                    text = (index + 1).toString() + ".",
                                    // color = if (note.priority == 1 && note.done.not()) colors.priorityText.copy(alpha = 0.7f) else colors.primaryText.copy(alpha = 0.33f),
                                    color = colors.primaryText.copy(alpha = 0.33f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 16.sp,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp).weight(1f),
                                    text = styledText.value,
                                    color = colors.primaryText.copy(alpha = if (note.done) 0.33f else 1f),
                                    fontSize = 18.sp,
                                    lineHeight = 20.sp,
                                    letterSpacing = 0.5.sp,
                                    maxLines = if (showMenu) Int.MAX_VALUE else if (note.done) 1 else 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                                            orderAt = Clock.System.now().toEpochMilliseconds()
                                        )
                                    )
                                },
                                onEdit = { setEditingNote(note) },
                                onSetRepeat = { noteForRepeatDialog = note },
                                onSetReminder = { noteForReminderDialog = note }
                            )
                        }
                    }
                    if (!note.done)
                        Spacer(modifier = Modifier.height(14.dp))
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
                    .height(32.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, AppTheme.colors.background)
                        )
                    ).align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center
            ) {}

            if (noteForRepeatDialog != null) {
                RepeatFrequencyDialog(
                    currentFrequency = RepeatFrequency.fromOrdinal(noteForRepeatDialog!!.repeatFrequency),
                    onDismiss = { noteForRepeatDialog = null },
                    onConfirm = { frequency ->
                        onSetRepeatFrequency(noteForRepeatDialog!!, frequency)
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

                    DateTimePickerDialog(
                        initialDate = initialDateTime.date,
                        initialHour = initialDateTime.hour,
                        initialMinute = initialDateTime.minute,
                        hasExistingReminder = note.reminderAt > 0,
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
                        }
                    )
                }
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
private fun NoteActionsMenu(
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
) {
    val colors = AppTheme.colors
    val currentFrequency = RepeatFrequency.fromOrdinal(note.repeatFrequency)
    val hasReminder = note.reminderAt > 0 && note.reminderEnabled == 1

    // Format reminder time for display
    val reminderLabel = if (hasReminder) {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(timeZone)
        val reminderDateTime = Instant.fromEpochMilliseconds(note.reminderAt)
            .toLocalDateTime(timeZone)

        when {
            // Same day - show time only
            reminderDateTime.date == now.date -> {
                val hour = reminderDateTime.hour
                val minute = reminderDateTime.minute
                val amPm = if (hour < 12) "AM" else "PM"
                val hour12 = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                "$hour12:${minute.toString().padStart(2, '0')} $amPm"
            }
            // Same year - show month and date
            reminderDateTime.year == now.year -> {
                val day = reminderDateTime.dayOfMonth
                val month = reminderDateTime.month.name.take(3).lowercase()
                    .replaceFirstChar { it.uppercase() }
                "$day $month"
            }
            // Different year - show year only
            else -> {
                reminderDateTime.year.toString()
            }
        }
    } else
        "Reminder"

    data class MenuAction(
        val label: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val tint: Color,
        val onClick: () -> Unit,
    )

    val actions = listOf(
        MenuAction(
            label = if (note.done) "Todo" else "Done",
            icon = Icons.Default.Check,
            tint = colors.primaryText,
            onClick = { onToggleDone(); onDismissRequest() }
        ),
        MenuAction(
            label = "Priority",
            icon = if (note.priority == 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            tint = colors.primaryText,
            onClick = { onSetPriority(); onDismissRequest() }
        ),
        MenuAction(
            label = "Copy",
            icon = Icons.Default.ContentCopy,
            tint = colors.primaryText,
            onClick = { onCopy(); onDismissRequest() }
        ),
        MenuAction(
            label = "Edit",
            icon = Icons.Default.Edit,
            tint = colors.primaryText,
            onClick = { onEdit(); onDismissRequest() }
        ),
        MenuAction(
            label = if (currentFrequency == RepeatFrequency.NONE) "Repeat" else currentFrequency.label,
            icon = Icons.Default.Repeat,
            tint = colors.primaryText,
            onClick = { onSetRepeat(); onDismissRequest() }
        ),
        MenuAction(
            label = reminderLabel,
            icon = Icons.Default.Notifications,
            tint = colors.primaryText,
            onClick = { onSetReminder(); onDismissRequest() }
        ),
        MenuAction(
            label = "Delete",
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
                                    maxLines = 1
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
private fun RepeatFrequencyDialog(
    currentFrequency: RepeatFrequency,
    onDismiss: () -> Unit,
    onConfirm: (RepeatFrequency) -> Unit,
) {
    var selectedFrequency by remember { mutableStateOf(currentFrequency) }
    val colors = AppTheme.colors

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.menuBackground,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Repeat task", color = colors.primaryText, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                RepeatFrequency.entries.forEach { frequency ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFrequency = frequency }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = selectedFrequency == frequency,
                            onClick = { selectedFrequency = frequency }
                        )
                        Text(text = frequency.label, color = colors.primaryText)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = colors.primaryText)
                    }
                    TextButton(onClick = { onConfirm(selectedFrequency) }) {
                        Text("Save", color = colors.primaryText)
                    }
                }
            }
        }
    }
}
