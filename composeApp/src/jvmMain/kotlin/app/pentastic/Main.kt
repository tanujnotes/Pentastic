package app.pentastic

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.pentastic.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(
                width = 1000.dp,
                height = 720.dp,
                position = WindowPosition(Alignment.Center)
            ),
            title = "Pentastic"
        ) {
            App()
        }
    }
}
