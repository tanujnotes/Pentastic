@file:OptIn(FlowPreview::class)

package app.pentastic.widget

import android.content.Context
import app.pentastic.data.MyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Repaints widgets when app-side edits change the data.
 *
 * A live Glance session already recomposes on its own, but sessions only exist while
 * the widget is visible and the process alive. This covers the rest: the user edits a
 * task in the app, and the widget behind it is correct when they go back to the home
 * screen.
 *
 * Maps to a cheap identity first so unrelated column changes (a drag reorder, an
 * edited body) don't trigger a repaint the widget wouldn't render differently.
 */
object WidgetRefreshObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start(context: Context, repository: MyRepository) {
        scope.launch {
            repository.liveNotesFlow()
                .map { notes ->
                    notes.map { "${it.id}:${it.done}:${it.priority}:${it.text}:${it.dueEndAt}:${it.repeatFrequency}" }
                }
                .distinctUntilChanged()
                // The first emission is just the current state — nothing has changed yet
                .drop(1)
                .debounce(400)
                .collect { WidgetUpdater.updateAll(context.applicationContext) }
        }
    }
}
