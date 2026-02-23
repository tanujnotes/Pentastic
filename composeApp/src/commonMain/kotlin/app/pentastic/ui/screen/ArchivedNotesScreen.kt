@file:OptIn(ExperimentalTime::class)

package app.pentastic.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.pentastic.data.Page
import app.pentastic.data.PageType
import app.pentastic.data.Note
import app.pentastic.ui.composables.NotePage
import app.pentastic.ui.theme.AppTheme
import app.pentastic.ui.viewmodel.MainViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime

@Composable
fun ArchivedNotesScreen(
    pageId: Long,
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<MainViewModel>()
    val notesByPage by viewModel.notesByPage.collectAsState()
    val subPages by viewModel.getArchivedSubPages(pageId).collectAsState(initial = emptyList())
    var page by remember { mutableStateOf<Page?>(null) }
    var selectedSubPageId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(pageId) {
        page = viewModel.getPageById(pageId)
    }

    val currentPage = page ?: return

    val displayedPageId = selectedSubPageId ?: pageId
    val notes = notesByPage[displayedPageId] ?: emptyList()

    val isNotesType = PageType.fromOrdinal(currentPage.pageType) == PageType.NOTES

    // Build aggregated notes like HomeScreen does for pages with subpages
    val aggregatedNotes = if (subPages.isNotEmpty()) {
        val parentNotes = notesByPage[pageId] ?: emptyList()
        val subPageNotes = subPages.flatMap { notesByPage[it.id] ?: emptyList() }
        if (isNotesType) {
            (parentNotes + subPageNotes).sortedBy { it.createdAt }
        } else {
            (parentNotes + subPageNotes).sortedWith(
                compareBy<Note> { it.done }
                    .thenByDescending { if (!it.done) it.priority else 0 }
                    .thenByDescending { it.orderAt }
            )
        }
    } else {
        val pageNotes = notesByPage[pageId] ?: emptyList()
        if (isNotesType) pageNotes.sortedBy { it.createdAt } else pageNotes
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        NotePage(
            notes = aggregatedNotes,
            notesByPage = notesByPage,
            onUpdateNote = { note -> viewModel.updateNote(note) },
            onDeleteNote = { note -> viewModel.deleteNote(note) },
            toggleNoteDone = { note -> viewModel.toggleNoteDone(note, isNotesType) },
            page = currentPage,
            pageType = PageType.fromOrdinal(currentPage.pageType),
            subPages = subPages,
            selectedSubPageId = selectedSubPageId,
            onSelectedSubPageChange = { subPageId -> selectedSubPageId = subPageId },
            setEditingNote = { note -> viewModel.setEditingNote(note) },
            onSetRepeatFrequency = { note, frequency, startDate, reminderTime, reminderEnabled ->
                viewModel.setNoteRepeatFrequency(note, frequency, startDate, reminderTime, reminderEnabled)
            },
            onSetReminder = { note, reminderAt, enabled -> viewModel.setNoteReminder(note, reminderAt, enabled) },
            onRemoveReminder = { note -> viewModel.removeNoteReminder(note) },
        )
    }
}
