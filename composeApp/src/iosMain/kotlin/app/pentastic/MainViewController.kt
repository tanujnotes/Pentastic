package app.pentastic

import androidx.compose.ui.window.ComposeUIViewController
import app.pentastic.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) { App() }