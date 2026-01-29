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
}