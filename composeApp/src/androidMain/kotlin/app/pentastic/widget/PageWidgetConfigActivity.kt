package app.pentastic.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import app.pentastic.data.DataStoreRepository
import app.pentastic.data.MyRepository
import app.pentastic.data.Page
import app.pentastic.data.ThemeMode
import app.pentastic.ui.theme.AppTheme
import app.pentastic.ui.theme.interTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Placement-time page picker for [PageWidget]. Also serves reconfigure (the provider
 * is marked `reconfigurable`), since both cases are the same interaction.
 */
class PageWidgetConfigActivity : ComponentActivity(), KoinComponent {

    private val repository: MyRepository by inject()
    private val dataStoreRepository: DataStoreRepository by inject()

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Set first, so a back press cancels the placement rather than leaving a
        // half-configured widget on the home screen
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        // This Activity has to be exported for the launcher to start it, so reject
        // ids belonging to anyone else's provider
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)?.provider
        if (provider != null && provider.packageName != packageName) {
            finish()
            return
        }

        setContent {
            val themeOrdinal by dataStoreRepository.themeMode.collectAsState(initial = ThemeMode.DAY_NIGHT.ordinal)
            AppTheme(themeMode = ThemeMode.fromOrdinal(themeOrdinal)) {
                PagePicker(repository = repository, onPick = ::commit)
            }
        }
    }

    /**
     * Deliberately not on lifecycleScope: finish() cancels it, and the repaint has to
     * outlive this Activity. The write must also land before the launcher binds the
     * widget (which is what setResult/finish triggers), otherwise the resulting
     * APPWIDGET_UPDATE races it and renders the unconfigured state — so the order here
     * is write, then hand back, then force a render regardless of who won.
     */
    private fun commit(pageId: Long) {
        val widgetId = appWidgetId
        val appContext = applicationContext
        commitScope.launch {
            val glanceId = GlanceAppWidgetManager(appContext).getGlanceIdBy(widgetId)
            updateAppWidgetState(appContext, glanceId) { prefs ->
                prefs[PageWidget.KEY_PAGE_ID] = pageId
            }
            withContext(Dispatchers.Main) {
                setResult(
                    RESULT_OK,
                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
                )
                finish()
            }
            PageWidget().update(appContext, glanceId)
        }
    }

    companion object {
        /** Outlives the Activity so the widget still repaints after finish(). */
        private val commitScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        /**
         * Distinct action per widget id: PendingIntent matches on Intent.filterEquals,
         * which ignores extras, so without it every placed widget would reconfigure
         * whichever one was created first.
         */
        fun reconfigureIntent(context: Context, glanceId: androidx.glance.GlanceId): Intent {
            val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
            return Intent(context, PageWidgetConfigActivity::class.java).apply {
                action = "app.pentastic.WIDGET_RECONFIGURE_$widgetId"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
        }
    }
}

@Composable
private fun PagePicker(repository: MyRepository, onPick: (Long) -> Unit) {
    val colors = AppTheme.colors
    val rootPages by repository.getRootPages().collectAsState(initial = emptyList())
    val allPages by repository.getAllPages().collectAsState(initial = emptyList())
    val timelinePage by repository.getTimelinePage().collectAsState(initial = null)

    val entries = remember(rootPages, allPages, timelinePage) {
        val subPagesByParent = allPages.filter { it.parentId != null }.groupBy { it.parentId!! }
        buildList {
            timelinePage?.let { add(it to false) }
            rootPages.forEach { root ->
                add(root to false)
                subPagesByParent[root.id].orEmpty()
                    .sortedByDescending { it.orderAt }
                    .forEach { add(it to true) }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Choose a page",
            modifier = Modifier.padding(top = 48.dp, bottom = 16.dp),
            color = colors.pageTitle,
            fontSize = 20.sp,
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(entries, key = { it.first.id }) { (page, isSubPage) ->
                Text(
                    text = page.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(page.id) }
                        .padding(start = if (isSubPage) 20.dp else 0.dp, top = 14.dp, bottom = 14.dp),
                    color = colors.primaryText,
                    fontSize = 17.sp,
                )
            }
        }
    }
}
