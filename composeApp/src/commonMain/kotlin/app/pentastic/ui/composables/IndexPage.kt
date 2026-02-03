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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pentastic.data.Page
import app.pentastic.data.ThemeMode
import app.pentastic.ui.theme.AppTheme.colors
import app.pentastic.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import pentastic.composeapp.generated.resources.Merriweather_Light
import pentastic.composeapp.generated.resources.Res
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun IndexPage(
    pages: List<Page>,
    subPagesByParent: Map<Long, List<Page>>,
    notesCountByPage: Map<Long, Int>,
    priorityNotesCountByPage: Map<Long, Int>,
    showRateButton: Boolean,
    onPageClick: (Long) -> Unit,
    onPageNameChange: (Page, String) -> Unit,
    onPageOrderChange: (List<Page>) -> Unit,
    onPageDelete: (Page) -> Unit,
    onAddSubPage: (Long, String) -> Unit,
) {
    val viewModel = koinViewModel<MainViewModel>()

    var showRenameDialog by remember { mutableStateOf(false) }
    var pageToRename: Page? by remember { mutableStateOf(null) }
    var pageToRenameIndexLabel by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pageToDelete: Page? by remember { mutableStateOf(null) }
    var showAddSubPageDialog by remember { mutableStateOf(false) }
    var parentPageForSubPage: Page? by remember { mutableStateOf(null) }

    var localPages by remember { mutableStateOf(pages.filter { it.id != 0L }) }
    val uriHandler = LocalUriHandler.current
    var showTopMenu by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

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
                    color = colors.pageTitle,
                    fontSize = 36.sp,
                    fontFamily = FontFamily(Font(Res.font.Merriweather_Light))
                )
            )
            if (isReorderMode) {
                Text(
                    text = "Done",
                    style = TextStyle(color = colors.primaryText, fontWeight = FontWeight.Medium),
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
                                tint = colors.hint
                            )
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false },
                            modifier = Modifier.background(color = colors.menuBackground),
                            offset = DpOffset(x = (-12).dp, y = (-4).dp),
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rate", color = colors.primaryText) },
                                onClick = {
                                    showTopMenu = false
                                    viewModel.onRateClicked()
                                    uriHandler.openUri("https://play.google.com/store/apps/details?id=app.pentastic")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share", color = colors.primaryText) },
                                onClick = {
                                    showTopMenu = false
                                    coroutineScope.launch {
                                        clipboardManager.setText(AnnotatedString("Minimal Todo Lists - Pentastic!\nhttps://play.google.com/store/apps/details?id=app.pentastic"))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Follow", color = colors.primaryText) },
                                onClick = {
                                    showTopMenu = false
                                    uriHandler.openUri("https://x.com/tanujnotes")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Theme", color = colors.primaryText) },
                                onClick = {
                                    showTopMenu = false
                                    showThemeDialog = true
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
                                .background(if (isDragging) colors.dragging else Color.Transparent)
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
                                    fontSize = 16.sp,
                                    lineHeight = 20.sp,
                                    fontFamily = FontFamily(Font(Res.font.Merriweather_Light)),
                                    color = colors.primaryText.copy(alpha = 0.33f),
                                    modifier = Modifier.padding(top = 1.dp).defaultMinSize(minWidth = 32.dp)
                                )

                                Text(
                                    text = page.name.take(20),
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    color = colors.primaryText,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                Spacer(Modifier.width(6.dp))

                                if (!isReorderMode) {
                                    Text(
                                        text = "................................................................................................................... ",
                                        color = colors.hint,
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
                                        color = if ((priorityNotesCountByPage[page.id] ?: 0) > 0) colors.priorityText
                                        else if ((notesCountByPage[page.id] ?: 0) > 0) colors.icon
                                        else colors.hint,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Reorder",
                                        tint = colors.hint,
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
                                offset = DpOffset(x = 80.dp, y = 0.dp),
                                modifier = Modifier.background(color = colors.menuBackground),
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename", color = colors.primaryText) },
                                    onClick = {
                                        showMenu = false
                                        pageToRename = page
                                        pageToRenameIndexLabel = "${index + 1}."
                                        showRenameDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, tint = colors.icon, contentDescription = null) }
                                )
                                if (page.parentId == null) {
                                    DropdownMenuItem(
                                        text = { Text("Add sub-page", color = colors.primaryText) },
                                        onClick = {
                                            showMenu = false
                                            parentPageForSubPage = page
                                            showAddSubPageDialog = true
                                        },
                                        leadingIcon = { Icon(Icons.Default.Add, tint = colors.icon, contentDescription = null) }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Reorder", color = colors.primaryText) },
                                    onClick = {
                                        showMenu = false
                                        isReorderMode = true
                                    },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, tint = colors.icon, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", color = colors.primaryText) },
                                    onClick = {
                                        showMenu = false
                                        pageToDelete = page
                                        showDeleteDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, tint = colors.icon, contentDescription = null) }
                                )
                            }
                        }
                    }

                    // Sub-pages for this parent
                    val subPages = subPagesByParent[page.id] ?: emptyList()
                    subPages.forEachIndexed { subIndex, subPage ->
                        SubPageItem(
                            subPage = subPage,
                            parentIndex = index + 1,
                            subIndex = subIndex + 1,
                            notesCount = notesCountByPage[subPage.id] ?: 0,
                            priorityNotesCount = priorityNotesCountByPage[subPage.id] ?: 0,
                            onPageClick = onPageClick,
                            onRename = {
                                pageToRename = subPage
                                pageToRenameIndexLabel = "${index + 1}.${subIndex + 1}"
                                showRenameDialog = true
                            },
                            onDelete = {
                                pageToDelete = subPage
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }

            if (showRenameDialog && pageToRename != null) {
                EditPageNameDialog(
                    page = pageToRename!!,
                    indexLabel = pageToRenameIndexLabel,
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

            if (showThemeDialog) {
                ThemeSelectionDialog(
                    currentTheme = viewModel.themeMode.value,
                    onDismiss = { showThemeDialog = false },
                    onConfirm = { selectedTheme ->
                        viewModel.setThemeMode(selectedTheme)
                        showThemeDialog = false
                    }
                )
            }

            if (showAddSubPageDialog && parentPageForSubPage != null) {
                AddSubPageDialog(
                    parentPage = parentPageForSubPage!!,
                    onDismiss = { showAddSubPageDialog = false },
                    onConfirm = { subPageName ->
                        onAddSubPage(parentPageForSubPage!!.id, subPageName.ifBlank { "Sub-page" })
                        showAddSubPageDialog = false
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
                            colors = listOf(colors.background, Color.Transparent)
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
                            colors = listOf(Color.Transparent, colors.background)
                        )
                    ).align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center
            ) {}
        }
    }
}

@Composable
private fun SubPageItem(
    subPage: Page,
    parentIndex: Int,
    subIndex: Int,
    notesCount: Int,
    priorityNotesCount: Int,
    onPageClick: (Long) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, top = 8.dp, bottom = 8.dp, end = 16.dp)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onPageClick(subPage.id) },
                    onLongClick = { showMenu = true }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$parentIndex.$subIndex",
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontFamily = FontFamily(Font(Res.font.Merriweather_Light)),
                color = colors.primaryText.copy(alpha = 0.33f),
                modifier = Modifier.defaultMinSize(minWidth = 32.dp)
            )
            Text(
                text = subPage.name.take(20),
                fontSize = 16.sp,
                maxLines = 1,
                color = colors.primaryText.copy(alpha = 0.8f),
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "................................................................................................................... ",
                color = colors.hint,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                modifier = Modifier.defaultMinSize(minWidth = 16.dp),
                text = (if (priorityNotesCount > 0) priorityNotesCount else notesCount).toString(),
                fontSize = 16.sp,
                color = if (priorityNotesCount > 0) colors.priorityText
                else if (notesCount > 0) colors.icon
                else colors.hint,
                textAlign = TextAlign.Center
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(x = 80.dp, y = 0.dp),
            modifier = Modifier.background(color = colors.menuBackground),
        ) {
            DropdownMenuItem(
                text = { Text("Rename", color = colors.primaryText) },
                onClick = {
                    showMenu = false
                    onRename()
                },
                leadingIcon = { Icon(Icons.Default.Edit, tint = colors.icon, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = colors.primaryText) },
                onClick = {
                    showMenu = false
                    onDelete()
                },
                leadingIcon = { Icon(Icons.Default.Delete, tint = colors.icon, contentDescription = null) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPageNameDialog(
    page: Page,
    indexLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(page.name) }
    val colors = colors

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.menuBackground,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Edit page name", color = colors.primaryText, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = text.take(20),
                    onValueChange = { if (it.length <= 20) text = it },
                    label = { Text("Page $indexLabel") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = colors.primaryText)
                    }
                    Button(onClick = { onConfirm(text) }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletePageConfirmationDialog(
    page: Page,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = colors

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.menuBackground,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Delete page", color = colors.primaryText, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                Text("Are you sure you want to delete page '${page.name}' and all its notes?", color = colors.primaryText)
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = colors.primaryText)
                    }
                    Button(onClick = onConfirm) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubPageDialog(
    parentPage: Page,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val colors = colors

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.menuBackground,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Add sub-page", color = colors.primaryText, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text("Parent: ${parentPage.name}", color = colors.hint, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = text.take(20),
                    onValueChange = { if (it.length <= 20) text = it },
                    label = { Text("Sub-page name") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = colors.primaryText)
                    }
                    Button(onClick = { onConfirm(text) }) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionDialog(
    currentTheme: ThemeMode,
    onDismiss: () -> Unit,
    onConfirm: (ThemeMode) -> Unit,
) {
    var selectedTheme by remember { mutableStateOf(currentTheme) }
    val colors = colors

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.menuBackground,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Theme", color = colors.primaryText, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                ThemeMode.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTheme = theme }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = selectedTheme == theme,
                            onClick = { selectedTheme = theme }
                        )
                        Text(text = theme.label, color = colors.primaryText)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = colors.primaryText)
                    }
                    TextButton(onClick = { onConfirm(selectedTheme) }) {
                        Text("Save", color = colors.primaryText)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun IndexPagePreview() {
    Surface(color = Color.White) {
        IndexPage(
            pages = listOf(Page(1, name = "Default"), Page(2, name = "Todo later"), Page(5, name = "Pro Launcher")),
            subPagesByParent = mapOf(1L to listOf(Page(10, name = "Sub 1", parentId = 1), Page(11, name = "Sub 2", parentId = 1))),
            notesCountByPage = mapOf(
                1L to 5,
                2L to 3,
                5L to 18,
                8L to 1,
                10L to 12
            ),
            priorityNotesCountByPage = mapOf(1L to 4),
            showRateButton = false,
            onPageClick = {},
            onPageNameChange = { _, _ -> },
            onPageOrderChange = {},
            onPageDelete = {},
            onAddSubPage = { _, _ -> },
        )
    }
}