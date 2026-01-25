package app.pentastic

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import app.pentastic.nav.SetupNavGraph
import app.pentastic.ui.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    AppTheme {
        val navController = rememberNavController()
        SetupNavGraph(navController = navController)
    }
}