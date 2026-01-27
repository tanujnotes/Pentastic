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
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
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