package app.pentastic.widget

import app.pentastic.data.Note

/**
 * Flat render model for a widget row. Glance serialises its composition into
 * RemoteViews, so the widget carries only the handful of fields it draws rather than
 * whole [Note] rows.
 */
data class WidgetTask(
    val id: Long,
    val uuid: String,
    val text: String,
    val done: Boolean,
    val priority: Int,
    val isNotesType: Boolean,
)

fun Note.toWidgetTask(isNotesType: Boolean = false) = WidgetTask(
    id = id,
    uuid = uuid,
    text = text,
    done = done,
    priority = priority,
    isNotesType = isNotesType,
)
