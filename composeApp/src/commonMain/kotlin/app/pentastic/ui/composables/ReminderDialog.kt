@file:OptIn(ExperimentalMaterial3Api::class, kotlin.time.ExperimentalTime::class)

package app.pentastic.ui.composables

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import app.pentastic.ui.theme.AppTheme
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Composable
fun ReminderDialog(
    currentReminderAt: Long,
    reminderEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (reminderAt: Long, enabled: Boolean) -> Unit,
    onRemoveReminder: () -> Unit,
) {
    val colors = AppTheme.colors
    val timeZone = TimeZone.currentSystemDefault()
    val now = Clock.System.now()

    var isEnabled by remember { mutableStateOf(reminderEnabled || currentReminderAt == 0L) }

    // Initialize date/time from existing reminder or default to tomorrow 9 AM
    val initialDateTime = remember {
        if (currentReminderAt > 0) {
            Instant.fromEpochMilliseconds(currentReminderAt).toLocalDateTime(timeZone)
        } else {
            val tomorrow = now.toLocalDateTime(timeZone).date.plus(1, DateTimeUnit.DAY)
            LocalDateTime(tomorrow, LocalTime(9, 0))
        }
    }

    var selectedDate by remember { mutableStateOf(initialDateTime.date) }
    var selectedHour by remember { mutableStateOf(initialDateTime.hour) }
    var selectedMinute by remember { mutableStateOf(initialDateTime.minute) }

    var showDateTimePicker by remember { mutableStateOf(false) }

    fun calculateReminderTime(): Long {
        return LocalDateTime(selectedDate, LocalTime(selectedHour, selectedMinute))
            .toInstant(timeZone).toEpochMilliseconds()
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.menuBackground,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Set Reminder",
                    color = colors.primaryText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 24.sp
                )

                Spacer(Modifier.height(20.dp))

                // Date/Time display - clickable to open picker
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${selectedDate.dayOfMonth} ${formatMonth(selectedDate.monthNumber)} ${selectedDate.year}",
                        color = colors.primaryText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${padZero(selectedHour)}:${padZero(selectedMinute)}",
                        color = colors.primaryText.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Change button
                TextButton(
                    onClick = { showDateTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change date & time", color = colors.primaryText)
                }

                // Enable/Disable toggle (only shown if reminder already exists)
                if (currentReminderAt > 0) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enabled", color = colors.primaryText)
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentReminderAt > 0) {
                        TextButton(onClick = {
                            onRemoveReminder()
                            onDismiss()
                        }) {
                            Text("Remove", color = colors.primaryText.copy(alpha = 0.6f))
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = colors.primaryText)
                        }
                        TextButton(onClick = {
                            val reminderTime = calculateReminderTime()
                            onConfirm(reminderTime, isEnabled)
                        }) {
                            Text("Save", color = colors.primaryText)
                        }
                    }
                }
            }
        }
    }

    // Date/Time picker dialog
    if (showDateTimePicker) {
        DateTimePickerDialog(
            initialDate = selectedDate,
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            hasExistingReminder = currentReminderAt > 0,
            onDismiss = { showDateTimePicker = false },
            onConfirm = { date, hour, minute ->
                selectedDate = date
                selectedHour = hour
                selectedMinute = minute
                showDateTimePicker = false
            },
            onClear = {
                onRemoveReminder()
                onDismiss()
            }
        )
    }
}

private fun formatMonth(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> ""
    }
}

private fun padZero(value: Int): String {
    return if (value < 10) "0$value" else value.toString()
}
