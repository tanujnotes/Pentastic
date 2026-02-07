@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)

package app.pentastic.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pentastic.data.Note
import app.pentastic.data.Page
import app.pentastic.ui.theme.AppTheme
import app.pentastic.ui.theme.AppTheme.colors
import app.pentastic.ui.viewmodel.MainViewModel
import org.jetbrains.compose.resources.Font
import org.koin.compose.viewmodel.koinViewModel
import pentastic.composeapp.generated.resources.Merriweather_Light
import pentastic.composeapp.generated.resources.Res
import kotlin.time.ExperimentalTime

@Composable
fun TrashScreen(onNavigateBack: () -> Unit) {
    val viewModel = koinViewModel<MainViewModel>()
    val trashedPages by viewModel.trashedPages.collectAsState()
    val trashedNotes by viewModel.trashedNotes.collectAsState()

    // Filter: show only pages whose parent is not also trashed (avoid duplicates)
    val trashedPageIds = trashedPages.map { it.id }.toSet()
    val visibleTrashedPages = trashedPages.filter { page ->
        page.parentId == null || page.parentId !in trashedPageIds
    }

    // Filter: show only individually trashed notes whose page is NOT trashed
    val visibleTrashedNotes = trashedNotes.filter { note ->
        note.pageId !in trashedPageIds
    }

    val isEmpty = visibleTrashedPages.isEmpty() && visibleTrashedNotes.isEmpty()

    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var pageToDelete by remember { mutableStateOf<Page?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background).statusBarsPadding().navigationBarsPadding()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, bottom = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Trash",
                    style = TextStyle(
                        color = colors.pageTitle,
                        fontSize = 36.sp,
                        fontFamily = FontFamily(Font(Res.font.Merriweather_Light))
                    )
                )
            }
            if (!isEmpty) {
                Text(
                    text = "Empty",
                    style = TextStyle(color = colors.pageTitle.copy(alpha = 0.8f), fontWeight = FontWeight.Medium),
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                        .clickable { showEmptyTrashDialog = true }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        if (isEmpty) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Trash is empty",
                    color = colors.hint,
                    fontSize = 16.sp,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Pages section
                if (visibleTrashedPages.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pages",
                            color = colors.primaryText.copy(alpha = 0.5f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(start = 15.dp, end = 15.dp, bottom = 8.dp))
                    }
                    items(visibleTrashedPages, key = { "page_${it.id}" }) { page ->
                        TrashPageItem(
                            page = page,
                            onRestore = { viewModel.restorePage(page) },
                            onDelete = { pageToDelete = page }
                        )
                    }
                }

                // Tasks section
                if (visibleTrashedNotes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Tasks",
                            color = colors.primaryText.copy(alpha = 0.5f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                        )
                        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(start = 15.dp, end = 15.dp, bottom = 8.dp))
                    }
                    items(visibleTrashedNotes, key = { "note_${it.id}" }) { note ->
                        TrashNoteItem(
                            note = note,
                            onRestore = { viewModel.restoreNote(note) },
                            onDelete = { noteToDelete = note }
                        )
                    }
                }
            }
        }
    }

    // Empty trash confirmation dialog
    if (showEmptyTrashDialog) {
        ConfirmationDialog(
            title = "Empty trash",
            message = "Permanently delete all items in trash? This cannot be undone.",
            confirmText = "Delete all",
            onDismiss = { showEmptyTrashDialog = false },
            onConfirm = {
                viewModel.emptyTrash()
                showEmptyTrashDialog = false
            }
        )
    }

    // Permanent delete page confirmation
    if (pageToDelete != null) {
        ConfirmationDialog(
            title = "Delete permanently",
            message = "Permanently delete page '${pageToDelete!!.name}' and all its notes? This cannot be undone.",
            confirmText = "Delete",
            onDismiss = { pageToDelete = null },
            onConfirm = {
                viewModel.permanentlyDeletePage(pageToDelete!!)
                pageToDelete = null
            }
        )
    }

    // Permanent delete note confirmation
    if (noteToDelete != null) {
        ConfirmationDialog(
            title = "Delete permanently",
            message = "Permanently delete this task? This cannot be undone.",
            confirmText = "Delete",
            onDismiss = { noteToDelete = null },
            onConfirm = {
                viewModel.permanentlyDeleteNote(noteToDelete!!)
                noteToDelete = null
            }
        )
    }
}

@Composable
private fun TrashPageItem(
    page: Page,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMenu = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = page.name.take(20),
                    fontSize = 18.sp,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (page.parentId != null) {
                    Text(
                        text = "Sub-page",
                        fontSize = 12.sp,
                        color = colors.hint,
                    )
                }
            }
        }

        TrashActionsMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            onRestore = { onRestore(); showMenu = false },
            onDelete = { onDelete(); showMenu = false },
        )
    }
}

@Composable
private fun TrashNoteItem(
    note: Note,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMenu = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = note.text,
                fontSize = 18.sp,
                color = colors.primaryText,
                maxLines = if (showMenu) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        TrashActionsMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            onRestore = { onRestore(); showMenu = false },
            onDelete = { onDelete(); showMenu = false },
        )
    }
}

@Composable
private fun TrashActionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(
        modifier = Modifier.background(color = colors.menuBackground),
        expanded = expanded,
        offset = DpOffset(x = 40.dp, y = 0.dp),
        onDismissRequest = onDismissRequest,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onRestore)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Default.Restore,
                contentDescription = "Restore",
                tint = colors.primaryText
            )
            Text(
                text = "Restore",
                style = TextStyle(fontSize = 14.sp, color = colors.primaryText),
            )
        }
        Row(
            modifier = Modifier
                .clickable(onClick = onDelete)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Default.DeleteForever,
                contentDescription = "Delete",
                tint = colors.primaryText
            )
            Text(
                text = "Delete forever",
                style = TextStyle(fontSize = 14.sp, color = colors.primaryText),
            )
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
                        Text("Cancel", color = colors.primaryText)
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
