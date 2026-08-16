package app.pentastic.db

import java.io.File

/**
 * Per-OS application data directory, shared by the database and DataStore so
 * everything the desktop app persists lives in one place.
 */
fun appDataDir(): File {
    val os = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")
    val appDataDir = when {
        os.contains("win") -> File(System.getenv("APPDATA"), "Pentastic")
        os.contains("mac") -> File(userHome, "Library/Application Support/Pentastic")
        else -> File(userHome, ".local/share/Pentastic")
    }

    if (!appDataDir.exists()) {
        appDataDir.mkdirs()
    }
    return appDataDir
}
