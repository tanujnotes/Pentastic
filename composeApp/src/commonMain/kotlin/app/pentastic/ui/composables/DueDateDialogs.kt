@file:OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)

package app.pentastic.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pentastic.data.DUE_SOMEDAY
import app.pentastic.data.DueDateOption
import app.pentastic.data.dueValueToLocalDate
import app.pentastic.data.resolveRange
import app.pentastic.data.toDueValue
import app.pentastic.ui.theme.AppTheme
import app.pentastic.ui.theme.appRadioButtonColors
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
internal fun DueDateOptionsDialog(
    currentDueStartAt: Long,
    currentDueEndAt: Long,
    onDismiss: () -> Unit,
    onApply: (dueStartAt: Long, dueEndAt: Long) -> Unit,
    // Clearing removes a task from the timeline, so the timeline hides the option
    showClear: Boolean = true,
) {
    val colors = AppTheme.colors
    val timeZone = TimeZone.currentSystemDefault()
    val today = remember { Clock.System.now().toLocalDateTime(timeZone).date }
    val hasDueDate = currentDueStartAt != 0L
    val canClear = showClear && hasDueDate

    // Map the stored range back to a preset for the initial selection
    val initialOption = remember {
        when {
            currentDueStartAt == DUE_SOMEDAY -> DueDateOption.SOMEDAY
            hasDueDate -> {
                val start = dueValueToLocalDate(currentDueStartAt, timeZone)
                val end = dueValueToLocalDate(currentDueEndAt, timeZone)
                DueDateOption.entries.firstOrNull { it.resolveRange(today) == start to end }
                    ?: DueDateOption.CUSTOM
            }

            else -> null
        }
    }
    var selectedOption by remember { mutableStateOf(initialOption) }

    // Seed for the Custom wheel picker: existing due end date, else today
    val customDate = remember {
        if (currentDueStartAt > 0) dueValueToLocalDate(currentDueEndAt, timeZone) else today
    }
    var showCustomDatePicker by remember { mutableStateOf(false) }

    // Drill-down toward an optional exact date: year presets go through a months page,
    // week/month presets straight to a days page. Save works at any depth.
    var page by remember { mutableStateOf(DueDatePage.OPTIONS) }
    var chosenMonth by remember { mutableStateOf<Month?>(null) }
    var chosenDay by remember { mutableStateOf<LocalDate?>(null) }

    val onOptionTap: (DueDateOption) -> Unit = { option ->
        when {
            // Options with nothing left to refine apply immediately, no Save click needed
            option == DueDateOption.TODAY || option == DueDateOption.TOMORROW -> {
                selectedOption = option
                val (start, end) = option.resolveRange(today)!!
                onApply(start.toDueValue(), end.toDueValue())
            }

            option == DueDateOption.SOMEDAY -> {
                selectedOption = option
                onApply(DUE_SOMEDAY, DUE_SOMEDAY)
            }
            // Custom goes straight to the wheel picker; applied on its Done
            option == DueDateOption.CUSTOM -> showCustomDatePicker = true

            else -> {
                if (option != selectedOption) {
                    chosenMonth = null
                    chosenDay = null
                }
                selectedOption = option
                when {
                    option.drillsToMonths -> page = DueDatePage.MONTHS
                    option.drillsToDays -> page = DueDatePage.DAYS
                }
            }
        }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.menuBackground,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                when (page) {
                    DueDatePage.OPTIONS -> {
                        Text("Due date", color = colors.primaryText, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                        Spacer(Modifier.height(16.dp))

                        // 2 column grid of radio options
                        DueDateOption.entries.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowItems.forEach { option ->
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { onOptionTap(option) },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        RadioButton(
                                            selected = selectedOption == option,
                                            onClick = { onOptionTap(option) },
                                            colors = appRadioButtonColors(),
                                        )
                                        Text(text = option.label, color = colors.primaryText)
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    DueDatePage.MONTHS -> {
                        // Only reachable after a year preset was tapped, so selectedOption is set
                        val option = selectedOption!!
                        DrillPageHeader(title = option.label, onBack = { page = DueDatePage.OPTIONS })
                        Spacer(Modifier.height(16.dp))

                        Month.entries.chunked(3).forEach { rowMonths ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                rowMonths.forEach { month ->
                                    val selected = chosenMonth == month
                                    Box(
                                        modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = monthLabel(month),
                                            fontSize = 15.sp,
                                            color = if (selected) colors.menuBackground else colors.primaryText,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .then(if (selected) Modifier.background(colors.primaryText) else Modifier)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    if (month != chosenMonth) chosenDay = null
                                                    chosenMonth = month
                                                    page = DueDatePage.DAYS
                                                }
                                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DueDatePage.DAYS -> {
                        // Only reachable after a preset tap (and a month tap for year presets),
                        // so selectedOption — and chosenMonth in the year flow — are set
                        val option = selectedOption!!
                        val (blockStart, blockEnd) =
                            if (option.drillsToMonths) monthBlock(option.presetYear(today), chosenMonth!!)
                            else option.resolveRange(today)!!
                        val title =
                            if (option.drillsToMonths) "${option.label} · ${monthLabel(chosenMonth!!)}"
                            else option.label
                        DrillPageHeader(
                            title = title,
                            onBack = {
                                page = if (option.drillsToMonths) DueDatePage.MONTHS else DueDatePage.OPTIONS
                            }
                        )
                        Spacer(Modifier.height(16.dp))

                        val days = remember(blockStart, blockEnd) {
                            generateSequence(blockStart) { it.plus(1, DateTimeUnit.DAY) }
                                .takeWhile { it <= blockEnd }
                                .toList()
                        }
                        val isWeek = option == DueDateOption.THIS_WEEK || option == DueDateOption.NEXT_WEEK
                        if (isWeek) {
                            // One row per day, weekday first; month in every label disambiguates
                            // weeks spanning a month boundary
                            days.forEach { day ->
                                val selected = chosenDay == day
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Text(
                                        text = "${weekDayLabel(day)} ${day.dayOfMonth} ${monthLabel(day.month)}",
                                        fontSize = 15.sp,
                                        color = if (selected) colors.menuBackground else colors.primaryText,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .then(if (selected) Modifier.background(colors.primaryText) else Modifier)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { chosenDay = if (selected) null else day }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        } else {
                            days.chunked(7).forEach { rowDays ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    rowDays.forEach { day ->
                                        val selected = chosenDay == day
                                        Box(
                                            modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .then(if (selected) Modifier.background(colors.primaryText) else Modifier)
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { chosenDay = if (selected) null else day },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${day.dayOfMonth}",
                                                    fontSize = 14.sp,
                                                    color = if (selected) colors.menuBackground else colors.primaryText
                                                )
                                            }
                                        }
                                    }
                                    repeat(7 - rowDays.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // The options page applies on tap, so only the drill pages need Save/Cancel
                val needsConfirm = page != DueDatePage.OPTIONS
                if (needsConfirm || canClear) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (canClear) {
                            TextButton(onClick = { onApply(0L, 0L) }) {
                                Text("Clear", color = colors.primaryText.copy(alpha = 0.6f))
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        if (needsConfirm) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = onDismiss) {
                                    Text("Cancel", color = colors.primaryText)
                                }
                                Button(
                                    onClick = {
                                        // Only drill presets reach here, so the option is set
                                        selectedOption?.let { option ->
                                            // Deepest refinement wins: exact day, else month
                                            // block (year drill), else the full preset block
                                            val day = chosenDay
                                            val month = chosenMonth
                                            val (start, end) = when {
                                                day != null -> day to day
                                                option.drillsToMonths && month != null ->
                                                    monthBlock(option.presetYear(today), month)

                                                else -> option.resolveRange(today)!!
                                            }
                                            onApply(start.toDueValue(), end.toDueValue())
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primaryText,
                                        contentColor = colors.menuBackground
                                    )
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomDatePicker) {
        StartDatePickerDialog(
            initialDate = customDate,
            onDismiss = { showCustomDatePicker = false },
            onConfirm = { date ->
                showCustomDatePicker = false
                selectedOption = DueDateOption.CUSTOM
                onApply(date.toDueValue(), date.toDueValue())
            },
            title = "Select date",
        )
    }
}

@Composable
private fun DrillPageHeader(title: String, onBack: () -> Unit) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = colors.primaryText,
            modifier = Modifier
                .size(20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onBack() }
        )
        Spacer(Modifier.width(12.dp))
        Text(title, color = colors.primaryText, fontWeight = FontWeight.Medium, fontSize = 18.sp)
    }
}

private enum class DueDatePage { OPTIONS, MONTHS, DAYS }

private val DueDateOption.drillsToMonths: Boolean
    get() = this == DueDateOption.THIS_YEAR || this == DueDateOption.NEXT_YEAR

private val DueDateOption.drillsToDays: Boolean
    get() = this == DueDateOption.THIS_WEEK || this == DueDateOption.NEXT_WEEK ||
            this == DueDateOption.THIS_MONTH || this == DueDateOption.NEXT_MONTH

private fun DueDateOption.presetYear(today: LocalDate): Int =
    if (this == DueDateOption.NEXT_YEAR) today.year + 1 else today.year

private fun monthBlock(year: Int, month: Month): Pair<LocalDate, LocalDate> {
    val first = LocalDate(year, month, 1)
    return first to first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
}

private fun monthLabel(month: Month): String =
    month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }

private fun weekDayLabel(date: LocalDate): String =
    date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
