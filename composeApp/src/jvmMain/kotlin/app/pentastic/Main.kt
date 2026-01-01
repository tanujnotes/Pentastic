package app.pentastic

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.pentastic.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = {},
            title = "Pentastic"
        ) {
            App()
        }
    }
}