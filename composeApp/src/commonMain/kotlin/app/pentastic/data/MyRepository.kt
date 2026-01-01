package app.pentastic.data

import kotlinx.coroutines.flow.Flow

class MyRepository(private val noteDao: NoteDao) {

    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
    }

    fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes()
    }

    fun getAllNotesByPage(page: Int): Flow<List<Note>> {
        return noteDao.getAllNotesByPage(page)
    }

    suspend fun deleteNote(id: Long) {
        return noteDao.deleteNote(id)
    }
}