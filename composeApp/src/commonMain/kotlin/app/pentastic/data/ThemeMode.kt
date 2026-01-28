package app.pentastic.data

enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System"),
    DAY_NIGHT("Day/Night");

    companion object {
        fun fromOrdinal(ordinal: Int): ThemeMode {
            return entries.getOrElse(ordinal) { DAY_NIGHT }
        }
    }
}
