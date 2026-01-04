@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalTime::class, ExperimentalComposeUiApi::class)

package app.pentastic.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.pentastic.ui.composables.IndexPage
import app.pentastic.ui.composables.NotePage
import app.pentastic.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime

@Composable
fun HomeScreen(prefs: DataStore<Preferences> = koinInject()) {
    val viewModel = koinViewModel<MainViewModel>()

    val pages by viewModel.pages.collectAsState()
    val notesByPage by viewModel.notesByPage.collectAsState()
    val notesCountByPage by viewModel.notesCountByPage.collectAsState()
    val priorityNotesCountByPage by viewModel.priorityNotesCountByPage.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { (pages.size + 1).coerceAtLeast(2) }
    )
    val coroutineScope = rememberCoroutineScope()

    BackHandler(pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(), containerColor = Color(0xFFF9FBFF)
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
                        notesCountByPage = notesCountByPage,
                        priorityNotesCountByPage = priorityNotesCountByPage,
                        onPageClick = { pageId ->
                            val targetIndex = pages.indexOfFirst { it.id == pageId } + 1
                            if (targetIndex > 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(targetIndex)
                                }
                            }
                        },
                        onPageNameChange = { page, name ->
                            viewModel.savePageName(page, name)
                        },
                        onPageOrderChange = { updatedPages ->
                            viewModel.updatePageOrder(updatedPages)
                        }
                    )
                else {
                    val currentPage = pages.getOrNull(pageIndex - 1) // pages start with id 1
                    if (currentPage != null) {
                        NotePage(
                            notes = notesByPage[currentPage.id] ?: emptyList(),
                            onInsertNote = { pageId, text -> viewModel.insertNote(pageId, text) },
                            onUpdateNote = { note -> viewModel.updateNote(note) },
                            onDeleteNote = { note -> viewModel.deleteNote(note) },
                            toggleNoteDone = { note -> viewModel.toggleNoteDone(note) },
                            page = currentPage,
                        )
                    }
                }
            }
        }
    }
}
