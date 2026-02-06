@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalTime::class, ExperimentalComposeUiApi::class)

package app.pentastic.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.pentastic.data.Note
import app.pentastic.navigation.getDeepLinkPageId
import app.pentastic.ui.composables.CommonInput
import app.pentastic.ui.composables.IndexPage
import app.pentastic.ui.composables.NotePage
import app.pentastic.ui.theme.AppTheme
import app.pentastic.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime

@Composable
fun HomeScreen(prefs: DataStore<Preferences> = koinInject()) {
    val viewModel = koinViewModel<MainViewModel>()

    val pages by viewModel.pages.collectAsState()
    val subPagesByParent by viewModel.subPagesByParent.collectAsState()
    val notesByPage by viewModel.notesByPage.collectAsState()
    val notesCountByPage by viewModel.notesCountByPage.collectAsState()
    val priorityNotesCountByPage by viewModel.priorityNotesCountByPage.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()
    val showRateButton by viewModel.showRateButton.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { (pages.size + 1).coerceAtLeast(2) }
    )
    val coroutineScope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var selectedSubPageByParent by remember { mutableStateOf<Map<Long, Long?>>(emptyMap()) }

    // Handle deep link navigation from notification
    val deepLinkPageId = getDeepLinkPageId()
    var hasNavigatedFromDeepLink by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.resetRepeatingTasksTodo()
    }

    // Navigate to the page from deep link (notification tap)
    LaunchedEffect(deepLinkPageId, pages, hasNavigatedFromDeepLink) {
        if (deepLinkPageId != null && pages.isNotEmpty() && !hasNavigatedFromDeepLink) {
            // Find if it's a root page
            val rootPageIndex = pages.indexOfFirst { it.id == deepLinkPageId }
            if (rootPageIndex >= 0) {
                pagerState.scrollToPage(rootPageIndex + 1)
                hasNavigatedFromDeepLink = true
            } else {
                // Check if it's a sub-page
                val parentPage = pages.find { parent ->
                    subPagesByParent[parent.id]?.any { it.id == deepLinkPageId } == true
                }
                if (parentPage != null) {
                    val parentIndex = pages.indexOf(parentPage)
                    selectedSubPageByParent = selectedSubPageByParent.toMutableMap().apply {
                        put(parentPage.id, deepLinkPageId)
                    }
                    pagerState.scrollToPage(parentIndex + 1)
                    hasNavigatedFromDeepLink = true
                }
            }
        }
    }

    LaunchedEffect(editingNote) {
        editingNote?.let { text = it.text }
    }

    BackHandler(pagerState.currentPage > 0) {
        coroutineScope.launch {
            if (editingNote != null) {
                viewModel.setEditingNote(null)
                text = ""
            } else
                pagerState.animateScrollToPage(0)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppTheme.colors.background,
        bottomBar = {
            CommonInput(
                modifier = Modifier.navigationBarsPadding().imePadding(),
                text = text,
                onTextChange = { text = it },
                onActionClick = {
                    val note = editingNote
                    if (note != null)
                        viewModel.updateNote(note.copy(text = text.trim()))
                    else {
                        if (pagerState.currentPage == 0)
                            viewModel.addPage(text.trim())
                        else
                            pages.getOrNull(pagerState.currentPage - 1)?.let { page ->
                                val targetPageId = selectedSubPageByParent[page.id] ?: page.id
                                viewModel.insertNote(targetPageId, text.trim())
                            }
                    }
                    text = ""
                },
                isEditing = editingNote != null
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HorizontalPager(state = pagerState) { pageIndex ->
                if (pageIndex == 0)
                    IndexPage(
                        pages = pages,
                        subPagesByParent = subPagesByParent,
                        notesCountByPage = notesCountByPage,
                        priorityNotesCountByPage = priorityNotesCountByPage,
                        showRateButton = showRateButton,
                        onPageClick = { pageId ->
                            val rootPageIndex = pages.indexOfFirst { it.id == pageId }
                            if (rootPageIndex >= 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(rootPageIndex + 1)
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
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(parentIndex + 1)
                                    }
                                }
                            }
                        },
                        onPageNameChange = { page, name ->
                            viewModel.savePageName(page, name)
                        },
                        onPageOrderChange = { updatedPages ->
                            viewModel.updatePageOrder(updatedPages)
                        },
                        onPageDelete = { page -> viewModel.deletePage(page) },
                        onAddSubPage = { parentId, name -> viewModel.addSubPage(parentId, name) }
                    )
                else {
                    val currentPage = pages.getOrNull(pageIndex - 1)
                    if (currentPage != null) {
                        val subPages = subPagesByParent[currentPage.id] ?: emptyList()

                        val aggregatedNotes = if (subPages.isNotEmpty()) {
                            val parentNotes = notesByPage[currentPage.id] ?: emptyList()
                            val subPageNotes = subPages.flatMap { notesByPage[it.id] ?: emptyList() }
                            (parentNotes + subPageNotes).sortedWith(
                                compareBy<Note> { it.done }
                                    .thenByDescending { if (!it.done) it.priority else 0 }
                                    .thenByDescending { it.orderAt }
                            )
                        } else {
                            notesByPage[currentPage.id] ?: emptyList()
                        }

                        val selectedSubPageId = selectedSubPageByParent[currentPage.id]

                        NotePage(
                            notes = aggregatedNotes,
                            notesByPage = notesByPage,
                            onUpdateNote = { note -> viewModel.updateNote(note) },
                            onDeleteNote = { note -> viewModel.deleteNote(note) },
                            toggleNoteDone = { note -> viewModel.toggleNoteDone(note) },
                            page = currentPage,
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
                        )
                    }
                }
            }
        }
    }
}
