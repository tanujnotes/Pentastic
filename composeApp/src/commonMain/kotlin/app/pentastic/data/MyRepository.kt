package app.pentastic.data

import kotlinx.coroutines.flow.Flow

class MyRepository(
    private val noteDao: NoteDao,
    private val pageDao: PageDao
) {

    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun updateNotes(notes: List<Note>) {
        noteDao.updateNotes(notes)
    }

    fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes()
    }

    suspend fun getCompletedRepeatingNotes(): List<Note> {
        return noteDao.getCompletedRepeatingNotes()
    }

    fun getAllNotesByPage(pageId: Long): Flow<List<Note>> {
        return noteDao.getAllNotesByPage(pageId)
    }

    suspend fun deleteNote(id: Long) {
        return noteDao.deleteNote(id)
    }

    suspend fun insertPage(page: Page): Long {
        return pageDao.insertPage(page)
    }

    suspend fun updatePage(page: Page) {
        pageDao.updatePage(page)
    }

    suspend fun updatePages(pages: List<Page>) {
        pageDao.updatePages(pages)
    }

    fun getAllPages(): Flow<List<Page>> {
        return pageDao.getAllPages()
    }

    suspend fun getPageById(id: Long): Page? {
        return pageDao.getPageById(id)
    }

    suspend fun deletePage(id: Long) {
        pageDao.deletePage(id)
    }
}
