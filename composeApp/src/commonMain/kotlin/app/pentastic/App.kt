package app.pentastic

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.pentastic.data.DataStoreRepository
import app.pentastic.data.ThemeMode
import app.pentastic.nav.Screen
import app.pentastic.nav.SetupNavGraph
import app.pentastic.ui.composables.AppBottomBar
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
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val tabRoutes = remember { setOf(Screen.Home.route, Screen.Timeline.route, Screen.Settings.route) }

        Scaffold(
            containerColor = AppTheme.colors.background,
            // Zero insets: each screen keeps handling its own status/navigation bar padding
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if ((currentRoute ?: Screen.Home.route) in tabRoutes) {
                    AppBottomBar(
                        currentRoute = currentRoute,
                        onTabSelected = { route ->
                            if (route != currentRoute) {
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            SetupNavGraph(
                navController = navController,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            )
        }
    }
}
