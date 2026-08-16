package app.pentastic.widget

import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import app.pentastic.data.MyRepository
import app.pentastic.data.NoteActions
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Double-tap-to-complete, matching the gesture the task list uses in-app.
 *
 * RemoteViews has no double-tap gesture — a widget only ever sees single clicks — so
 * this is a two-step state machine: the first tap arms the row (persisted per widget
 * and rendered as a tinted row), and a second tap on the *same* row inside
 * [DOUBLE_TAP_WINDOW_MS] completes it. Tapping a different row re-arms instead.
 *
 * Must stay a top-level public class: Glance instantiates it reflectively by name,
 * and the consumer ProGuard rule that survives R8 is
 * `-keep public class * extends androidx.glance.appwidget.action.ActionCallback`.
 */
class ToggleNoteDoneAction : ActionCallback, KoinComponent {

    private val repository: MyRepository by inject()
    private val noteActions: NoteActions by inject()

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val uuid = parameters[NOTE_UUID_KEY] ?: return
        // Elapsed realtime, not wall clock: immune to the user or the network moving
        // the clock between the two taps
        val now = SystemClock.elapsedRealtime()

        val state = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val armedUuid = state[ARMED_UUID_KEY]
        val armedAt = state[ARMED_AT_KEY] ?: 0L
        val isSecondTap = armedUuid == uuid && now - armedAt in 0..DOUBLE_TAP_WINDOW_MS

        if (isSecondTap) {
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs.remove(ARMED_UUID_KEY)
                prefs.remove(ARMED_AT_KEY)
            }
            val note = repository.getNoteByUuid(uuid) ?: return
            noteActions.toggleDone(note, isNotesType = parameters[IS_NOTES_TYPE_KEY] == true)
        } else {
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[ARMED_UUID_KEY] = uuid
                prefs[ARMED_AT_KEY] = now
            }
        }

        // Push explicitly rather than waiting on WidgetRefreshObserver's debounce: the
        // process can be torn down as soon as this returns, and the arm step changes
        // no data so the observer would never fire for it
        WidgetUpdater.updateAll(context)
    }

    companion object {
        val NOTE_UUID_KEY = ActionParameters.Key<String>("note_uuid")
        val IS_NOTES_TYPE_KEY = ActionParameters.Key<Boolean>("is_notes_type")

        val ARMED_UUID_KEY = stringPreferencesKey("armed_uuid")
        val ARMED_AT_KEY = longPreferencesKey("armed_at")

        /**
         * Generous next to the platform's 300ms double-tap timeout: each tap is a
         * broadcast plus a widget update round trip, so a native-feeling window would
         * be unhittable here.
         */
        const val DOUBLE_TAP_WINDOW_MS = 2_000L
    }
}
