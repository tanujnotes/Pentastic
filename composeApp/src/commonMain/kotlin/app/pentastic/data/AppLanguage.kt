package app.pentastic.data

enum class AppLanguage(val label: String, val code: String, val isRtl: Boolean) {
    ENGLISH("English", "en", isRtl = false),
    FARSI("فارسی", "fa", isRtl = true);

    companion object {
        fun fromOrdinal(ordinal: Int): AppLanguage {
            return entries.getOrElse(ordinal) { ENGLISH }
        }
    }
}