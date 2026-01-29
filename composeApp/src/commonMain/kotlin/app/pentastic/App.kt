package app.pentastic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import app.pentastic.data.DataStoreRepository
import app.pentastic.data.ThemeMode
import app.pentastic.nav.SetupNavGraph
import app.pentastic.ui.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    val dataStoreRepository = koinInject<DataStoreRepository>()
    val themeOrdinal by dataStoreRepository.themeMode.collectAsState(initial = ThemeMode.DAY_NIGHT.ordinal)
    val themeMode = ThemeMode.fromOrdinal(themeOrdinal)

    AppTheme(themeMode = themeMode) {
        val navController = rememberNavController()
        SetupNavGraph(navController = navController)
    }
}