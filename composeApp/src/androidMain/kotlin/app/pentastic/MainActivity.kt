package app.pentastic

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal to provide navigation target page ID from notification deep links
 */
val LocalNavigateToPageId = staticCompositionLocalOf<Long?> { null }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT, Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT, Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)

        val navigateToPageId = getNavigateToPageId(intent)

        setContent {
            CompositionLocalProvider(LocalNavigateToPageId provides navigateToPageId) {
                App()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle new intent when app is already running
        val navigateToPageId = getNavigateToPageId(intent)
        if (navigateToPageId != null && navigateToPageId != -1L) {
            setContent {
                CompositionLocalProvider(LocalNavigateToPageId provides navigateToPageId) {
                    App()
                }
            }
        }
    }

    private fun getNavigateToPageId(intent: Intent?): Long? {
        val pageId = intent?.getLongExtra("navigate_to_page_id", -1L)
        return if (pageId != null && pageId != -1L) pageId else null
    }
}
