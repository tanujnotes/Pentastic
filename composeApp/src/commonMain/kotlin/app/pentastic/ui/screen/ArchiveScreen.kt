@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)

package app.pentastic.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pentastic.data.Page
import app.pentastic.ui.theme.AppTheme
import app.pentastic.ui.theme.AppTheme.colors
import app.pentastic.ui.viewmodel.MainViewModel
import org.jetbrains.compose.resources.Font
import org.koin.compose.viewmodel.koinViewModel
import pentastic.composeapp.generated.resources.Merriweather_Light
import pentastic.composeapp.generated.resources.Res
import kotlin.time.ExperimentalTime

@Composable
fun ArchiveScreen(onNavigateBack: () -> Unit) {
    val viewModel = koinViewModel<MainViewModel>()
    val archivedPages by viewModel.archivedPages.collectAsState()

    val isEmpty = archivedPages.isEmpty()

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background).statusBarsPadding().navigationBarsPadding()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, bottom = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Archive",
                style = TextStyle(
                    color = colors.pageTitle,
                    fontSize = 36.sp,
                    fontFamily = FontFamily(Font(Res.font.Merriweather_Light))
                )
            )
        }

        Spacer(Modifier.height(4.dp))

        if (isEmpty) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Archive is empty",
                    color = colors.hint,
                    fontSize = 16.sp,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(archivedPages, key = { "archive_${it.id}" }) { page ->
                    ArchivePageItem(
                        page = page,
                        onUnarchive = { viewModel.unarchivePage(page) },
                        onMoveToTrash = { viewModel.deletePage(page) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchivePageItem(
    page: Page,
    onUnarchive: () -> Unit,
    onMoveToTrash: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMenu = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = page.name.take(20),
                fontSize = 18.sp,
                color = colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(
            modifier = Modifier.background(color = colors.menuBackground),
            expanded = showMenu,
            offset = DpOffset(x = 40.dp, y = 0.dp),
            onDismissRequest = { showMenu = false },
        ) {
            Row(
                modifier = Modifier
                    .clickable(onClick = { onUnarchive(); showMenu = false })
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Default.Unarchive,
                    contentDescription = "Unarchive",
                    tint = colors.primaryText
                )
                Text(
                    text = "Unarchive",
                    style = TextStyle(fontSize = 14.sp, color = colors.primaryText),
                )
            }
            Row(
                modifier = Modifier
                    .clickable(onClick = { onMoveToTrash(); showMenu = false })
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Move to trash",
                    tint = colors.primaryText
                )
                Text(
                    text = "Move to trash",
                    style = TextStyle(fontSize = 14.sp, color = colors.primaryText),
                )
            }
        }
    }
}
