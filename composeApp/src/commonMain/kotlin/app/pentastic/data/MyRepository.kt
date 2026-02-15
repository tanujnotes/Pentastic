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

    suspend fun getNotesWithActiveReminders(): List<Note> {
        return noteDao.getNotesWithActiveReminders()
    }

    suspend fun getNoteById(id: Long): Note? {
        return noteDao.getNoteById(id)
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

    fun getRootPages(): Flow<List<Page>> {
        return pageDao.getRootPages()
    }

    fun getSubPages(parentId: Long): Flow<List<Page>> {
        return pageDao.getSubPages(parentId)
    }

    suspend fun getPageById(id: Long): Page? {
        return pageDao.getPageById(id)
    }

    suspend fun deletePage(id: Long) {
        pageDao.deletePage(id)
    }

    // Trash operations

    suspend fun softDeleteNote(id: Long, deletedAt: Long) {
        noteDao.softDeleteNote(id, deletedAt)
    }

    suspend fun softDeletePage(id: Long, deletedAt: Long) {
        pageDao.softDeleteSubPages(id, deletedAt)
        pageDao.softDeletePage(id, deletedAt)
    }

    suspend fun restoreNote(id: Long) {
        noteDao.restoreNote(id)
    }

    suspend fun restorePage(id: Long) {
        pageDao.restorePage(id)
        pageDao.restoreSubPages(id)
    }

    fun getTrashedPages(): Flow<List<Page>> {
        return pageDao.getTrashedPages()
    }

    fun getTrashedNotes(): Flow<List<Note>> {
        return noteDao.getTrashedNotes()
    }

    suspend fun emptyTrash() {
        noteDao.permanentlyDeleteAllTrashedNotes()
        pageDao.permanentlyDeleteAllTrashedPages()
    }

    // Archive operations

    suspend fun archivePage(id: Long, archivedAt: Long) {
        pageDao.archiveSubPages(id, archivedAt)
        pageDao.archivePage(id, archivedAt)
    }

    suspend fun unarchivePage(id: Long) {
        pageDao.unarchivePage(id)
        pageDao.unarchiveSubPages(id)
    }

    fun getArchivedPages(): Flow<List<Page>> {
        return pageDao.getArchivedPages()
    }

    fun getArchivedSubPages(parentId: Long): Flow<List<Page>> {
        return pageDao.getArchivedSubPages(parentId)
    }
}
