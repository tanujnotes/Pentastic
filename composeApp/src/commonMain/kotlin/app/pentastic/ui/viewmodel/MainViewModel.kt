package app.pentastic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pentastic.data.DataStoreRepository
import app.pentastic.data.MyRepository
import app.pentastic.data.Note
import app.pentastic.data.Page
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

    init {
        checkFirstLaunch()
        loadNotesByPage()
    }

    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())
    val allNotes: StateFlow<List<Note>> = _allNotes.asStateFlow()

    private val _notesByPage = MutableStateFlow<Map<Long, List<Note>>>(emptyMap())
    val notesByPage: StateFlow<Map<Long, List<Note>>> = _notesByPage.asStateFlow()

    private val _notesCountByPage = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val notesCountByPage: StateFlow<Map<Long, Int>> = _notesCountByPage.asStateFlow()

    private val _priorityNotesCountByPage = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val priorityNotesCountByPage: StateFlow<Map<Long, Int>> = _priorityNotesCountByPage.asStateFlow()

    val pages: StateFlow<List<Page>> = repository.getAllPages().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addPage(pageName: String) {
        viewModelScope.launch {
            repository.insertPage(Page(name = pageName))
        }
    }

    fun savePageName(page: Page, name: String) {
        viewModelScope.launch {
            repository.updatePage(page.copy(name = name))
        }
    }

    fun updatePageOrder(pages: List<Page>) {
        viewModelScope.launch {
            val baseTime = Clock.System.now().toEpochMilliseconds()

            pages.forEachIndexed { index, page ->
                repository.updatePage(page.copy(orderAt = baseTime - index))
            }
        }
    }

    private fun loadNotesByPage() {
        viewModelScope.launch {
            repository.getAllNotes().collect { allNotes ->
                val groupedNotes = allNotes.groupBy { it.pageId }
                _allNotes.emit(allNotes)
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
        }
    }

    fun toggleNoteDone(note: Note) {
        viewModelScope.launch {
            repository.updateNote(
                note.copy(
                    done = !note.done,
                    priority = 0,
                    orderAt = Clock.System.now().toEpochMilliseconds()
                )
            )
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { repository.deleteNote(note.id) }
    }

    private fun checkFirstLaunch() {
        viewModelScope.launch {
            if (dataStoreRepository.firstLaunch.first()) {
                val page1 = repository.insertPage(Page(name = "Today"))
                val page2 = repository.insertPage(Page(name = "Later"))
                val page3 = repository.insertPage(Page(name = "Page 3"))
                val page4 = repository.insertPage(Page(name = "Page 4"))
                val page5 = repository.insertPage(Page(name = "Page 5"))
                val page6 = repository.insertPage(Page(name = "Page 6"))
                val page7 = repository.insertPage(Page(name = "Page 7"))
                val page8 = repository.insertPage(Page(name = "Page 8"))
                val page9 = repository.insertPage(Page(name = "2026"))
                val page10 = repository.insertPage(Page(name = "Life goals"))

                repository.insertNote(Note(pageId = page1, text = "Install Pentastic!️", done = true, orderAt = 3L))
                repository.insertNote(Note(pageId = page1, text = "Double tap to mark a task as done. ✔", orderAt = 2L))
                repository.insertNote(Note(pageId = page1, text = "Single tap for menu; long press to reorder.", orderAt = 1L))
                repository.insertNote(Note(pageId = page1, text = "And swipe right... because we're a perfect match. 😎", orderAt = 0L))

                repository.insertNote(
                    Note(
                        pageId = page2,
                        text = "Tip: You can long press the page names on the index page to rename them.",
                        orderAt = 4L
                    )
                )
                repository.insertNote(
                    Note(
                        pageId = page9,
                        text = "Write down your New Year's resolutions before you forget them, like last year. :D",
                        orderAt = 5L
                    )
                )
                repository.insertNote(
                    Note(
                        pageId = page10,
                        text = "Write down five big things you want to accomplish in your lifetime. Do it!",
                        orderAt = 6L
                    )
                )

                dataStoreRepository.firstLaunchDone()
                dataStoreRepository.pagesReordered()
            } else if (!dataStoreRepository.pagesReordered.first()) {
                pages.collect {
                    if (it.isNotEmpty()) {
                        it.forEach { page ->
                            repository.updatePage(page.copy(orderAt = 0))
                        }
                        dataStoreRepository.pagesReordered()
                    }
                }
            }
        }
    }
}
