@file:OptIn(ExperimentalTime::class)

package app.pentastic.ui.composables

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.pentastic.data.Note
import app.pentastic.data.Page
import app.pentastic.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun NotePage(
    notes: List<Note>,
    onUpdateNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    toggleNoteDone: (Note) -> Unit,
    page: Page,
    setEditingNote: (Note?) -> Unit,
) {
    val noteMovedToIndex = remember { mutableStateOf(-1) }
    val focusManager = LocalFocusManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var list by remember { mutableStateOf(notes) }
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

    LaunchedEffect(notes) {
        list = notes
        if (notes.isNotEmpty()) {
            lazyListState.animateScrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 16.dp, bottom = 26.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = page.name,
                style = TextStyle(
                    color = AppTheme.colors.pageTitle,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light
                )
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
            ) {
                itemsIndexed(list, key = { _, it -> it.id }) { index, note ->
                    if (note.done) Spacer(modifier = Modifier.height(12.dp))

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
                                            handleToggleDone(note, toggleNoteDone, scope, styledText)
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
                                    modifier = Modifier.padding(start = 12.dp, top = 4.dp).defaultMinSize(minWidth = 28.dp),
                                    text = (index + 1).toString() + ".",
                                    color = if (note.priority == 1) colors.priorityText.copy(alpha = 0.7f) else colors.primaryText.copy(alpha = 0.33f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 17.sp,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp).weight(1f),
                                    text = styledText.value,
                                    color = colors.primaryText.copy(alpha = if (note.done) 0.33f else 1f),
                                    fontSize = 17.sp,
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
                                onToggleDone = { handleToggleDone(note, toggleNoteDone, scope, styledText) },
                                onSetPriority = {
                                    onUpdateNote(
                                        note.copy(
                                            priority = if (note.priority == 0) 1 else 0,
                                            done = false,
                                            orderAt = Clock.System.now().toEpochMilliseconds()
                                        )
                                    )
                                },
                                onEdit = { setEditingNote(note) }
                            )
                        }
                    }
                    if (!note.done)
                        Spacer(modifier = Modifier.height(12.dp))
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
) {
    val colors = AppTheme.colors
    DropdownMenu(
        modifier = Modifier.background(color = colors.menuBackground),
        expanded = expanded,
        offset = DpOffset(x = 80.dp, y = 0.dp),
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuItem(
            text = { Text(if (note.done) "Todo" else "Done", color = colors.primaryText) },
            leadingIcon = {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Default.Check,
                    contentDescription = "Done",
                    tint = colors.primaryText
                )
            },
            onClick = {
                onToggleDone()
                onDismissRequest()
            }
        )

        DropdownMenuItem(
            text = {
                Text(
                    text = "Priority",
                    color = colors.primaryText
                )
            },
            leadingIcon = {
                Icon(
                    modifier = Modifier.size(22.dp),
                    imageVector = if (note.priority == 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = "Change priority",
                    tint = colors.primaryText
                )
            },
            onClick = {
                onSetPriority()
                onDismissRequest()
            }
        )

        DropdownMenuItem(
            text = { Text("Copy", color = colors.primaryText) },
            leadingIcon = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy note",
                    tint = colors.primaryText
                )
            },
            onClick = {
                onCopy()
                onDismissRequest()
            }
        )

        DropdownMenuItem(
            text = { Text("Edit", color = colors.primaryText) },
            leadingIcon = {
                Icon(
                    modifier = Modifier.size(22.dp),
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit note",
                    tint = colors.primaryText
                )
            },
            onClick = {
                onEdit()
                onDismissRequest()
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = colors.divider
        )

        DropdownMenuItem(
            text = { Text("Delete", color = colors.primaryText) },
            leadingIcon = {
                Icon(
                    modifier = Modifier.size(22.dp),
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete note",
                    tint = colors.primaryText
                )
            },
            onClick = {
                onDelete()
                onDismissRequest()
            }
        )
    }
}

private fun handleToggleDone(
    note: Note,
    toggleNoteDone: (Note) -> Unit,
    scope: CoroutineScope,
    styledText: MutableState<AnnotatedString>,
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
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(note.text.take(index + 1))
                        }
                        append(note.text.substring(index + 1))
                    }
                }
            }
            toggleNoteDone(note)
        }
    }
}
