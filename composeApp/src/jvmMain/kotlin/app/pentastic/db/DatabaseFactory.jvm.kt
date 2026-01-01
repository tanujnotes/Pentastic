package app.pentastic.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual class DatabaseFactory {
    actual fun create(): RoomDatabase.Builder<PentasticDatabase> {
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

        val dbFile = File(appDataDir, PentasticDatabase.DB_NAME)
        return Room.databaseBuilder(dbFile.absolutePath)
    }
}