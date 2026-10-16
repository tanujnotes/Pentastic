package app.pentastic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import app.pentastic.data.AppCalendar
import app.pentastic.data.AppLanguage
import app.pentastic.data.WeekStart
import app.pentastic.data.DataStoreRepository
import app.pentastic.data.ThemeMode
import app.pentastic.nav.SetupNavGraph
import app.pentastic.ui.screen.WelcomeScreen
import app.pentastic.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    val dataStoreRepository = koinInject<DataStoreRepository>()
    val coroutineScope = rememberCoroutineScope()

    // First run: ask for the language before anything else renders or seeds.
    // null while the preference is still loading, so nothing flashes early.
    var showWelcome by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        showWelcome = dataStoreRepository.firstLaunch.first()
    }

    when (showWelcome) {
        null -> Unit
        true -> WelcomeScreen(
            onDone = { language, calendar ->
                showWelcome = false
                coroutineScope.launch {
                    dataStoreRepository.saveLanguage(language.ordinal)
                    dataStoreRepository.saveCalendar(calendar.ordinal)
                    dataStoreRepository.firstLaunchDone()
                }
            }
        )
        else -> PentasticApp(dataStoreRepository)
    }
}

@Composable
private fun PentasticApp(dataStoreRepository: DataStoreRepository) {
    val themeOrdinal by dataStoreRepository.themeMode.collectAsState(initial = ThemeMode.DAY_NIGHT.ordinal)
    val themeMode = ThemeMode.fromOrdinal(themeOrdinal)

    val languageOrdinal by dataStoreRepository.language.collectAsState(initial = AppLanguage.FARSI.ordinal)
    val language = AppLanguage.fromOrdinal(languageOrdinal)

    val calendarOrdinal by dataStoreRepository.calendar.collectAsState(initial = AppCalendar.GREGORIAN.ordinal)
    val calendar = AppCalendar.fromOrdinal(calendarOrdinal)

    // Unset preference falls back by language: Saturday for Farsi, Monday otherwise
    val weekStartPref by dataStoreRepository.weekStart.collectAsState(initial = null)
    val weekStart = weekStartPref?.let { WeekStart.fromOrdinal(it) }
        ?: if (language == AppLanguage.FARSI) WeekStart.SATURDAY else WeekStart.MONDAY

    AppTheme(themeMode = themeMode, language = language, calendar = calendar, weekStart = weekStart) {
        val navController = rememberNavController()
        SetupNavGraph(navController = navController)
    }
}
