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

    @Query("SELECT * FROM note ORDER BY done, priority DESC, orderAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM note WHERE page = :page ORDER BY done, priority DESC, orderAt DESC")
    fun getAllNotesByPage(page: Int): Flow<List<Note>>

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun deleteNote(id: Long)
}
