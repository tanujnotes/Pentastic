package app.pentastic.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.pentastic.ui.screen.HomeScreen
import app.pentastic.ui.screen.TrashScreen

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToTrash = {
                    navController.navigate(Screen.Trash.route)
                }
            )
        }
        composable(route = Screen.Trash.route) {
            TrashScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}