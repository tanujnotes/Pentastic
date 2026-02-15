package app.pentastic.nav

sealed class Screen(val route: String) {
    data object Home : Screen(route = "home")
    data object Settings : Screen(route = "settings")
    data object Trash : Screen(route = "trash")
    data object Archive : Screen(route = "archive")
    data object ArchivedNotes : Screen(route = "archived_notes/{pageId}") {
        fun createRoute(pageId: Long) = "archived_notes/$pageId"
    }
}