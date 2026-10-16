package app.pentastic.data

enum class AppCalendar {
    GREGORIAN,
    JALALI;

    companion object {
        fun fromOrdinal(ordinal: Int): AppCalendar {
            return entries.getOrElse(ordinal) { GREGORIAN }
        }
    }
}