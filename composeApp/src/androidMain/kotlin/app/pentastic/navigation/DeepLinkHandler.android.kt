package app.pentastic.navigation

import androidx.compose.runtime.Composable
import app.pentastic.LocalNavigateToPageId

@Composable
actual fun getDeepLinkPageId(): Long? {
    return LocalNavigateToPageId.current
}
