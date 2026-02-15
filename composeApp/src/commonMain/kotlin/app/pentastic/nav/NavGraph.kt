package app.pentastic.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.pentastic.ui.screen.ArchivedNotesScreen
import app.pentastic.ui.screen.HomeScreen
import app.pentastic.ui.screen.SettingsScreen
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
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToArchivedNotes = { pageId ->
                    navController.navigate(Screen.ArchivedNotes.createRoute(pageId))
                }
            )
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateToTrash = {
                    navController.navigate(Screen.Trash.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
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
        composable(
            route = Screen.ArchivedNotes.route,
            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
            ArchivedNotesScreen(
                pageId = pageId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}