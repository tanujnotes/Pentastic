package app.pentastic.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pentastic.ui.composables.ThemeSelectionDialog
import app.pentastic.ui.theme.AppTheme
import app.pentastic.ui.theme.AppTheme.colors
import app.pentastic.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font
import org.koin.compose.viewmodel.koinViewModel
import pentastic.composeapp.generated.resources.Merriweather_Light
import pentastic.composeapp.generated.resources.Res

@Composable
fun SettingsScreen(
    onNavigateToTrash: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<MainViewModel>()
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val themeMode by viewModel.themeMode.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Title
        Text(
            text = "Settings",
            style = TextStyle(
                color = colors.pageTitle,
                fontSize = 36.sp,
                fontFamily = FontFamily(Font(Res.font.Merriweather_Light))
            ),
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)
        )

        Spacer(Modifier.height(12.dp))

        SettingsItem(
            icon = { Icon(Icons.Default.Palette, contentDescription = null, tint = colors.icon, modifier = Modifier.size(22.dp)) },
            title = "Theme",
            onClick = { showThemeDialog = true },
            trailing = {
                Text(
                    text = themeMode.label,
                    fontSize = 14.sp,
                    color = colors.hint,
                )
            }
        )

        SettingsItem(
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = colors.icon, modifier = Modifier.size(22.dp)) },
            title = "Trash",
            onClick = onNavigateToTrash,
            trailing = {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.hint,
                    modifier = Modifier.size(22.dp)
                )
            }
        )

        HorizontalDivider(color = colors.divider.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))

        SettingsItem(
            icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = colors.icon, modifier = Modifier.size(22.dp)) },
            title = "Follow",
            onClick = {
                uriHandler.openUri("https://x.com/tanujnotes")
            }
        )
        SettingsItem(
            icon = { Icon(Icons.Default.Share, contentDescription = null, tint = colors.icon, modifier = Modifier.size(22.dp)) },
            title = "Share",
            onClick = {
                coroutineScope.launch {
                    clipboardManager.setText(AnnotatedString("Get things done with Pentastic!\nhttps://play.google.com/store/apps/details?id=app.pentastic"))
                }
            }
        )
        SettingsItem(
            icon = { Icon(Icons.Default.Star, contentDescription = null, tint = colors.icon, modifier = Modifier.size(22.dp)) },
            title = "Rate",
            onClick = {
                viewModel.onRateClicked()
                uriHandler.openUri("https://play.google.com/store/apps/details?id=app.pentastic")
            }
        )

    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = viewModel.themeMode.value,
            onDismiss = { showThemeDialog = false },
            onConfirm = { selectedTheme ->
                viewModel.setThemeMode(selectedTheme)
                showThemeDialog = false
            }
        )
    }
}

@Composable
private fun SettingsItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = colors.primaryText,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            trailing()
        }
    }
}
