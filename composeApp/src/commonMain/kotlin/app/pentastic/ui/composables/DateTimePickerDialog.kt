@file:OptIn(ExperimentalMaterial3Api::class)

package app.pentastic.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pentastic.data.AppCalendar
import app.pentastic.data.RepeatFrequency
import app.pentastic.ui.theme.AppTheme
import app.pentastic.utils.JalaliCalendar
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun DateTimePickerDialog(
    initialDate: LocalDate,
    initialHour: Int,
    initialMinute: Int,
    hasExistingReminder: Boolean,
    isRepeatingTask: Boolean = false,
    repeatFrequency: RepeatFrequency = RepeatFrequency.NONE,
    onDismiss: () -> Unit,
    onConfirm: (date: LocalDate, hour: Int, minute: Int) -> Unit,
    onClear: () -> Unit,
    onOpenRepeatDialog: () -> Unit = {},
) {
    val colors = AppTheme.colors
    val strings = AppTheme.strings

    var selectedDate by remember { mutableStateOf(initialDate) }
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    // Check if selected date/time is in the past
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val selectedDateTime = LocalDateTime(
        selectedDate.year,
        selectedDate.month,
        selectedDate.dayOfMonth,
        selectedHour,
        selectedMinute
    )
    val isInPast = selectedDateTime < now

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.menuBackground,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Title
                Text(
                    text = strings.setReminder,
                    color = colors.primaryText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )

                Spacer(Modifier.height(20.dp))

                if (isRepeatingTask) {
                    // Show info text for repeating tasks instead of date picker
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onOpenRepeatDialog()
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.primaryText.copy(alpha = 0.08f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = strings.repeatingTaskInfo(repeatFrequency),
                                color = colors.primaryText,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = strings.tapToEditRepeat,
                                color = colors.primaryText.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Time picker only for repeating tasks
                    CompactTimePicker(
                        selectedHour = selectedHour,
                        selectedMinute = selectedMinute,
                        onTimeSelected = { hour, minute ->
                            selectedHour = hour
                            selectedMinute = minute
                        }
                    )
                } else {
                    // Date wheel picker for non-repeating tasks
                    DateWheelPicker(
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it }
                    )

                    Spacer(Modifier.height(16.dp))

                    // Time picker inline
                    CompactTimePicker(
                        selectedHour = selectedHour,
                        selectedMinute = selectedMinute,
                        onTimeSelected = { hour, minute ->
                            selectedHour = hour
                            selectedMinute = minute
                        }
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clear button (only shown if there's an existing reminder)
                    if (hasExistingReminder) {
                        TextButton(onClick = {
                            onClear()
                            onDismiss()
                        }) {
                            Text(strings.clear, color = colors.primaryText.copy(alpha = 0.6f))
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) {
                            Text(strings.cancel, color = colors.primaryText)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onConfirm(selectedDate, selectedHour, selectedMinute)
                            },
                            enabled = !isInPast,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primaryText,
                                contentColor = colors.menuBackground,
                                disabledContainerColor = colors.primaryText.copy(alpha = 0.3f),
                                disabledContentColor = colors.menuBackground.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(strings.done)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactTimePicker(
    selectedHour: Int,
    selectedMinute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
) {
    val colors = AppTheme.colors
    val dateTexts = AppTheme.dateTexts

    // Convert 24-hour to 12-hour format
    val isAm = selectedHour < 12
    val hour12 = when {
        selectedHour == 0 -> 12
        selectedHour > 12 -> selectedHour - 12
        else -> selectedHour
    }

    fun toHour24(hour12: Int, isAm: Boolean): Int {
        return when {
            hour12 == 12 && isAm -> 0
            hour12 == 12 && !isAm -> 12
            isAm -> hour12
            else -> hour12 + 12
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Digits read left-to-right even in Farsi, so the time keeps its hour : minute
        // order in both languages; only the AM/PM marker moves, matching how the
        // formatted labels render («ب.ظ ۳:۰۵»). Under an RTL layout direction the Row
        // mirrors child order, so there the children are declared minute-first.
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

        val hourWheel: @Composable () -> Unit = {
            // Hour picker (1-12)
            WheelPicker(
                items = (1..12).toList(),
                selectedItem = hour12,
                onItemSelected = { newHour12 ->
                    onTimeSelected(toHour24(newHour12, isAm), selectedMinute)
                },
                format = { dateTexts.num2(it) }
            )
        }
        val colon: @Composable () -> Unit = {
            Text(
                text = ":",
                color = colors.icon,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        val minuteWheel: @Composable () -> Unit = {
            // Minute picker
            WheelPicker(
                items = (0..59).toList(),
                selectedItem = selectedMinute,
                onItemSelected = { onTimeSelected(selectedHour, it) },
                format = { dateTexts.num2(it) }
            )
        }
        val amPmWheel: @Composable () -> Unit = {
            Spacer(Modifier.width(8.dp))

            // AM/PM picker
            WheelPickerGeneric(
                items = listOf(true, false),
                selectedItem = isAm,
                onItemSelected = { newIsAm ->
                    onTimeSelected(toHour24(hour12, newIsAm), selectedMinute)
                },
                format = { dateTexts.amPm(it) },
                itemWidth = 52.dp
            )
        }

        if (rtl) {
            minuteWheel()
            colon()
            hourWheel()
            amPmWheel()
        } else {
            hourWheel()
            colon()
            minuteWheel()
            amPmWheel()
        }
    }
}

@Composable
private fun WheelPicker(
    items: List<Int>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    format: (Int) -> String = { it.toString() },
) {
    val colors = AppTheme.colors
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val itemHeight = 40.dp
    val visibleItems = 3
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    // Scroll to selected item on first composition
    LaunchedEffect(Unit) {
        val index = items.indexOf(selectedItem)
        if (index >= 0) {
            listState.scrollToItem(index)
        }
    }

    // Detect when scrolling stops and snap to center item
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val firstVisibleOffset = listState.firstVisibleItemScrollOffset

            // Determine which item is closest to center
            val centerIndex = if (firstVisibleOffset > itemHeightPx / 2) {
                firstVisibleIndex + 1
            } else {
                firstVisibleIndex
            }

            // Update selection and snap to position
            if (centerIndex in items.indices && items[centerIndex] != selectedItem) {
                onItemSelected(items[centerIndex])
            }
            listState.animateScrollToItem(centerIndex)
        }
    }

    Box(
        modifier = Modifier
            .width(56.dp)
            .height(itemHeight * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        // Top divider line
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = itemHeight),
            thickness = 1.dp,
            color = colors.primaryText.copy(alpha = 0.2f)
        )

        // Bottom divider line
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = itemHeight),
            thickness = 1.dp,
            color = colors.primaryText.copy(alpha = 0.2f)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * visibleItems),
            contentPadding = PaddingValues(vertical = itemHeight),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            items(items) { item ->
                val isSelected = item == selectedItem

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val index = items.indexOf(item)
                            onItemSelected(item)
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(item),
                        color = if (isSelected) colors.primaryText else colors.primaryText.copy(alpha = 0.4f),
                        fontSize = if (isSelected) 20.sp else 16.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun DateWheelPicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    val colors = AppTheme.colors
    val dateTexts = AppTheme.dateTexts
    val isJalali = AppTheme.calendar == AppCalendar.JALALI
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    // Today and the selected date, broken down in the active calendar
    val todayParts = dateTexts.parts(today.toEpochDays().toInt())
    val selectedParts = dateTexts.parts(selectedDate.toEpochDays().toInt())

    // Generate year range (current year to +10 years) in the active calendar
    val years = remember(todayParts.year) { (todayParts.year..(todayParts.year + 10)).toList() }
    val months = remember { (1..12).toList() }

    var selectedYear by remember { mutableStateOf(selectedParts.year) }
    var selectedMonth by remember { mutableStateOf(selectedParts.month) }
    var selectedDay by remember { mutableStateOf(selectedParts.day) }

    // Calculate days in the selected month, in the active calendar
    val daysInMonth = remember(selectedYear, selectedMonth, isJalali) {
        if (isJalali) JalaliCalendar.monthLength(selectedYear, selectedMonth)
        else getDaysInMonth(selectedYear, Month.entries[selectedMonth - 1])
    }
    val days = remember(daysInMonth) { (1..daysInMonth).toList() }

    // Adjust day if it exceeds the days in the new month
    LaunchedEffect(daysInMonth) {
        if (selectedDay > daysInMonth) {
            selectedDay = daysInMonth
        }
    }

    // Update the selected date when any component changes
    LaunchedEffect(selectedYear, selectedMonth, selectedDay) {
        val day = selectedDay.coerceAtMost(daysInMonth)
        val newDate = if (isJalali) {
            LocalDate.fromEpochDays(JalaliCalendar.toEpochDays(selectedYear, selectedMonth, day))
        } else {
            LocalDate(selectedYear, selectedMonth, day)
        }
        if (newDate != selectedDate && newDate >= today) {
            onDateSelected(newDate)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Digits and month names read left-to-right even in Farsi, so the wheels keep
        // their year | month | day left-to-right order in both languages. Under an RTL
        // layout direction the Row mirrors child order, so there the wheels are
        // declared day-first to cancel the mirroring and match the date labels.
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

        val yearWheel: @Composable () -> Unit = {
            // Year picker
            WheelPickerGeneric(
                items = years,
                selectedItem = selectedYear,
                onItemSelected = { selectedYear = it },
                format = { dateTexts.num(it) },
                itemWidth = 64.dp
            )
        }
        val monthWheel: @Composable () -> Unit = {
            // Month picker
            WheelPickerGeneric(
                items = months,
                selectedItem = selectedMonth,
                onItemSelected = { selectedMonth = it },
                format = { dateTexts.monthAbbrev(it) },
                itemWidth = 92.dp
            )
        }
        val dayWheel: @Composable () -> Unit = {
            // Day picker
            WheelPickerGeneric(
                items = days,
                selectedItem = selectedDay,
                onItemSelected = { selectedDay = it },
                format = { dateTexts.num(it) }
            )
        }

        if (rtl) {
            dayWheel()
            Spacer(Modifier.width(8.dp))
            monthWheel()
            Spacer(Modifier.width(8.dp))
            yearWheel()
        } else {
            yearWheel()
            Spacer(Modifier.width(8.dp))
            monthWheel()
            Spacer(Modifier.width(8.dp))
            dayWheel()
        }
    }
}

@Composable
private fun <T> WheelPickerGeneric(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    format: (T) -> String,
    itemWidth: Dp = 56.dp,
) {
    val colors = AppTheme.colors
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val itemHeight = 40.dp
    val visibleItems = 3
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    // Scroll to selected item on first composition
    LaunchedEffect(Unit) {
        val index = items.indexOf(selectedItem)
        if (index >= 0) {
            listState.scrollToItem(index)
        }
    }

    // Detect when scrolling stops and snap to center item
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val firstVisibleOffset = listState.firstVisibleItemScrollOffset

            // Determine which item is closest to center
            val centerIndex = if (firstVisibleOffset > itemHeightPx / 2) {
                firstVisibleIndex + 1
            } else {
                firstVisibleIndex
            }

            // Update selection and snap to position
            if (centerIndex in items.indices && items[centerIndex] != selectedItem) {
                onItemSelected(items[centerIndex])
            }
            listState.animateScrollToItem(centerIndex)
        }
    }

    Box(
        modifier = Modifier
            .width(itemWidth)
            .height(itemHeight * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        // Top divider line
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = itemHeight),
            thickness = 1.dp,
            color = colors.primaryText.copy(alpha = 0.2f)
        )

        // Bottom divider line
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = itemHeight),
            thickness = 1.dp,
            color = colors.primaryText.copy(alpha = 0.2f)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * visibleItems),
            contentPadding = PaddingValues(vertical = itemHeight),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isSelected = item == selectedItem

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onItemSelected(item)
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(item),
                        color = if (isSelected) colors.primaryText else colors.primaryText.copy(alpha = 0.4f),
                        fontSize = if (isSelected) 18.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun getDaysInMonth(year: Int, month: Month): Int {
    return when (month) {
        Month.JANUARY, Month.MARCH, Month.MAY, Month.JULY,
        Month.AUGUST, Month.OCTOBER, Month.DECEMBER,
            -> 31

        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        Month.FEBRUARY -> if (isLeapYear(year)) 29 else 28
    }
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}
