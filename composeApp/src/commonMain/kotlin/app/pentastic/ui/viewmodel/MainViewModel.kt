package app.pentastic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pentastic.data.DataStoreRepository
import app.pentastic.data.MyRepository
import app.pentastic.data.Note
import app.pentastic.data.Page
import app.pentastic.data.PageType
import app.pentastic.data.RepeatFrequency
import app.pentastic.data.ThemeMode
import app.pentastic.notification.ReminderScheduler
import app.pentastic.utils.hasBeenHours
import app.pentastic.utils.hasRepeatIntervalPassed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class MainViewModel(
    private val repository: MyRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    companion object {
        const val MAX_PAGE_NAME_LENGTH = 20
    }

    private val _showRateButton = MutableStateFlow(false)
    val showRateButton: StateFlow<Boolean> = _showRateButton.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.DAY_NIGHT)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _notesByPage = MutableStateFlow<Map<Long, List<Note>>>(emptyMap())
    val notesByPage: StateFlow<Map<Long, List<Note>>> = _notesByPage.asStateFlow()

    private val _notesCountByPage = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val notesCountByPage: StateFlow<Map<Long, Int>> = _notesCountByPage.asStateFlow()

    private val _priorityNotesCountByPage = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val priorityNotesCountByPage: StateFlow<Map<Long, Int>> = _priorityNotesCountByPage.asStateFlow()

    private val _showCompletedTasks = MutableStateFlow(false)
    val showCompletedTasks: StateFlow<Boolean> = _showCompletedTasks.asStateFlow()

    private val _showSubPages = MutableStateFlow(true)
    val showSubPages: StateFlow<Boolean> = _showSubPages.asStateFlow()

    private val _editingNote = MutableStateFlow<Note?>(null)
    val editingNote: StateFlow<Note?> = _editingNote.asStateFlow()

    private val _subPagesByParent = MutableStateFlow<Map<Long, List<Page>>>(emptyMap())
    val subPagesByParent: StateFlow<Map<Long, List<Page>>> = _subPagesByParent.asStateFlow()

    val trashedPages: StateFlow<List<Page>> = repository.getTrashedPages().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val trashedNotes: StateFlow<List<Note>> = repository.getTrashedNotes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val archivedPages: StateFlow<List<Page>> = repository.getArchivedPages().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val pages: StateFlow<List<Page>> = repository.getRootPages().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        checkFirstLaunch()
        loadNotesByPage()
        loadSubPages()
        checkForRateButton()
        loadThemeMode()
        loadShowCompletedTasks()
        loadShowSubPages()
        rescheduleRemindersOnStart()
    }

    private fun rescheduleRemindersOnStart() {
        viewModelScope.launch {
            reminderScheduler.rescheduleAllReminders()
        }
    }

    fun setEditingNote(note: Note?) {
        _editingNote.value = note
    }

    suspend fun getPageById(id: Long): Page? {
        return repository.getPageById(id)
    }

    fun getArchivedSubPages(parentId: Long): Flow<List<Page>> {
        return repository.getArchivedSubPages(parentId)
    }

    fun addPage(pageName: String) {
        viewModelScope.launch {
            if (pages.value.size < 100) {
                repository.insertPage(Page(name = pageName.take(MAX_PAGE_NAME_LENGTH), parentId = null))
            }
        }
    }

    fun addSubPage(parentId: Long, pageName: String) {
        viewModelScope.launch {
            repository.insertPage(Page(name = pageName.take(MAX_PAGE_NAME_LENGTH), parentId = parentId))
        }
    }

    fun updatePageType(page: Page, pageType: PageType) {
        viewModelScope.launch {
            repository.updatePageType(page.id, pageType.ordinal)
        }
    }

    fun savePageName(page: Page, name: String) {
        viewModelScope.launch {
            repository.updatePage(page.copy(name = name.take(MAX_PAGE_NAME_LENGTH)))
        }
    }

    fun updatePageOrder(pages: List<Page>) {
        viewModelScope.launch {
            val updatedPages = pages.mapIndexed { index, page ->
                page.copy(orderAt = index.toLong())
            }
            repository.updatePages(updatedPages)
        }
    }

    fun deletePage(page: Page) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            // Cancel reminders for notes in this page
            val notes = repository.getAllNotesByPage(page.id).first()
            notes.filter { it.reminderEnabled == 1 }.forEach { note ->
                reminderScheduler.cancelReminder(note.uuid)
            }
            // Cancel reminders for notes in sub-pages
            val subPages = repository.getSubPages(page.id).first()
            for (subPage in subPages) {
                val subNotes = repository.getAllNotesByPage(subPage.id).first()
                subNotes.filter { it.reminderEnabled == 1 }.forEach { note ->
                    reminderScheduler.cancelReminder(note.uuid)
                }
            }
            repository.softDeletePage(page.id, now)
        }
    }

    fun archivePage(page: Page) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            // Cancel reminders for notes in this page
            val notes = repository.getAllNotesByPage(page.id).first()
            notes.filter { it.reminderEnabled == 1 }.forEach { note ->
                reminderScheduler.cancelReminder(note.uuid)
            }
            // Cancel reminders for notes in sub-pages
            val subPages = repository.getSubPages(page.id).first()
            for (subPage in subPages) {
                val subNotes = repository.getAllNotesByPage(subPage.id).first()
                subNotes.filter { it.reminderEnabled == 1 }.forEach { note ->
                    reminderScheduler.cancelReminder(note.uuid)
                }
            }
            repository.archivePage(page.id, now)
        }
    }

    fun unarchivePage(page: Page) {
        viewModelScope.launch {
            repository.unarchivePage(page.id)
            reminderScheduler.rescheduleAllReminders()
        }
    }

    private fun loadNotesByPage() {
        viewModelScope.launch {
            repository.getAllNotes().collect { allNotes ->
                val groupedNotes = allNotes.groupBy { it.pageId }
                _notesByPage.emit(groupedNotes)
                _notesCountByPage.emit(groupedNotes.mapValues { (_, notes) -> notes.count { !it.done } })
                _priorityNotesCountByPage.emit(groupedNotes.mapValues { (_, notes) -> notes.count { it.priority > 0 && !it.done } })
            }
        }
    }

    private fun loadSubPages() {
        viewModelScope.launch {
            repository.getAllPages().collect { allPages ->
                val subPagesMap = allPages
                    .filter { it.parentId != null }
                    .groupBy { it.parentId!! }
                _subPagesByParent.emit(subPagesMap)
            }
        }
    }

    fun insertNote(pageId: Long, text: String) {
        viewModelScope.launch {
            repository.insertNote(Note(pageId = pageId, text = text))
        }
    }

    fun insertPriorityNote(pageId: Long, text: String) {
        viewModelScope.launch {
            repository.insertNote(Note(pageId = pageId, text = text, priority = 1))
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
            setEditingNote(null)
        }
    }

    fun moveNoteToPage(note: Note, targetPageId: Long) {
        viewModelScope.launch {
            repository.updateNote(
                note.copy(
                    pageId = targetPageId,
                    orderAt = Clock.System.now().toEpochMilliseconds()
                )
            )
        }
    }

    fun toggleNoteDone(note: Note, isNotesType: Boolean = false) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val newDoneState = !note.done
            val isRepeatingTask = note.repeatFrequency > 0

            // Cancel reminder when marking as done (except for repeating tasks)
            if (newDoneState && note.reminderEnabled == 1 && !isRepeatingTask) {
                reminderScheduler.cancelReminder(note.uuid)
            }

            repository.updateNote(
                note.copy(
                    done = newDoneState,
                    orderAt = if (isNotesType) note.orderAt else now,
                    taskLastDoneAt = if (note.done) note.taskLastDoneAt else now,
                    // Disable reminder when done (except for repeating tasks)
                    reminderEnabled = if (newDoneState && !isRepeatingTask) 0 else note.reminderEnabled
                )
            )
        }
    }

    fun setNoteRepeatFrequency(
        note: Note,
        frequency: RepeatFrequency,
        startDate: Long,
        reminderTime: Long?,
        reminderEnabled: Boolean,
    ) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val updatedNote = note.copy(
                repeatFrequency = frequency.ordinal,
                updatedAt = now,
                repeatTaskStartFrom = if (frequency != RepeatFrequency.NONE) startDate else 0L,
                reminderAt = reminderTime ?: 0L,
                reminderEnabled = if (reminderEnabled && frequency != RepeatFrequency.NONE) 1 else 0
            )
            repository.updateNote(updatedNote)

            // Schedule or cancel reminder based on settings
            if (reminderEnabled && frequency != RepeatFrequency.NONE && reminderTime != null && reminderTime > 0) {
                reminderScheduler.scheduleReminder(updatedNote)
            } else if (!reminderEnabled || frequency == RepeatFrequency.NONE) {
                reminderScheduler.cancelReminder(note.uuid)
            }
        }
    }

    fun resetRepeatingTasksTodo() {
        viewModelScope.launch {
            val completedRepeatingNotes = repository.getCompletedRepeatingNotes()
            val notesToReset = completedRepeatingNotes.filter { note ->
                val frequency = RepeatFrequency.fromOrdinal(note.repeatFrequency)
                note.taskLastDoneAt.hasRepeatIntervalPassed(frequency)
            }
            if (notesToReset.isNotEmpty()) {
                val now = Clock.System.now().toEpochMilliseconds()
                val resetNotes = notesToReset.map { note ->
                    note.copy(
                        done = false,
                        orderAt = now
                    )
                }
                repository.updateNotes(resetNotes)
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            if (note.reminderEnabled == 1) {
                reminderScheduler.cancelReminder(note.uuid)
            }
            val now = Clock.System.now().toEpochMilliseconds()
            repository.softDeleteNote(note.id, now)
        }
    }

    fun moveCompletedTasksToTrash(notes: List<Note>) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            notes.filter { it.done }.forEach { note ->
                if (note.reminderEnabled == 1) {
                    reminderScheduler.cancelReminder(note.uuid)
                }
                repository.softDeleteNote(note.id, now)
            }
        }
    }

    fun setNoteReminder(note: Note, reminderAt: Long, enabled: Boolean) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val updatedNote = note.copy(
                reminderAt = reminderAt,
                reminderEnabled = if (enabled) 1 else 0,
                updatedAt = now
            )
            repository.updateNote(updatedNote)

            if (enabled && reminderAt > now) {
                reminderScheduler.scheduleReminder(updatedNote)
            } else {
                reminderScheduler.cancelReminder(note.uuid)
            }
        }
    }

    fun removeNoteReminder(note: Note) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val updatedNote = note.copy(
                reminderAt = 0,
                reminderEnabled = 0,
                updatedAt = now
            )
            repository.updateNote(updatedNote)
            reminderScheduler.cancelReminder(note.uuid)
        }
    }

    // Trash operations

    fun restorePage(page: Page) {
        viewModelScope.launch {
            repository.restorePage(page.id)
            reminderScheduler.rescheduleAllReminders()
        }
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            repository.restoreNote(note.id)
            if (note.reminderEnabled == 1 && note.reminderAt > Clock.System.now().toEpochMilliseconds()) {
                reminderScheduler.scheduleReminder(note)
            }
        }
    }

    fun permanentlyDeletePage(page: Page) {
        viewModelScope.launch {
            val notes = repository.getAllNotesByPage(page.id).first()
            notes.filter { it.reminderEnabled == 1 }.forEach { note ->
                reminderScheduler.cancelReminder(note.uuid)
            }
            val subPages = repository.getSubPages(page.id).first()
            for (subPage in subPages) {
                val subNotes = repository.getAllNotesByPage(subPage.id).first()
                subNotes.filter { it.reminderEnabled == 1 }.forEach { note ->
                    reminderScheduler.cancelReminder(note.uuid)
                }
            }
            repository.deletePage(page.id)
        }
    }

    fun permanentlyDeleteNote(note: Note) {
        viewModelScope.launch {
            if (note.reminderEnabled == 1) {
                reminderScheduler.cancelReminder(note.uuid)
            }
            repository.deleteNote(note.id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            // Cancel reminders for all trashed notes
            val trashedNotes = repository.getTrashedNotes().first()
            trashedNotes.filter { it.reminderEnabled == 1 }.forEach { note ->
                reminderScheduler.cancelReminder(note.uuid)
            }
            repository.emptyTrash()
        }
    }

    fun hasNotificationPermission(): Boolean = reminderScheduler.hasNotificationPermission()

    suspend fun requestNotificationPermission(): Boolean = reminderScheduler.requestNotificationPermission()

    fun onRateClicked() {
        viewModelScope.launch {
            _showRateButton.value = false
            dataStoreRepository.rateButtonClicked()
        }
    }

    private fun loadThemeMode() {
        viewModelScope.launch {
            dataStoreRepository.themeMode.collect { ordinal ->
                _themeMode.value = ThemeMode.fromOrdinal(ordinal)
            }
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            _themeMode.value = themeMode
            dataStoreRepository.saveThemeMode(themeMode.ordinal)
        }
    }

    private fun loadShowCompletedTasks() {
        viewModelScope.launch {
            dataStoreRepository.showCompletedTasks.collect { show ->
                _showCompletedTasks.value = show
            }
        }
    }

    fun toggleShowCompletedTasks() {
        viewModelScope.launch {
            val newValue = !_showCompletedTasks.value
            _showCompletedTasks.value = newValue
            dataStoreRepository.setShowCompletedTasks(newValue)
        }
    }

    private fun loadShowSubPages() {
        viewModelScope.launch {
            dataStoreRepository.showSubPages.collect { show ->
                _showSubPages.value = show
            }
        }
    }

    fun toggleShowSubPages() {
        viewModelScope.launch {
            val newValue = !_showSubPages.value
            _showSubPages.value = newValue
            dataStoreRepository.setShowSubPages(newValue)
        }
    }

    private fun checkForRateButton() {
        viewModelScope.launch {
            if (dataStoreRepository.showRateButton.first()
                && dataStoreRepository.firstLaunchTime.first().hasBeenHours(1)
                && repository.getAllNotes().first().size > 10
            ) {
                _showRateButton.value = true
            }
        }
    }

    private fun checkFirstLaunch() {
        viewModelScope.launch {
            if (dataStoreRepository.firstLaunch.first()) {
                dataStoreRepository.setFirstLaunchTime(Clock.System.now().toEpochMilliseconds())
                dataStoreRepository.firstLaunchDone()

                val pageToday = repository.insertPage(Page(name = "Today"))
                repository.insertNote(
                    Note(pageId = pageToday, text = "Install Pentastic!️", done = true, orderAt = 3L)
                )
                repository.insertNote(
                    Note(pageId = pageToday, text = "Double tap to mark a task as done. ✔", orderAt = 2L)
                )
                repository.insertNote(
                    Note(pageId = pageToday, text = "Single tap for menu; long press to reorder.", orderAt = 1L)
                )
                repository.insertNote(
                    Note(pageId = pageToday, text = "And swipe right... because we're a perfect match. 😎", orderAt = 0L)
                )

                val pageReminders = repository.insertPage(Page(name = "Reminders"))
                repository.insertNote(
                    Note(
                        pageId = pageReminders,
                        text = "You can set reminders for your one-off or repeating tasks. Tap on the task to open the menu.",
                    )
                )

                val pageThings = repository.insertPage(Page(name = "Stuff"))
                repository.insertPage(Page(name = "To do", parentId = pageThings))
                repository.insertPage(Page(name = "Read", parentId = pageThings))
                repository.insertPage(Page(name = "Watch", parentId = pageThings))
                repository.insertNote(
                    Note(
                        pageId = pageThings,
                        text = "so that you don't forget to actually do it! 😄",
                    )
                )
                repository.insertNote(
                    Note(
                        pageId = pageThings,
                        text = "A place to note down all the fun stuff you want to do, read, or watch...",
                    )
                )

                val pageNotes = repository.insertPage(Page(name = "Notes", pageType = PageType.NOTES.ordinal))
                repository.insertNote(
                    Note(
                        pageId = pageNotes,
                        text = "This is a Notes page with timestamps for each note. 📝",
                    )
                )
                repository.insertNote(
                    Note(
                        pageId = pageNotes,
                        text = "You can change the type of a page from the Index page.",
                    )
                )

                val pageIdeas = repository.insertPage(Page(name = "Ideas", pageType = PageType.NOTES.ordinal))
                repository.insertNote(
                    Note(
                        pageId = pageIdeas,
                        text = "Scribble your random thoughts and ideas here to revisit later.",
                    )
                )
                repository.insertNote(
                    Note(
                        pageId = pageIdeas,
                        text = " Happy scribbling! :)",
                    )
                )

                val pageDontDo = repository.insertPage(Page(name = "Don't do"))
                repository.insertNote(
                    Note(
                        pageId = pageDontDo,
                        text = "Press BACK to go to the Index page.",
                    )
                )
                repository.insertNote(
                    Note(
                        pageId = pageDontDo,
                        text = "Priority tasks will appear in red. Tap on any task to change priority. 👆",
                    )
                )
                repository.insertNote(
                    Note(
                        pageId = pageDontDo,
                        text = "It's okay... we all need this list. :D",
                        priority = 1,
                    )
                )
            }
        }
    }
}
