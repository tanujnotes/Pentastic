@file:OptIn(ExperimentalTime::class)

package app.pentastic.data

import app.pentastic.utils.hasRepeatIntervalPassed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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

    suspend fun getNoteByUuid(uuid: String): Note? {
        return noteDao.getNoteByUuid(uuid)
    }

    /**
     * Notes on live pages — the same set the index counts and the Timeline groups.
     * Callers outside the UI (widgets) need this without a ViewModel, and it must not
     * drift from what the app shows, so the page filter lives here rather than being
     * rebuilt per call site. See [livePageIds].
     */
    fun liveNotesFlow(): Flow<List<Note>> = combine(
        noteDao.getAllNotes(),
        pageDao.getRootPages(),
        pageDao.getAllPages(),
        pageDao.getTimelinePage(),
    ) { notes, rootPages, allPages, timelinePage ->
        val subPagesByParent = allPages.filter { it.parentId != null }.groupBy { it.parentId!! }
        val ids = livePageIds(rootPages, subPagesByParent, timelinePage)
        notes.filter { it.pageId in ids }
    }

    /**
     * Flips completed repeating tasks back to pending once their interval has elapsed.
     * Lives here rather than in the ViewModel because the widget's midnight refresh
     * runs with no UI attached and must apply the identical reset.
     */
    suspend fun resetRepeatingTasksTodo() {
        val completedRepeatingNotes = getCompletedRepeatingNotes()
        val notesToReset = completedRepeatingNotes.filter { note ->
            val frequency = RepeatFrequency.fromOrdinal(note.repeatFrequency)
            note.taskLastDoneAt.hasRepeatIntervalPassed(frequency)
        }
        if (notesToReset.isNotEmpty()) {
            val now = Clock.System.now().toEpochMilliseconds()
            updateNotes(notesToReset.map { it.copy(done = false, orderAt = now) })
        }
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

    fun getTimelinePage(): Flow<Page?> {
        return pageDao.getTimelinePage()
    }

    suspend fun getTimelinePageOnce(): Page? {
        return pageDao.getTimelinePageOnce()
    }

    fun getSubPages(parentId: Long): Flow<List<Page>> {
        return pageDao.getSubPages(parentId)
    }

    suspend fun getPageById(id: Long): Page? {
        return pageDao.getPageById(id)
    }

    fun getPageByIdFlow(id: Long): Flow<Page?> {
        return pageDao.getPageByIdFlow(id)
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

    suspend fun updatePageType(id: Long, pageType: Int) {
        pageDao.updatePageType(id, pageType)
    }
}
