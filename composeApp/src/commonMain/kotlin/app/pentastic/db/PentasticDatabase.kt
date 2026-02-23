package app.pentastic.db

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.pentastic.data.Note
import app.pentastic.data.NoteDao
import app.pentastic.data.Page
import app.pentastic.data.PageDao

@Database(
    entities = [Note::class, Page::class],
    version = 7,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
    ],
    exportSchema = true
)
@TypeConverters(
    StringListTypeConverter::class
)
@ConstructedBy(PentasticDatabaseConstructor::class)
abstract class PentasticDatabase : RoomDatabase() {
    abstract val noteDao: NoteDao
    abstract val pageDao: PageDao

    companion object Companion {
        const val DB_NAME = "pentastic.db"
    }
}