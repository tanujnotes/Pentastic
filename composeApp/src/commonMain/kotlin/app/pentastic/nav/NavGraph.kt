package app.pentastic.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.pentastic.ui.screen.HomeScreen

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
            HomeScreen()

//            HomeScreen(
//                navigateToSettings = {
//                    navController.navigate(Screen.Settings.route)
//                }
//            )
        }
    }
}