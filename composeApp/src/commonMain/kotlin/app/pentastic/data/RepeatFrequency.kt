package app.pentastic.data

enum class RepeatFrequency(val days: Int, val label: String) {
    NONE(0, "None"),
    DAILY(1, "Daily"),
    WEEKLY(7, "Weekly"),
    MONTHLY(30, "Monthly"),
    YEARLY(365, "Yearly");

    companion object {
        fun fromOrdinal(ordinal: Int): RepeatFrequency {
            return entries.getOrElse(ordinal) { NONE }
        }
    }
}
