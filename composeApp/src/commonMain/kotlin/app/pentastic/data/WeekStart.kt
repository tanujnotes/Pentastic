package app.pentastic.data

/**
 * The day a week begins on, as the user chose in Settings.
 * [isoWeekday] follows the app's ISO numbering: Mon=1 ... Sat=6, Sun=7.
 */
enum class WeekStart(val isoWeekday: Int) {
    SATURDAY(6),
    SUNDAY(7),
    MONDAY(1);

    companion object {
        fun fromOrdinal(ordinal: Int): WeekStart {
            return entries.getOrElse(ordinal) { MONDAY }
        }
    }
}
