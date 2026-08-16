@file:OptIn(ExperimentalTime::class)

package app.pentastic.widget

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import app.pentastic.data.DataStoreRepository
import app.pentastic.data.MyRepository
import app.pentastic.data.PageType
import app.pentastic.data.ThemeMode
import kotlinx.coroutines.flow.flowOf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.ExperimentalTime

/**
 * One chosen page's pending tasks. The page is picked at placement time by
 * [PageWidgetConfigActivity] and stored per widget id in Glance state, which also
 * means the store is cleaned up for us when the widget is removed.
 *
 * Everything is resolved *inside* [provideContent]. Glance's `update()` recomposes a
 * live session's content lambda rather than re-running [provideGlance], so anything
 * read before that lambda is captured once and never refreshed — which silently
 * pinned the widget to whichever page it was first configured with.
 */
class PageWidget : GlanceAppWidget(), KoinComponent {

    private val repository: MyRepository by inject()
    private val dataStoreRepository: DataStoreRepository by inject()

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val pageId = prefs[KEY_PAGE_ID]

            val themeOrdinal by dataStoreRepository.themeMode
                .collectAsState(initial = ThemeMode.DAY_NIGHT.ordinal)

            val page by remember(pageId) {
                if (pageId == null) flowOf(null) else repository.getPageByIdFlow(pageId)
            }.collectAsState(initial = null)

            val notes by remember(pageId) {
                if (pageId == null) flowOf(emptyList()) else repository.getAllNotesByPage(pageId)
            }.collectAsState(initial = emptyList())

            val livePage = page?.takeIf { it.deletedAt == 0L && it.archivedAt == 0L }
            val isNotesType = livePage != null &&
                    PageType.fromOrdinal(livePage.pageType) == PageType.NOTES

            val rows = remember(notes, livePage?.id, isNotesType) {
                if (livePage == null) emptyList()
                else notes.filter { !it.done }
                    .mapIndexed { index, note ->
                        WidgetRow.Task(note.toWidgetTask(isNotesType), index + 1)
                    }
            }

            val armedUuid = prefs[ToggleNoteDoneAction.ARMED_UUID_KEY]?.takeIf {
                SystemClock.elapsedRealtime() - (prefs[ToggleNoteDoneAction.ARMED_AT_KEY] ?: 0L) in
                        0..ToggleNoteDoneAction.DOUBLE_TAP_WINDOW_MS
            }

            val reconfigureAction = actionStartActivity(
                PageWidgetConfigActivity.reconfigureIntent(context, id)
            )

            TaskListWidgetContent(
                title = livePage?.name,
                rows = rows,
                colors = widgetColors(context, ThemeMode.fromOrdinal(themeOrdinal)),
                isDark = isWidgetDark(context, ThemeMode.fromOrdinal(themeOrdinal)),
                // Distinguish "never configured" from "the page is gone" — and stay
                // blank while the first query is still in flight, rather than flashing
                // the reconfigure prompt
                emptyMessage = when {
                    pageId == null -> "Tap to choose a page"
                    page == null -> ""
                    livePage == null -> "Page unavailable — tap to choose another"
                    else -> "Nothing left here"
                },
                openAppAction = if (livePage == null) reconfigureAction
                else actionStartActivity(openAppIntent(context, livePage.id)),
                armedUuid = armedUuid,
                rowAction = { task -> toggleDoneAction(task) },
            )
        }
    }

    companion object {
        val KEY_PAGE_ID = longPreferencesKey("page_id")
    }
}
