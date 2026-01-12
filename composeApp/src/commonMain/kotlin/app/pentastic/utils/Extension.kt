@file:OptIn(ExperimentalTime::class)

package app.pentastic.utils

import kotlin.time.Clock
import kotlin.time.ExperimentalTime


fun Long.hasBeenDays(days: Int): Boolean =
    ((Clock.System.now().toEpochMilliseconds() - this) / Constant.ONE_DAY_IN_MILLIS) >= days

fun Long.hasBeenHours(hours: Int): Boolean =
    ((Clock.System.now().toEpochMilliseconds() - this) / Constant.ONE_HOUR_IN_MILLIS) >= hours

fun Long.hasBeenMinutes(minutes: Int): Boolean =
    ((Clock.System.now().toEpochMilliseconds() - this) / Constant.ONE_MINUTE_IN_MILLIS) >= minutes
