package app.pentastic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pentastic.data.DataStoreRepository
import app.pentastic.data.MyRepository
import app.pentastic.data.Note
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

//    val currentPage: StateFlow<Int> = runBlocking {
//        val initialPage = dataStoreRepository.currentPage.first()
//        dataStoreRepository.currentPage.stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = initialPage
//        )
//    }
//
//    fun saveCurrentPage(pageNumber: Int) {
//        viewModelScope.launch {
//            dataStoreRepository.saveCurrentPage(pageNumber)
//        }
//    }

    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())
    val allNotes: StateFlow<List<Note>> = _allNotes.asStateFlow()

    private val _notesByPage = MutableStateFlow<Map<Int, List<Note>>>(emptyMap())
    val notesByPage: StateFlow<Map<Int, List<Note>>> = _notesByPage.asStateFlow()

    private val _notesCountByPage = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val notesCountByPage: StateFlow<Map<Int, Int>> = _notesCountByPage.asStateFlow()

    private val _priorityNotesCountByPage = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val priorityNotesCountByPage: StateFlow<Map<Int, Int>> = _priorityNotesCountByPage.asStateFlow()

    val pageNames: StateFlow<Map<Int, String>> = dataStoreRepository.pageNames.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun savePageNames(page: Int, name: String) {
        val updatedMap = pageNames.value.toMutableMap()
        updatedMap[page] = name
        viewModelScope.launch {
            dataStoreRepository.savePageNames(updatedMap)
        }
    }

    private fun loadNotesByPage() {
        viewModelScope.launch {
            repository.getAllNotes().collect { allNotes ->
                val groupedNotes = allNotes.groupBy { it.page }
                _allNotes.emit(allNotes)
                _notesByPage.emit(groupedNotes)
                _notesCountByPage.emit(groupedNotes.mapValues { (_, notes) -> notes.count { !it.done } })
                _priorityNotesCountByPage.emit(groupedNotes.mapValues { (_, notes) -> notes.count { it.priority > 0 && !it.done } })
            }
        }
    }

    fun insertNote(page: Int, text: String) {
        viewModelScope.launch {
            repository.insertNote(Note(page = page, text = text))
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
                repository.insertNote(Note(page = 1, text = "Welcome to Pentastic! 🖊️", orderAt = 3L))
                repository.insertNote(Note(page = 1, text = "Double tap a task to mark it as done. ✔", orderAt = 2L))
                repository.insertNote(Note(page = 1, text = "Single tap, long press or swipe for more...", orderAt = 1L))

                repository.insertNote(Note(page = 2, text = "Tip: You can long press the page names on the index page to rename them.", orderAt = 4L))
                repository.insertNote(Note(page = 9, text = "Write down your new year resolution before you forget them :D", orderAt = 5L))
                repository.insertNote(Note(page = 10, text = "3 big things that you want to accomplish in your lifetime.", orderAt = 6L))

                renamePages()
                dataStoreRepository.firstLaunchDone()
            }
        }
    }

    fun renamePages() {
        val updatedMap = pageNames.value.toMutableMap()
        updatedMap[1] = "Today"
        updatedMap[2] = "Later"
        updatedMap[9] = "2026"
        updatedMap[10] = "Life goals"
        viewModelScope.launch {
            dataStoreRepository.savePageNames(updatedMap)
        }
    }
}