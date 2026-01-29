package app.pentastic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pentastic.data.DataStoreRepository
import app.pentastic.data.MyRepository
import app.pentastic.data.Note
import app.pentastic.data.Page
import app.pentastic.data.RepeatFrequency
import app.pentastic.data.ThemeMode
import app.pentastic.utils.calendarDaysSince
import app.pentastic.utils.hasBeenHours
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
) : ViewModel() {

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

    private val _editingNote = MutableStateFlow<Note?>(null)
    val editingNote: StateFlow<Note?> = _editingNote.asStateFlow()

    val pages: StateFlow<List<Page>> = repository.getAllPages().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        checkFirstLaunch()
        loadNotesByPage()
        checkForRateButton()
        loadThemeMode()
    }

    fun setEditingNote(note: Note?) {
        _editingNote.value = note
    }

    fun addPage(pageName: String) {
        viewModelScope.launch {
            if (pages.value.size < 100) {
                repository.insertPage(Page(name = pageName))
            }
        }
    }

    fun savePageName(page: Page, name: String) {
        viewModelScope.launch {
            repository.updatePage(page.copy(name = name))
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
            repository.deletePage(page.id)
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

    fun insertNote(pageId: Long, text: String) {
        viewModelScope.launch {
            repository.insertNote(Note(pageId = pageId, text = text))
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
            setEditingNote(null)
        }
    }

    fun toggleNoteDone(note: Note) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            repository.updateNote(
                note.copy(
                    done = !note.done,
                    orderAt = now,
                    taskLastDoneAt = if (note.done) note.taskLastDoneAt else now
                )
            )
        }
    }

    fun setNoteRepeatFrequency(note: Note, frequency: RepeatFrequency) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            repository.updateNote(
                note.copy(
                    repeatFrequency = frequency.ordinal,
                    updatedAt = now,
                    repeatTaskStartFrom = if (frequency != RepeatFrequency.NONE) now else 0L
                )
            )
        }
    }

    fun resetRepeatingTasksTodo() {
        viewModelScope.launch {
            val completedRepeatingNotes = repository.getCompletedRepeatingNotes()
            val notesToReset = completedRepeatingNotes.filter { note ->
                val frequency = RepeatFrequency.fromOrdinal(note.repeatFrequency)
                note.taskLastDoneAt.calendarDaysSince() >= frequency.days
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
        viewModelScope.launch { repository.deleteNote(note.id) }
    }

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

                val page1 = repository.insertPage(Page(name = "Today"))
                val page2 = repository.insertPage(Page(name = "Later"))
                repository.insertPage(Page(name = "Page 3"))
                repository.insertPage(Page(name = "Page 4"))
                repository.insertPage(Page(name = "Page 5"))
                repository.insertPage(Page(name = "Page 6"))
                val page7 = repository.insertPage(Page(name = "Don't do"))
                val page8 = repository.insertPage(Page(name = "Quotes"))
                val page9 = repository.insertPage(Page(name = "2026"))
                val page10 = repository.insertPage(Page(name = "YOLO"))

                repository.insertNote(Note(pageId = page1, text = "Install Pentastic!️", done = true, orderAt = 3L))
                repository.insertNote(Note(pageId = page1, text = "Double tap to mark a task as done. ✔", orderAt = 2L))
                repository.insertNote(Note(pageId = page1, text = "Single tap for menu; long press to reorder.", orderAt = 1L))
                repository.insertNote(Note(pageId = page1, text = "And swipe right... because we're a perfect match. 😎", orderAt = 0L))

                repository.insertNote(
                    Note(
                        pageId = page2,
                        text = "Tip: You can long press the page names on the index page to rename them.",
                    )
                )
                repository.insertNote(
                    Note(
                        pageId = page7,
                        text = "You know you need this list. We all do. 😁",
                        priority = 1,
                    )
                )
                repository.insertNote(
                    Note(
                        pageId = page8,
                        text = "I highly recommend using lists. I make lists of what I want to accomplish each year, each month, and each day. Lists are very focusing, and they help me with multitasking because I don’t have to keep as much in my head. — Sam Altman",
                    )
                )
                repository.insertNote(
                    Note(
                        pageId = page8,
                        text = "The person who writes down the thing has tremendous power. — Mark Andreessen",
                    )
                )
                repository.insertNote(
                    Note(
                        pageId = page8,
                        text = "You mind is for having ideas, not holding them. — David Allen",
                    )
                )
                repository.insertNote(
                    Note(
                        pageId = page9,
                        text = "Write down your New Year's resolutions before you forget them, like last year. :D",
                    )
                )
                repository.insertNote(
                    Note(
                        pageId = page10,
                        text = "Write down 3 big things you want to accomplish in your lifetime and just! do! it!",
                    )
                )

                dataStoreRepository.firstLaunchDone()
                dataStoreRepository.pagesReordered()
            } else if (!dataStoreRepository.pagesReordered.first()) {
                pages.collect {
                    if (it.isNotEmpty()) {
                        val updatedPages = it.mapIndexed { index, page ->
                            page.copy(orderAt = index.toLong())
                        }
                        repository.updatePages(updatedPages)
                        dataStoreRepository.pagesReordered()
                    }
                }
            }
        }
    }
}
