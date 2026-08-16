package app.pentastic.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual class DatabaseFactory {
    actual fun create(): RoomDatabase.Builder<PentasticDatabase> {
        val dbFile = File(appDataDir(), PentasticDatabase.DB_NAME)
        return Room.databaseBuilder(dbFile.absolutePath)
    }
}
