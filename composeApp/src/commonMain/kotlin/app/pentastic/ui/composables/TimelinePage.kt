@file:OptIn(ExperimentalTime::class)

package app.pentastic.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pentastic.data.Note
import app.pentastic.data.TimelineBucket
import app.pentastic.data.TimelineSection
import app.pentastic.data.classifyDueDate
import app.pentastic.data.formatDueDateLabel
import app.pentastic.data.hasDueDate
import app.pentastic.data.isDueSomeday
import app.pentastic.ui.theme.AppTheme
import app.pentastic.ui.viewmodel.MainViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.Font
import org.koin.compose.viewmodel.koinViewModel
import pentastic.composeapp.generated.resources.Merriweather_Light
import pentastic.composeapp.generated.resources.Res
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun TimelinePage(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val viewModel = koinViewModel<MainViewModel>()
    val notesByPage by viewModel.notesByPage.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val subPagesByParent by viewModel.subPagesByParent.collectAsState()

    val timeZone = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(timeZone).date

    var noteForDueDateDialog by remember { mutableStateOf<Note?>(null) }

    // Keys the user toggled away from their section's default collapse state
    var toggledSections by rememberSaveable(
        stateSaver = listSaver(
            save = { it.toList() },
            restore = { it.toSet() }
        )
    ) { mutableStateOf(setOf<String>()) }

    val sections: List<TimelineSectionUi> =
        remember(notesByPage, pages, subPagesByParent, today) {
            // Live (non-deleted, non-archived) pages = root pages + their sub-pages
            val livePageIds = buildSet {
                pages.forEach { add(it.id) }
                subPagesByParent.values.forEach { subs -> subs.forEach { add(it.id) } }
            }
            val grouped = notesByPage
                .filterKeys { it in livePageIds }
                .values.flatten()
                .filter { it.hasDueDate && !it.done }
                .groupBy { classifyDueDate(it.dueStartAt, it.dueEndAt, today, timeZone) }

            fun datedSort(notes: List<Note>) =
                notes.sortedWith(compareBy({ it.dueEndAt }, { it.dueStartAt }))

            buildList {
                TimelineSection.entries.filter { it != TimelineSection.SOMEDAY }.forEach { section ->
                    add(
                        TimelineSectionUi(
                            key = section.name,
                            label = section.label,
                            notes = datedSort(grouped[TimelineBucket.Section(section)] ?: emptyList()),
                            collapsedByDefault = false,
                            isOverdue = section == TimelineSection.OVERDUE,
                        )
                    )
                }
                // Year sections: always this year and next, plus any further year with tasks
                val years = buildSet {
                    add(today.year)
                    add(today.year + 1)
                    grouped.keys.filterIsInstance<TimelineBucket.Year>().forEach { add(it.year) }
                }.sorted()
                years.forEach { year ->
                    add(
                        TimelineSectionUi(
                            key = "YEAR_$year",
                            label = year.toString(),
                            notes = datedSort(grouped[TimelineBucket.Year(year)] ?: emptyList()),
                            collapsedByDefault = true,
                            isOverdue = false,
                        )
                    )
                }
                val someday = grouped[TimelineBucket.Section(TimelineSection.SOMEDAY)] ?: emptyList()
                add(
                    TimelineSectionUi(
                        key = TimelineSection.SOMEDAY.name,
                        label = TimelineSection.SOMEDAY.label,
                        notes = someday.sortedByDescending { it.orderAt },
                        collapsedByDefault = false,
                        isOverdue = false,
                    )
                )
            }
        }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Timeline",
            style = TextStyle(
                color = colors.pageTitle,
                fontSize = 36.sp,
                fontFamily = FontFamily(Font(Res.font.Merriweather_Light))
            ),
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            sections.forEachIndexed { sectionIndex, sectionUi ->
                val notes = sectionUi.notes
                val isCollapsed = (sectionUi.key in toggledSections) != sectionUi.collapsedByDefault
                item(key = "header_${sectionUi.key}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                toggledSections = if (sectionUi.key in toggledSections) {
                                    toggledSections - sectionUi.key
                                } else {
                                    toggledSections + sectionUi.key
                                }
                            }
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = if (sectionIndex == 0) 16.dp else 24.dp,
                                bottom = 8.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (notes.isEmpty()) sectionUi.label else "${sectionUi.label} (${notes.size})",
                            color = colors.primaryText.copy(alpha = 0.5f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isCollapsed) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isCollapsed) "Expand ${sectionUi.label}" else "Collapse ${sectionUi.label}",
                            tint = colors.primaryText.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    HorizontalDivider(
                        color = colors.divider,
                        modifier = Modifier.padding(start = 15.dp, end = 15.dp, bottom = 8.dp)
                    )
                }
                if (!isCollapsed) {
                    items(notes, key = { it.id }) { note ->
                        TimelineNoteRow(
                            note = note,
                            isOverdueSection = sectionUi.isOverdue,
                            onTap = { noteForDueDateDialog = note },
                            onDoubleTap = { viewModel.toggleNoteDone(note) },
                        )
                    }
                }
            }
        }
    }

    if (noteForDueDateDialog != null) {
        val note = noteForDueDateDialog!!
        DueDateOptionsDialog(
            currentDueStartAt = note.dueStartAt,
            currentDueEndAt = note.dueEndAt,
            onDismiss = { noteForDueDateDialog = null },
            onApply = { dueStartAt, dueEndAt ->
                viewModel.setNoteDueDate(note, dueStartAt, dueEndAt)
                noteForDueDateDialog = null
            }
        )
    }
}

private data class TimelineSectionUi(
    val key: String,
    val label: String,
    val notes: List<Note>,
    val collapsedByDefault: Boolean,
    val isOverdue: Boolean,
)

@Composable
private fun TimelineNoteRow(
    note: Note,
    isOverdueSection: Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(note) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { onDoubleTap() },
                )
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = note.text.take(25),
            fontSize = 18.sp,
            maxLines = 1,
            color = if (note.priority == 1) colors.priorityText else colors.primaryText,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "................................................................................................................... ",
            color = colors.hint,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (!note.isDueSomeday) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatDueDateLabel(note.dueStartAt, note.dueEndAt),
                fontSize = 14.sp,
                color = if (isOverdueSection) colors.priorityText else colors.primaryText.copy(alpha = 0.7f),
            )
        }
    }
}
