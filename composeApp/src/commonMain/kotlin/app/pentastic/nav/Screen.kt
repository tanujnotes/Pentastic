package app.pentastic.nav

sealed class Screen(val route: String) {
    data object Home : Screen(route = "home")
    data object Settings : Screen(route = "settings")
}