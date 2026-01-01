package app.pentastic.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object DatastoreKeys {
    val FIRST_LAUNCH = booleanPreferencesKey("first_launch")

    val TOTAL_PAGES = intPreferencesKey("total_pages")
    val CURRENT_PAGE = intPreferencesKey("current_page")
    val PAGE_NAMES_MAP = stringPreferencesKey("page_names_map")
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

    val totalPages: Flow<Int> = dataStore.data.map { preferences ->
        preferences[DatastoreKeys.TOTAL_PAGES] ?: 11
    }

    suspend fun saveTotalPages(page: Int) {
        dataStore.edit { settings ->
            settings[DatastoreKeys.TOTAL_PAGES] = page
        }
    }

    val currentPage: Flow<Int> = dataStore.data.map { preferences ->
        preferences[DatastoreKeys.CURRENT_PAGE] ?: 0
    }

    suspend fun saveCurrentPage(page: Int) {
        dataStore.edit { settings ->
            settings[DatastoreKeys.CURRENT_PAGE] = page
        }
    }

    val pageNames: Flow<Map<Int, String>> = dataStore.data.map { preferences ->
        val jsonString = preferences[DatastoreKeys.PAGE_NAMES_MAP]
        if (jsonString != null)
            Json.decodeFromString(
                MapSerializer(
                    Int.serializer(),
                    String.serializer()
                ), jsonString
            )
        else
            emptyMap()
    }

    suspend fun savePageNames(names: Map<Int, String>) {
        dataStore.edit { preferences ->
            preferences[DatastoreKeys.PAGE_NAMES_MAP] = Json.encodeToString(names)
        }
    }
}