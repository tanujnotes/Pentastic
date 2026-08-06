@file:OptIn(ExperimentalTime::class)

package app.pentastic.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object DatastoreKeys {
    val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    val FIRST_LAUNCH_TIME = longPreferencesKey("first_launch_time")

    val SHOW_RATE_BUTTON = booleanPreferencesKey("show_rate_button")
    val THEME_MODE = intPreferencesKey("theme_mode")
    val SHOW_COMPLETED_TASKS = booleanPreferencesKey("show_completed_tasks")
    val SHOW_TIMELINE = booleanPreferencesKey("show_timeline")
    val SHOW_SUB_PAGES = booleanPreferencesKey("show_sub_pages")
}

class DataStoreRepository(private val dataStore: DataStore<Preferences>) {

    val firstLaunch: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DatastoreKeys.FIRST_LAUNCH] ?: true
    }

    suspend fun firstLaunchDone() {
        dataStore.edit { settings ->
            settings[DatastoreKeys.FIRST_LAUNCH] = false
        }
    }

    val firstLaunchTime: Flow<Long> = dataStore.data.map { preferences ->
        preferences[DatastoreKeys.FIRST_LAUNCH_TIME] ?: run {
            dataStore.edit { settings ->
                settings[DatastoreKeys.FIRST_LAUNCH_TIME] = Clock.System.now().toEpochMilliseconds()
            }
            Clock.System.now().toEpochMilliseconds()
        }
    }

    suspend fun setFirstLaunchTime(firstLaunchTime: Long) {
        dataStore.edit { settings ->
            settings[DatastoreKeys.FIRST_LAUNCH_TIME] = firstLaunchTime
        }
    }

    val showRateButton: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DatastoreKeys.SHOW_RATE_BUTTON] ?: true
    }

    suspend fun rateButtonClicked() {
        dataStore.edit { settings ->
            settings[DatastoreKeys.SHOW_RATE_BUTTON] = false
        }
    }

    val themeMode: Flow<Int> = dataStore.data.map { preferences ->
        preferences[DatastoreKeys.THEME_MODE] ?: ThemeMode.DAY_NIGHT.ordinal
    }

    suspend fun saveThemeMode(themeMode: Int) {
        dataStore.edit { settings ->
            settings[DatastoreKeys.THEME_MODE] = themeMode
        }
    }

    val showCompletedTasks: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DatastoreKeys.SHOW_COMPLETED_TASKS] ?: true
    }

    suspend fun setShowCompletedTasks(show: Boolean) {
        dataStore.edit { settings ->
            settings[DatastoreKeys.SHOW_COMPLETED_TASKS] = show
        }
    }

    val showTimeline: Flow<Boolean> = dataStore.data.map { preferences ->
        // No stored choice means the user has never seen the toggle. Fresh installs get the
        // timeline; upgrades don't, so an existing list doesn't rearrange itself on update.
        // FIRST_LAUNCH is only ever written once a version has already run.
        preferences[DatastoreKeys.SHOW_TIMELINE] ?: (preferences[DatastoreKeys.FIRST_LAUNCH] == null)
    }

    val showTimelineChosen: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DatastoreKeys.SHOW_TIMELINE] != null
    }

    suspend fun setShowTimeline(show: Boolean) {
        dataStore.edit { settings ->
            settings[DatastoreKeys.SHOW_TIMELINE] = show
        }
    }

    val showSubPages: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DatastoreKeys.SHOW_SUB_PAGES] ?: true
    }

    suspend fun setShowSubPages(show: Boolean) {
        dataStore.edit { settings ->
            settings[DatastoreKeys.SHOW_SUB_PAGES] = show
        }
    }
}