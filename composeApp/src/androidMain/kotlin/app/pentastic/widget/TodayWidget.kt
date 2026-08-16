@file:OptIn(ExperimentalTime::class)

package app.pentastic.widget

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import app.pentastic.MainActivity
import app.pentastic.data.DataStoreRepository
import app.pentastic.data.MyRepository
import app.pentastic.data.ThemeMode
import app.pentastic.data.TimelineSection
import app.pentastic.data.notesBySection
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The near-term timeline: Overdue, Today and Tomorrow's pending tasks, grouped the
 * way the in-app Timeline groups them.
 *
 * Runs in the app process, so Koin resolves the repository exactly as the
 * notification receivers do. The note flow is collected inside [provideContent] so
 * edits made in the app repaint the widget while a session is alive;
 * [WidgetRefreshObserver] covers the case where the session has died.
 */
class TodayWidget : GlanceAppWidget(), KoinComponent {

    private val repository: MyRepository by inject()
    private val dataStoreRepository: DataStoreRepository by inject()

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val notesFlow = repository.liveNotesFlow()
        val timelinePageId = repository.getTimelinePageOnce()?.id

        provideContent {
            val notes by notesFlow.collectAsState(initial = emptyList())
            // Collected inside the content lambda, not before it: update() recomposes
            // this lambda without re-running provideGlance, so anything captured
            // outside would never see a theme change
            val themeOrdinal by dataStoreRepository.themeMode
                .collectAsState(initial = ThemeMode.DAY_NIGHT.ordinal)
            val prefs = currentState<Preferences>()
            val timeZone = remember { TimeZone.currentSystemDefault() }
            // Captured once per composition; WidgetRefreshScheduler restarts the
            // session at midnight so the buckets roll over
            val today = remember { Clock.System.now().toLocalDateTime(timeZone).date }

            val rows = remember(notes, today) {
                buildRows(
                    notesBySection(notes, WIDGET_SECTIONS, today, timeZone)
                        .map { (section, note) -> section to note.toWidgetTask() }
                )
            }

            // Only honour an arm that is still inside the double-tap window, so a row
            // left armed hours ago is not still highlighted
            val armedUuid = prefs[ToggleNoteDoneAction.ARMED_UUID_KEY]?.takeIf {
                SystemClock.elapsedRealtime() - (prefs[ToggleNoteDoneAction.ARMED_AT_KEY] ?: 0L) in
                        0..ToggleNoteDoneAction.DOUBLE_TAP_WINDOW_MS
            }

            TaskListWidgetContent(
                rows = rows,
                colors = widgetColors(context, ThemeMode.fromOrdinal(themeOrdinal)),
                isDark = isWidgetDark(context, ThemeMode.fromOrdinal(themeOrdinal)),
                emptyMessage = "Nothing due",
                openAppAction = actionStartActivity(openAppIntent(context, timelinePageId)),
                armedUuid = armedUuid,
                rowAction = { task -> toggleDoneAction(task) },
            )
        }
    }

    private companion object {
        val WIDGET_SECTIONS = setOf(
            TimelineSection.OVERDUE,
            TimelineSection.TODAY,
            TimelineSection.TOMORROW,
        )
    }
}

/**
 * Flattens section-tagged tasks into heading + numbered task rows. Numbering restarts
 * per section, matching how each section is numbered in the app.
 */
internal fun buildRows(tagged: List<Pair<TimelineSection, WidgetTask>>): List<WidgetRow> =
    buildList {
        var currentSection: TimelineSection? = null
        var number = 0
        tagged.forEach { (section, task) ->
            if (section != currentSection) {
                add(WidgetRow.Heading(section.label))
                currentSection = section
                number = 0
            }
            number++
            add(WidgetRow.Task(task, number))
        }
    }

/**
 * Reuses the `navigate_to_page_id` extra the reminder notifications already use, so
 * MainActivity's existing deep-link handling resolves it to the right pager index.
 */
internal fun openAppIntent(context: Context, pageId: Long?): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        pageId?.let { putExtra("navigate_to_page_id", it) }
        // Distinct per page: PendingIntent matches on filterEquals, which ignores
        // extras, so two widgets pointing at different pages would otherwise share one
        action = "app.pentastic.WIDGET_OPEN_${pageId ?: 0}"
    }

internal fun toggleDoneAction(task: WidgetTask): Action = actionRunCallback<ToggleNoteDoneAction>(
    actionParametersOf(
        ToggleNoteDoneAction.NOTE_UUID_KEY to task.uuid,
        ToggleNoteDoneAction.IS_NOTES_TYPE_KEY to task.isNotesType,
    )
)
