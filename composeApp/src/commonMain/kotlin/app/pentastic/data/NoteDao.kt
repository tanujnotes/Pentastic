package app.pentastic.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateNote(note: Note)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateNotes(notes: List<Note>)

    @Query("SELECT * FROM note ORDER BY done, CASE WHEN done = 0 THEN priority ELSE 0 END DESC, orderAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM note WHERE pageId = :pageId ORDER BY done, CASE WHEN done = 0 THEN priority ELSE 0 END DESC, orderAt DESC")
    fun getAllNotesByPage(pageId: Long): Flow<List<Note>>

    @Query("SELECT * FROM note WHERE repeatFrequency > 0 AND taskLastDoneAt > 0 AND done = 1")
    suspend fun getCompletedRepeatingNotes(): List<Note>

    @Query("SELECT * FROM note WHERE reminderEnabled = 1 AND reminderAt > 0")
    suspend fun getNotesWithActiveReminders(): List<Note>

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Query("SELECT * FROM note WHERE uuid = :uuid")
    suspend fun getNoteByUuid(uuid: String): Note?

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun deleteNote(id: Long)
}
