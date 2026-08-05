@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalTime::class, ExperimentalComposeUiApi::class)

package app.pentastic.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.pentastic.data.Note
import app.pentastic.data.PageType
import app.pentastic.navigation.getDeepLinkPageId
import app.pentastic.ui.composables.CommonInput
import app.pentastic.ui.composables.DueDateOptionsDialog
import app.pentastic.ui.composables.IndexPage
import app.pentastic.ui.composables.NotePage
import app.pentastic.ui.composables.TimelinePage
import app.pentastic.ui.theme.AppTheme
import app.pentastic.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime

@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToArchivedNotes: (Long) -> Unit = {},
    onNavigateToTimeline: () -> Unit = {},
    prefs: DataStore<Preferences> = koinInject(),
) {
    val viewModel = koinViewModel<MainViewModel>()

    val pages by viewModel.pages.collectAsState()
    val archivedPages by viewModel.archivedPages.collectAsState()
    val subPagesByParent by viewModel.subPagesByParent.collectAsState()
    val notesByPage by viewModel.notesByPage.collectAsState()
    val notesCountByPage by viewModel.notesCountByPage.collectAsState()
    val priorityNotesCountByPage by viewModel.priorityNotesCountByPage.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()
    val showRateButton by viewModel.showRateButton.collectAsState()
    val showCompletedTasks by viewModel.showCompletedTasks.collectAsState()
    val showTimeline by viewModel.showTimeline.collectAsState()
    // With Timeline enabled, pager index 1 is the Timeline page and real pages shift by one
    val timelineOffset = if (showTimeline) 1 else 0
    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { (pages.size + 1 + (if (showTimeline) 1 else 0)).coerceAtLeast(2) }
    )
    val coroutineScope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var selectedSubPageByParent by remember { mutableStateOf<Map<Long, Long?>>(emptyMap()) }
    var selectedWidePageIndex by remember { mutableIntStateOf(0) }
    var wideShowsTimeline by remember { mutableStateOf(false) }
    // Task text waiting for a due date before being added to the Timeline page
    var pendingTimelineTask by remember { mutableStateOf<String?>(null) }

    // Handle deep link navigation from notification
    val deepLinkPageId = getDeepLinkPageId()
    var hasNavigatedFromDeepLink by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.resetRepeatingTasksTodo()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppTheme.colors.background,
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isWideLayout = maxWidth >= 600.dp

            // Sync state when switching between layouts
            LaunchedEffect(isWideLayout) {
                if (isWideLayout) {
                    // Switching to wide: sync from pager
                    if (showTimeline && pagerState.currentPage == 1) {
                        wideShowsTimeline = true
                    } else if (pagerState.currentPage > timelineOffset) {
                        wideShowsTimeline = false
                        selectedWidePageIndex = pagerState.currentPage - 1 - timelineOffset
                    }
                } else {
                    // Switching to narrow: sync to pager
                    if (wideShowsTimeline && showTimeline) {
                        pagerState.scrollToPage(1)
                    } else if (selectedWidePageIndex >= 0 && selectedWidePageIndex < pages.size) {
                        pagerState.scrollToPage(selectedWidePageIndex + 1 + timelineOffset)
                    }
                }
            }

            // Navigate to the page from deep link (notification tap)
            LaunchedEffect(deepLinkPageId, pages, hasNavigatedFromDeepLink) {
                if (deepLinkPageId != null && pages.isNotEmpty() && !hasNavigatedFromDeepLink) {
                    val rootPageIndex = pages.indexOfFirst { it.id == deepLinkPageId }
                    if (rootPageIndex >= 0) {
                        if (isWideLayout) {
                            wideShowsTimeline = false
                            selectedWidePageIndex = rootPageIndex
                        } else {
                            pagerState.scrollToPage(rootPageIndex + 1 + timelineOffset)
                        }
                        hasNavigatedFromDeepLink = true
                    } else {
                        val parentPage = pages.find { parent ->
                            subPagesByParent[parent.id]?.any { it.id == deepLinkPageId } == true
                        }
                        if (parentPage != null) {
                            val parentIndex = pages.indexOf(parentPage)
                            selectedSubPageByParent = selectedSubPageByParent.toMutableMap().apply {
                                put(parentPage.id, deepLinkPageId)
                            }
                            if (isWideLayout) {
                                wideShowsTimeline = false
                                selectedWidePageIndex = parentIndex
                            } else {
                                pagerState.scrollToPage(parentIndex + 1 + timelineOffset)
                            }
                            hasNavigatedFromDeepLink = true
                        }
                    }
                }
            }

            LaunchedEffect(editingNote) {
                editingNote?.let { text = it.text }
            }

            // Helper to navigate to a page by ID
            fun navigateToPage(pageId: Long) {
                val rootPageIndex = pages.indexOfFirst { it.id == pageId }
                if (rootPageIndex >= 0) {
                    if (isWideLayout) {
                        wideShowsTimeline = false
                        selectedWidePageIndex = rootPageIndex
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(rootPageIndex + 1 + timelineOffset)
                        }
                    }
                } else {
                    val parentPage = pages.find { parent ->
                        subPagesByParent[parent.id]?.any { it.id == pageId } == true
                    }
                    if (parentPage != null) {
                        val parentIndex = pages.indexOf(parentPage)
                        selectedSubPageByParent = selectedSubPageByParent.toMutableMap().apply {
                            put(parentPage.id, pageId)
                        }
                        if (isWideLayout) {
                            wideShowsTimeline = false
                            selectedWidePageIndex = parentIndex
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(parentIndex + 1 + timelineOffset)
                            }
                        }
                    }
                }
            }

            // Helper to open the Timeline page
            fun openTimeline() {
                if (isWideLayout) {
                    wideShowsTimeline = true
                } else {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                }
            }

            // Determine current page for input bar logic
            val isOnTimelinePage = if (isWideLayout) {
                wideShowsTimeline && showTimeline
            } else {
                showTimeline && pagerState.currentPage == 1
            }
            val currentActivePage = if (isOnTimelinePage) {
                null
            } else if (isWideLayout) {
                pages.getOrNull(selectedWidePageIndex)
            } else {
                pages.getOrNull(pagerState.currentPage - 1 - timelineOffset)
            }
            val isOnIndexPage = !isWideLayout && pagerState.currentPage == 0

            if (isWideLayout) {
                // === WIDE LAYOUT: Side-by-side ===
                BackHandler(editingNote != null) {
                    viewModel.setEditingNote(null)
                    text = ""
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        IndexPage(
                            modifier = Modifier.width(300.dp).fillMaxHeight(),
                            pages = pages,
                            subPagesByParent = subPagesByParent,
                            notesCountByPage = notesCountByPage,
                            priorityNotesCountByPage = priorityNotesCountByPage,
                            showRateButton = showRateButton,
                            onPageClick = { pageId -> navigateToPage(pageId) },
                            onPageNameChange = { page, name -> viewModel.savePageName(page, name) },
                            onPageOrderChange = { updatedPages -> viewModel.updatePageOrder(updatedPages) },
                            onPageDelete = { page -> viewModel.deletePage(page) },
                            onPageArchive = { page -> viewModel.archivePage(page) },
                            onPageTypeChange = { page, type ->
                                viewModel.updatePageType(page, type)
                                navigateToPage(page.id)
                            },
                            onAddSubPage = { parentId, name -> viewModel.addSubPage(parentId, name) },
                            archivedPages = archivedPages,
                            onArchivedPageClick = { page -> onNavigateToArchivedNotes(page.id) },
                            onPageUnarchive = { page -> viewModel.unarchivePage(page) },
                            onNavigateToSettings = onNavigateToSettings,
                            showTimeline = showTimeline,
                            // Pinned: scroll to the pager page. Unpinned: push the
                            // standalone Timeline screen
                            onTimelineClick = { if (showTimeline) openTimeline() else onNavigateToTimeline() },
                        )

                        VerticalDivider(color = AppTheme.colors.divider)

                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Box(modifier = Modifier.weight(1f)) {
                                val widePage = pages.getOrNull(selectedWidePageIndex)
                                if (isOnTimelinePage) {
                                    TimelinePage()
                                } else if (widePage != null) {
                                    val subPages = subPagesByParent[widePage.id] ?: emptyList()
                                    val aggregatedNotes = aggregateNotes(widePage, subPages, notesByPage)
                                    val selectedSubPageId = selectedSubPageByParent[widePage.id]

                                    NotePage(
                                        notes = aggregatedNotes,
                                        notesByPage = notesByPage,
                                        onUpdateNote = { note -> viewModel.updateNote(note) },
                                        onDeleteNote = { note -> viewModel.deleteNote(note) },
                                        toggleNoteDone = { note ->
                                            viewModel.toggleNoteDone(note, PageType.fromOrdinal(widePage.pageType) == PageType.NOTES)
                                        },
                                        page = widePage,
                                        pageType = PageType.fromOrdinal(widePage.pageType),
                                        subPages = subPages,
                                        selectedSubPageId = selectedSubPageId,
                                        onSelectedSubPageChange = { subPageId ->
                                            selectedSubPageByParent = selectedSubPageByParent.toMutableMap().apply {
                                                put(widePage.id, subPageId)
                                            }
                                        },
                                        setEditingNote = { note -> viewModel.setEditingNote(note) },
                                        onSetRepeatFrequency = { note, frequency, startDate, reminderTime, reminderEnabled ->
                                            viewModel.setNoteRepeatFrequency(note, frequency, startDate, reminderTime, reminderEnabled)
                                        },
                                        onSetReminder = { note, reminderAt, enabled -> viewModel.setNoteReminder(note, reminderAt, enabled) },
                                        onRemoveReminder = { note -> viewModel.removeNoteReminder(note) },
                                        onSetDueDate = { note, dueStartAt, dueEndAt -> viewModel.setNoteDueDate(note, dueStartAt, dueEndAt) },
                                        allPages = pages,
                                        allSubPagesByParent = subPagesByParent,
                                        onMoveNote = { note, targetPageId -> viewModel.moveNoteToPage(note, targetPageId) },
                                        showCompletedTasks = showCompletedTasks,
                                        onToggleShowCompleted = { viewModel.toggleShowCompletedTasks() },
                                        onDeleteCompletedTasks = { tasks -> viewModel.moveCompletedTasksToTrash(tasks) },
                                    )
                                }
                            }

                            CommonInput(
                                modifier = Modifier.navigationBarsPadding().imePadding(),
                                text = text,
                                onTextChange = { text = it },
                                onActionClick = {
                                    val note = editingNote
                                    if (note != null) {
                                        viewModel.updateNote(note.copy(text = text.trim()))
                                        text = ""
                                    } else if (isOnTimelinePage) {
                                        if (text.isNotBlank()) pendingTimelineTask = text.trim()
                                    } else {
                                        currentActivePage?.let { page ->
                                            val targetPageId = selectedSubPageByParent[page.id] ?: page.id
                                            viewModel.insertNote(targetPageId, text.trim())
                                        }
                                        text = ""
                                    }
                                },
                                isEditing = editingNote != null,
                                placeholder = when {
                                    editingNote != null -> ""
                                    isOnTimelinePage -> "Add a task..."
                                    currentActivePage != null && PageType.fromOrdinal(currentActivePage.pageType) == PageType.NOTES -> "Add a note..."
                                    currentActivePage != null -> "Add a task..."
                                    else -> ""
                                },
                                showPriorityButton = currentActivePage != null && PageType.fromOrdinal(currentActivePage.pageType) == PageType.TASKS,
                                onPriorityActionClick = {
                                    currentActivePage?.let { page ->
                                        val targetPageId = selectedSubPageByParent[page.id] ?: page.id
                                        viewModel.insertPriorityNote(targetPageId, text.trim())
                                    }
                                    text = ""
                                }
                            )
                        }
                    }
                }
            } else {
                // === NARROW LAYOUT: HorizontalPager (existing behavior) ===
                BackHandler(pagerState.currentPage > 0) {
                    coroutineScope.launch {
                        if (editingNote != null) {
                            viewModel.setEditingNote(null)
                            text = ""
                        } else
                            pagerState.animateScrollToPage(0)
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { pageIndex ->
                        val pageOffset = pagerState.getOffsetDistanceInPages(pageIndex)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationX = (if (pageOffset < 0f) 0f else -pageOffset * size.width)
                                    val scale = lerp(1f, 0.9f, pageOffset.coerceIn(0f, 1f))
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = lerp(1f, 0.5f, pageOffset.coerceIn(0f, 1f))
                                }
                                .zIndex(if (pageOffset < 0f) 1f else 0f)
                        ) {
                            if (pageIndex == 0)
                                IndexPage(
                                    pages = pages,
                                    subPagesByParent = subPagesByParent,
                                    notesCountByPage = notesCountByPage,
                                    priorityNotesCountByPage = priorityNotesCountByPage,
                                    showRateButton = showRateButton,
                                    onPageClick = { pageId -> navigateToPage(pageId) },
                                    onPageNameChange = { page, name -> viewModel.savePageName(page, name) },
                                    onPageOrderChange = { updatedPages -> viewModel.updatePageOrder(updatedPages) },
                                    onPageDelete = { page -> viewModel.deletePage(page) },
                                    onPageArchive = { page -> viewModel.archivePage(page) },
                                    onPageTypeChange = { page, type ->
                                        viewModel.updatePageType(page, type)
                                        navigateToPage(page.id)
                                    },
                                    onAddSubPage = { parentId, name -> viewModel.addSubPage(parentId, name) },
                                            archivedPages = archivedPages,
                                    onArchivedPageClick = { page -> onNavigateToArchivedNotes(page.id) },
                                    onPageUnarchive = { page -> viewModel.unarchivePage(page) },
                                    onNavigateToSettings = onNavigateToSettings,
                                    showTimeline = showTimeline,
                                    onTimelineClick = { if (showTimeline) openTimeline() else onNavigateToTimeline() },
                                )
                            else if (showTimeline && pageIndex == 1) {
                                TimelinePage()
                            } else {
                                val currentPage = pages.getOrNull(pageIndex - 1 - timelineOffset)
                                if (currentPage != null) {
                                    val subPages = subPagesByParent[currentPage.id] ?: emptyList()
                                    val aggregatedNotes = aggregateNotes(currentPage, subPages, notesByPage)
                                    val selectedSubPageId = selectedSubPageByParent[currentPage.id]

                                    NotePage(
                                        notes = aggregatedNotes,
                                        notesByPage = notesByPage,
                                        onUpdateNote = { note -> viewModel.updateNote(note) },
                                        onDeleteNote = { note -> viewModel.deleteNote(note) },
                                        toggleNoteDone = { note ->
                                            viewModel.toggleNoteDone(note, PageType.fromOrdinal(currentPage.pageType) == PageType.NOTES)
                                        },
                                        page = currentPage,
                                        pageType = PageType.fromOrdinal(currentPage.pageType),
                                        subPages = subPages,
                                        selectedSubPageId = selectedSubPageId,
                                        onSelectedSubPageChange = { subPageId ->
                                            selectedSubPageByParent = selectedSubPageByParent.toMutableMap().apply {
                                                put(currentPage.id, subPageId)
                                            }
                                        },
                                        setEditingNote = { note -> viewModel.setEditingNote(note) },
                                        onSetRepeatFrequency = { note, frequency, startDate, reminderTime, reminderEnabled ->
                                            viewModel.setNoteRepeatFrequency(note, frequency, startDate, reminderTime, reminderEnabled)
                                        },
                                        onSetReminder = { note, reminderAt, enabled -> viewModel.setNoteReminder(note, reminderAt, enabled) },
                                        onRemoveReminder = { note -> viewModel.removeNoteReminder(note) },
                                        onSetDueDate = { note, dueStartAt, dueEndAt -> viewModel.setNoteDueDate(note, dueStartAt, dueEndAt) },
                                        allPages = pages,
                                        allSubPagesByParent = subPagesByParent,
                                        onMoveNote = { note, targetPageId -> viewModel.moveNoteToPage(note, targetPageId) },
                                        showCompletedTasks = showCompletedTasks,
                                        onToggleShowCompleted = { viewModel.toggleShowCompletedTasks() },
                                        onDeleteCompletedTasks = { tasks -> viewModel.moveCompletedTasksToTrash(tasks) },
                                    )
                                }
                            }
                        }
                    }

                    CommonInput(
                        modifier = Modifier.navigationBarsPadding().imePadding(),
                        text = text,
                        onTextChange = { text = it },
                        onActionClick = {
                            val note = editingNote
                            if (note != null) {
                                viewModel.updateNote(note.copy(text = text.trim()))
                                text = ""
                            } else if (isOnTimelinePage) {
                                if (text.isNotBlank()) pendingTimelineTask = text.trim()
                            } else {
                                if (isOnIndexPage)
                                    viewModel.addPage(text.trim())
                                else
                                    currentActivePage?.let { page ->
                                        val targetPageId = selectedSubPageByParent[page.id] ?: page.id
                                        viewModel.insertNote(targetPageId, text.trim())
                                    }
                                text = ""
                            }
                        },
                        isEditing = editingNote != null,
                        placeholder = when {
                            editingNote != null -> ""
                            isOnTimelinePage -> "Add a task..."
                            isOnIndexPage -> "Add a new page..."
                            else -> {
                                if (currentActivePage != null && PageType.fromOrdinal(currentActivePage.pageType) == PageType.NOTES)
                                    "Add a note..."
                                else
                                    "Add a task..."
                            }
                        },
                        showPriorityButton = !isOnIndexPage && currentActivePage != null && PageType.fromOrdinal(currentActivePage.pageType) == PageType.TASKS,
                        onPriorityActionClick = {
                            currentActivePage?.let { page ->
                                val targetPageId = selectedSubPageByParent[page.id] ?: page.id
                                viewModel.insertPriorityNote(targetPageId, text.trim())
                            }
                            text = ""
                        },
                        maxLength = if (isOnIndexPage && editingNote == null) 20 else 1000,
                    )
                }
            }

            // Ask for a due date before adding a task from the Timeline input
            if (pendingTimelineTask != null) {
                DueDateOptionsDialog(
                    currentDueStartAt = 0L,
                    currentDueEndAt = 0L,
                    onDismiss = { pendingTimelineTask = null },
                    onApply = { dueStartAt, dueEndAt ->
                        viewModel.addTimelineTask(pendingTimelineTask!!, dueStartAt, dueEndAt)
                        pendingTimelineTask = null
                        text = ""
                    }
                )
            }
        }
    }
}

private fun aggregateNotes(
    page: app.pentastic.data.Page,
    subPages: List<app.pentastic.data.Page>,
    notesByPage: Map<Long, List<Note>>,
): List<Note> {
    val isNotesType = PageType.fromOrdinal(page.pageType) == PageType.NOTES
    return if (subPages.isNotEmpty()) {
        val parentNotes = notesByPage[page.id] ?: emptyList()
        val subPageNotes = subPages.flatMap { notesByPage[it.id] ?: emptyList() }
        if (isNotesType) {
            (parentNotes + subPageNotes).sortedByDescending { it.createdAt }
        } else {
            (parentNotes + subPageNotes).sortedWith(
                compareBy<Note> { it.done }
                    .thenByDescending { if (!it.done) it.priority else 0 }
                    .thenByDescending { it.orderAt }
            )
        }
    } else {
        val pageNotes = notesByPage[page.id] ?: emptyList()
        if (isNotesType) pageNotes.sortedByDescending { it.createdAt } else pageNotes
    }
}
