package app.pentastic.data

enum class PageType(val label: String) {
    TASKS("Tasks"),
    NOTES("Notes");

    companion object {
        fun fromOrdinal(ordinal: Int): PageType {
            return entries.getOrElse(ordinal) { TASKS }
        }
    }
}
