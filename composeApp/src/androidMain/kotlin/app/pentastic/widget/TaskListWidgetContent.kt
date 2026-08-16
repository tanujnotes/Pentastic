package app.pentastic.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.pentastic.R
import app.pentastic.ui.theme.AppColors

/**
 * A widget row: either a section heading or a task under it.
 */
sealed interface WidgetRow {
    data class Heading(val label: String) : WidgetRow
    data class Task(val task: WidgetTask, val number: Int) : WidgetRow
}

/**
 * The one list renderer both widgets share. Everything here is Glance — a stray
 * `androidx.compose.material3` or `androidx.compose.foundation` import compiles
 * fine and then dies at runtime casting the Applier, so keep this file's imports
 * confined to androidx.glance (plus the unit/graphics types Glance itself uses).
 */
@Composable
fun TaskListWidgetContent(
    rows: List<WidgetRow>,
    colors: AppColors,
    emptyMessage: String,
    openAppAction: Action,
    isDark: Boolean,
    // Null for the Today widget, which labels its content with section headings
    // instead; the Page widget uses it for the page name
    title: String? = null,
    armedUuid: String? = null,
    rowAction: ((WidgetTask) -> Action)? = null,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            // A drawable rather than a plain colour + cornerRadius: the latter compiles
            // to setViewOutlinePreferredRadius, which is API 31+, so 26-30 would get
            // square corners against the home screen
            .background(
                ImageProvider(if (isDark) R.drawable.widget_bg_dark else R.drawable.widget_bg_light)
            )
            .cornerRadius(16.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (title != null) {
            Text(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
                    .noRippleClickable(openAppAction),
                text = title,
                style = TextStyle(
                    color = ColorProvider(colors.pageTitle),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
        }

        if (rows.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize().noRippleClickable(openAppAction),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emptyMessage,
                    style = TextStyle(
                        color = ColorProvider(colors.hint),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 3,
                )
            }
        } else {
            // LazyColumn, not Column: a Column serialises every row into one RemoteViews
            // and a long list trips the ~1MB Binder transaction cap
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                itemsIndexed(rows, itemId = { _, row -> row.stableId() }) { _, row ->
                    when (row) {
                        is WidgetRow.Heading -> SectionHeading(row.label, colors, openAppAction)
                        is WidgetRow.Task -> TaskRow(
                            task = row.task,
                            number = row.number,
                            colors = colors,
                            isArmed = row.task.uuid == armedUuid,
                            action = rowAction?.invoke(row.task) ?: openAppAction,
                        )
                    }
                }
            }
        }
    }
}

/**
 * LazyColumn item ids must be stable and distinct. Note ids are small autoincrement
 * values, so headings are keyed into a negative range that cannot collide with them.
 */
private fun WidgetRow.stableId(): Long = when (this) {
    is WidgetRow.Heading -> -(label.hashCode().toLong() and 0xFFFF) - 1
    is WidgetRow.Task -> task.id
}

@Composable
private fun SectionHeading(label: String, colors: AppColors, action: Action) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp)
            .noRippleClickable(action),
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(colors.pageTitle),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun TaskRow(
    task: WidgetTask,
    number: Int,
    colors: AppColors,
    isArmed: Boolean,
    action: Action,
) {
    val textColor = when {
        task.done -> ColorProvider(colors.primaryText.copy(alpha = 0.33f))
        task.priority == 1 -> ColorProvider(colors.priorityText)
        else -> ColorProvider(colors.primaryText)
    }
    // A widget cannot detect a double tap, so the first tap arms the row and the
    // second within the window completes it. Without this tint the first tap would
    // look like nothing happened.
    val rowModifier = GlanceModifier
        .fillMaxWidth()
        .padding(vertical = 5.dp, horizontal = 4.dp)
        .let { if (isArmed) it.background(ColorProvider(colors.menuBackground)).cornerRadius(8.dp) else it }
        .noRippleClickable(action)

    Row(modifier = rowModifier, verticalAlignment = Alignment.Top) {
        Text(
            modifier = GlanceModifier.width(22.dp),
            text = "$number.",
            style = TextStyle(
                color = ColorProvider(colors.primaryText.copy(alpha = 0.33f)),
                fontSize = 15.sp,
            ),
            maxLines = 1,
        )
        Text(
            text = task.text,
            style = TextStyle(
                color = textColor,
                fontSize = 16.sp,
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
            ),
            maxLines = 2,
        )
    }
}

/**
 * Glance draws a ripple on every clickable by default. Overriding it with a fully
 * transparent drawable is the only way to suppress it — there is no "no ripple" flag.
 */
private fun GlanceModifier.noRippleClickable(action: Action): GlanceModifier =
    clickable(action, rippleOverride = R.drawable.widget_no_ripple)
