package app.pentastic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import app.pentastic.nav.SetupNavGraph
import app.pentastic.ui.theme.AppTheme
import app.pentastic.ui.viewmodel.MainViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App() {
    val viewModel = koinViewModel<MainViewModel>()
    val themeMode by viewModel.themeMode.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.resetRepeatingTasksTodo()
    }

    AppTheme(themeMode = themeMode) {
        val navController = rememberNavController()
        SetupNavGraph(navController = navController)
    }
}