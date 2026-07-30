@file:OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)

package app.pentastic.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pentastic.data.DUE_SOMEDAY
import app.pentastic.data.DueDateOption
import app.pentastic.data.epochMillisToLocalDate
import app.pentastic.data.resolveRange
import app.pentastic.data.toStartOfDayMillis
import app.pentastic.ui.theme.AppTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
internal fun DueDateOptionsDialog(
    currentDueStartAt: Long,
    currentDueEndAt: Long,
    onDismiss: () -> Unit,
    onApply: (dueStartAt: Long, dueEndAt: Long) -> Unit,
) {
    val colors = AppTheme.colors
    val timeZone = TimeZone.currentSystemDefault()
    val today = remember { Clock.System.now().toLocalDateTime(timeZone).date }
    val hasDueDate = currentDueStartAt != 0L

    // Map the stored range back to a preset for the initial selection
    val initialOption = remember {
        when {
            currentDueStartAt == DUE_SOMEDAY -> DueDateOption.SOMEDAY
            hasDueDate -> {
                val start = epochMillisToLocalDate(currentDueStartAt, timeZone)
                val end = epochMillisToLocalDate(currentDueEndAt, timeZone)
                DueDateOption.entries.firstOrNull { it.resolveRange(today) == start to end }
                    ?: DueDateOption.CUSTOM
            }

            else -> null
        }
    }
    var selectedOption by remember { mutableStateOf(initialOption) }

    val initialRange = remember {
        if (currentDueStartAt > 0) {
            epochMillisToLocalDate(currentDueStartAt, timeZone) to epochMillisToLocalDate(currentDueEndAt, timeZone)
        } else {
            today to today
        }
    }
    var customStart by remember { mutableStateOf(initialRange.first) }
    var customEnd by remember { mutableStateOf(initialRange.second) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.menuBackground,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
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
                                    ) { selectedOption = option },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedOption == option,
                                    onClick = { selectedOption = option }
                                )
                                Text(text = option.label, color = colors.primaryText)
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Start/end rows (only for Custom)
                if (selectedOption == DueDateOption.CUSTOM) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = colors.divider)
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showStartPicker = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Start", color = colors.primaryText, fontSize = 15.sp)
                        Text(
                            text = rangeDateLabel(customStart),
                            color = colors.primaryText.copy(alpha = 0.7f),
                            fontSize = 15.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = colors.divider)
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showEndPicker = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("End", color = colors.primaryText, fontSize = 15.sp)
                        Text(
                            text = rangeDateLabel(customEnd),
                            color = colors.primaryText.copy(alpha = 0.7f),
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasDueDate) {
                        TextButton(onClick = { onApply(0L, 0L) }) {
                            Text("Clear", color = colors.primaryText.copy(alpha = 0.6f))
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = colors.primaryText)
                        }
                        Button(
                            enabled = selectedOption != null,
                            onClick = {
                                when (val option = selectedOption) {
                                    null -> {}
                                    DueDateOption.SOMEDAY -> onApply(DUE_SOMEDAY, DUE_SOMEDAY)
                                    DueDateOption.CUSTOM -> onApply(
                                        customStart.toStartOfDayMillis(timeZone),
                                        customEnd.toStartOfDayMillis(timeZone)
                                    )

                                    else -> {
                                        val (start, end) = option.resolveRange(today)!!
                                        onApply(
                                            start.toStartOfDayMillis(timeZone),
                                            end.toStartOfDayMillis(timeZone)
                                        )
                                    }
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

    if (showStartPicker) {
        StartDatePickerDialog(
            initialDate = customStart,
            onDismiss = { showStartPicker = false },
            onConfirm = { date ->
                customStart = date
                if (customEnd < date) customEnd = date
                showStartPicker = false
            },
            title = "Select start date",
        )
    }
    if (showEndPicker) {
        StartDatePickerDialog(
            initialDate = customEnd,
            onDismiss = { showEndPicker = false },
            onConfirm = { date ->
                customEnd = date
                if (customStart > date) customStart = date
                showEndPicker = false
            },
            title = "Select end date",
        )
    }
}

private fun rangeDateLabel(date: LocalDate): String {
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "${date.dayOfMonth} $month ${date.year}"
}
