@file:OptIn(ExperimentalComposeUiApi::class)

package app.pentastic.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import app.pentastic.ui.composables.CommonInput
import app.pentastic.ui.composables.DueDateOptionsDialog
import app.pentastic.ui.composables.TimelinePage
import app.pentastic.ui.theme.AppTheme
import app.pentastic.ui.viewmodel.MainViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Standalone Timeline destination for when the timeline is unpinned from the home
 * pager: the index row pushes this screen instead of scrolling to a pager page,
 * so timeline tasks stay reachable in every configuration.
 */
@Composable
fun TimelineScreen() {
    val viewModel = koinViewModel<MainViewModel>()
    val editingNote by viewModel.editingNote.collectAsState()
    var text by remember { mutableStateOf("") }
    // Task text waiting for a due date before being added to the Timeline page
    var pendingTimelineTask by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editingNote) {
        editingNote?.let { text = it.text }
    }
    // Back while editing cancels the edit; the next back pops the screen
    BackHandler(enabled = editingNote != null) {
        viewModel.setEditingNote(null)
        text = ""
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppTheme.colors.background,
        // Only the top inset is reserved here. The bottom belongs to CommonInput, which
        // pads it itself so the bar reaches the screen edge
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top),
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TimelinePage(modifier = Modifier.weight(1f))

            CommonInput(
                text = text,
                onTextChange = { text = it },
                onActionClick = {
                    val note = editingNote
                    if (note != null) {
                        viewModel.updateNote(note.copy(text = text.trim()))
                        text = ""
                    } else if (text.isNotBlank()) {
                        pendingTimelineTask = text.trim()
                    }
                },
                isEditing = editingNote != null,
                placeholder = if (editingNote != null) "" else "Add a task...",
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
