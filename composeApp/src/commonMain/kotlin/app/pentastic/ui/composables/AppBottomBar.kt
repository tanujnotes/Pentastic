package app.pentastic.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChecklistRtl
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewTimeline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pentastic.nav.Screen
import app.pentastic.ui.theme.AppTheme

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun AppBottomBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
) {
    val colors = AppTheme.colors
    val tabs = remember {
        listOf(
            BottomTab(Screen.Home.route, "Task", Icons.Outlined.ChecklistRtl),
            BottomTab(Screen.Timeline.route, "Timeline", Icons.Outlined.ViewTimeline),
            BottomTab(Screen.Settings.route, "Settings", Icons.Outlined.Settings),
        )
    }

    // Background sits on the outer Column so the bar color fills the gesture-nav area
    Column(modifier = Modifier.fillMaxWidth().background(colors.background)) {
        HorizontalDivider(color = colors.divider.copy(alpha = 0.7f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(60.dp)
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                val tint = if (selected) colors.primaryText else colors.hint
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab.route) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(text = tab.label, fontSize = 11.sp, color = tint)
                }
            }
        }
    }
}
