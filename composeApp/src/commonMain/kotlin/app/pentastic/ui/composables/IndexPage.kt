@file:OptIn(
    ExperimentalTime::class, ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)

package app.pentastic.ui.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pentastic.data.Page
import app.pentastic.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun IndexPage(
    pages: List<Page>,
    notesCountByPage: Map<Long, Int>,
    priorityNotesCountByPage: Map<Long, Int>,
    showRateButton: Boolean,
    onPageClick: (Long) -> Unit,
    onPageNameChange: (Page, String) -> Unit,
    onPageOrderChange: (List<Page>) -> Unit,
    onPageDelete: (Page) -> Unit,
) {
    val viewModel = koinViewModel<MainViewModel>()

    var showRenameDialog by remember { mutableStateOf(false) }
    var pageToRename: Page? by remember { mutableStateOf(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pageToDelete: Page? by remember { mutableStateOf(null) }

    var localPages by remember { mutableStateOf(pages.filter { it.id != 0L }) }
    val uriHandler = LocalUriHandler.current
    var showTopMenu by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pages) {
        localPages = pages.filter { it.id != 0L }
    }

    var isReorderMode by remember { mutableStateOf(false) }

    BackHandler(enabled = isReorderMode) {
        isReorderMode = false
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localPages = localPages.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isReorderMode) "Reorder" else "Index",
                style = TextStyle(
                    color = Color(0xFF933A3A),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light
                )
            )
            if (isReorderMode) {
                Text(
                    text = "Done",
                    style = TextStyle(color = Color(0xFF284283), fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp).clickable(onClick = {
                        isReorderMode = false
                    })
                )
            } else {
                Row {
                    if (showRateButton)
                        Text(
                            text = "Rate",
                            style = TextStyle(fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp).clickable(onClick = {
                                viewModel.onRateClicked()
                                uriHandler.openUri("https://play.google.com/store/apps/details?id=app.pentastic")
                            })
                        )
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color.LightGray
                            )
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false },
                            modifier = Modifier.background(color = Color(0xFFF9FBFF)),
                            offset = DpOffset(x = 0.dp, y = 0.dp),
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rate", color = Color(0xFF284283)) },
                                onClick = {
                                    showTopMenu = false
                                    viewModel.onRateClicked()
                                    uriHandler.openUri("https://play.google.com/store/apps/details?id=app.pentastic")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share", color = Color(0xFF284283)) },
                                onClick = {
                                    showTopMenu = false
                                    coroutineScope.launch {
                                        clipboardManager.setText(AnnotatedString("Minimal Todo Lists - It's Pentastic!\nhttps://play.google.com/store/apps/details?id=app.pentastic"))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Follow", color = Color(0xFF284283)) },
                                onClick = {
                                    showTopMenu = false
                                    uriHandler.openUri("https://twitter.com/tanujnotes")
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState
            ) {
                itemsIndexed(localPages, key = { _, it -> it.id }) { index, page ->
                    ReorderableItem(reorderableState, key = page.id) { isDragging ->
                        val interactionSource = remember { MutableInteractionSource() }
                        var showMenu by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDragging) Color.White.copy(alpha = 0.8f) else Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .then(
                                        if (!isReorderMode) {
                                            Modifier.combinedClickable(
                                                interactionSource = interactionSource,
                                                indication = null,
                                                onClick = { onPageClick(page.id) },
                                                onLongClick = { showMenu = true }
                                            )
                                        } else Modifier
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    fontSize = 18.sp,
                                    color = Color.LightGray,
                                    modifier = Modifier.defaultMinSize(minWidth = 32.dp)
                                )
                                Spacer(Modifier.width(8.dp))

                                Text(
                                    text = page.name.take(20),
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                Spacer(Modifier.width(6.dp))

                                if (!isReorderMode) {
                                    Text(
                                        text = "................................................................................................................... ",
                                        color = Color.LightGray,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        modifier = Modifier.defaultMinSize(minWidth = 16.dp),
                                        text = (
                                                if ((priorityNotesCountByPage[page.id] ?: 0) > 0)
                                                    priorityNotesCountByPage[page.id]
                                                else
                                                    (notesCountByPage[page.id] ?: 0)
                                                ).toString(),
                                        fontSize = 18.sp,
                                        color = if ((priorityNotesCountByPage[page.id] ?: 0) > 0) Color(0xFFD01616)
                                        else if ((notesCountByPage[page.id] ?: 0) > 0) Color.Gray
                                        else Color.LightGray,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Reorder",
                                        tint = Color.LightGray,
                                        modifier = Modifier.draggableHandle(
                                            onDragStopped = {
                                                onPageOrderChange(localPages)
                                            },
                                            interactionSource = interactionSource
                                        )
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                offset = DpOffset(x = 42.dp, y = 0.dp),
                                modifier = Modifier.background(color = Color(0xFFF9FBFF)),
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    onClick = {
                                        showMenu = false
                                        pageToRename = page
                                        showRenameDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, tint = Color.Gray, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reorder") },
                                    onClick = {
                                        showMenu = false
                                        isReorderMode = true
                                    },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, tint = Color.Gray, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        showMenu = false
                                        pageToDelete = page
                                        showDeleteDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, tint = Color.Gray, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            }

            if (showRenameDialog && pageToRename != null) {
                EditPageNameDialog(
                    page = pageToRename!!,
                    onDismiss = { showRenameDialog = false },
                    onConfirm = { newName ->
                        onPageNameChange(pageToRename!!, newName.ifBlank { "Page" })
                        showRenameDialog = false
                    }
                )
            }

            if (showDeleteDialog && pageToDelete != null) {
                DeletePageConfirmationDialog(
                    page = pageToDelete!!,
                    onDismiss = { showDeleteDialog = false },
                    onConfirm = {
                        onPageDelete(pageToDelete!!)
                        showDeleteDialog = false
                    }
                )
            }

            // Top fade-to-edge gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFF9FBFF), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {}

            // Bottom fade-to-edge gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFFF9FBFF))
                        )
                    ).align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center
            ) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPageNameDialog(
    page: Page,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(page.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit page name") },
        text = {
            OutlinedTextField(
                value = text.take(20),
                onValueChange = { if (it.length <= 20) text = it },
                label = { Text("Page ${page.id}.") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeletePageConfirmationDialog(
    page: Page,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete page") },
        text = { Text("Are you sure you want to delete page '${page.name}' and all its notes?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview
@Composable
fun IndexPagePreview() {
    Surface(color = Color.White) {
        IndexPage(
            listOf(Page(1, name = "Default"), Page(2, name = "Todo later"), Page(5, name = "Pro Launcher")),
            mapOf(
                1L to 5,
                2L to 3,
                5L to 18,
                8L to 1,
                10L to 12
            ),
            mapOf(1L to 4),
            false,
            onPageClick = {},
            onPageNameChange = { _, _ -> },
            onPageOrderChange = {},
            {},
        )
    }
}