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
    val noOfPage = 11
    val viewModel = koinViewModel<MainViewModel>()

//    val currentPage by viewModel.currentPage.collectAsState()
    // val allNotes by viewModel.allNotes.collectAsState()
    val notesByPage by viewModel.notesByPage.collectAsState()
    val notesCountByPage by viewModel.notesCountByPage.collectAsState()
    val priorityNotesCountByPage by viewModel.priorityNotesCountByPage.collectAsState()
    val pageNames by viewModel.pageNames.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { noOfPage })
    val coroutineScope = rememberCoroutineScope()

    BackHandler(pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

//    LaunchedEffect(pagerState.currentPage) {
//        viewModel.saveCurrentPage(pagerState.currentPage)
//    }

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
                        noOfPage,
                        notesCountByPage,
                        priorityNotesCountByPage,
                        pageNames,
                        onPageClick = { page ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        },
                        onPageNameChange = { page, name ->
                            viewModel.savePageNames(page, name)
                        }
                    )
                else
                    NotePage(
                        notes = notesByPage[pageIndex] ?: emptyList(),
                        onInsertNote = { page, text -> viewModel.insertNote(page, text) },
                        onUpdateNote = { note -> viewModel.updateNote(note) },
                        onDeleteNote = { note -> viewModel.deleteNote(note) },
                        toggleNoteDone = { note -> viewModel.toggleNoteDone(note) },
                        pageIndex = pageIndex,
                        pageNames = pageNames,
                    )
            }
        }
    }
}
