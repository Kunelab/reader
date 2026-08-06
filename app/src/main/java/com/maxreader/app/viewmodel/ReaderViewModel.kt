package com.maxreader.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maxreader.app.epub.EpubParser
import com.maxreader.app.library.Bookmark
import com.maxreader.app.library.BookLibrary
import com.maxreader.app.library.LibraryBook
import com.maxreader.app.model.BookData
import com.maxreader.app.rsvp.RsvpEngine
import com.maxreader.app.rsvp.RsvpState
import com.maxreader.app.settings.RsvpSettings
import com.maxreader.app.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepo = SettingsRepository(application)
    val engine = RsvpEngine()
    val library = BookLibrary(application)

    private val _bookData = MutableStateFlow<BookData?>(null)
    val bookData: StateFlow<BookData?> = _bookData.asStateFlow()

    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    private var currentUri: Uri? = null

    val rsvpState: StateFlow<RsvpState> = engine.state

    val settings: StateFlow<RsvpSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, RsvpSettings())

    val libraryBooks: StateFlow<List<LibraryBook>> = library.books
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val bookmarks: StateFlow<List<Bookmark>> = library.bookmarks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            settings.collect { newSettings ->
                engine.updateSettings(newSettings)
            }
        }
    }

    fun loadBook(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.value = LoadingState.Loading
            try {
                val book = EpubParser.parse(getApplication(), uri)
                _bookData.value = book
                currentUri = uri

                // Check if we have a saved position for this book
                val savedProgress = library.getBookProgress(uri)
                val startIndex = savedProgress?.lastWordIndex ?: 0

                engine.loadBook(book, startIndex)
                library.loadBookmarks(uri)
                _loadingState.value = LoadingState.Success
            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error(e.message ?: "Failed to load book")
            }
        }
    }

    fun saveCurrentProgress() {
        val uri = currentUri ?: return
        val book = _bookData.value ?: return
        val state = rsvpState.value
        library.saveBookProgress(
            uri = uri,
            title = book.title,
            author = book.author,
            wordIndex = state.wordIndex,
            totalWords = state.totalWords,
            chapterTitle = state.currentChapterTitle
        )
    }

    fun removeLibraryBook(uriString: String) {
        library.removeBook(uriString)
    }

    fun resetLoadingState() {
        _loadingState.value = LoadingState.Idle
    }

    fun play() = engine.play()
    fun pause() {
        engine.pause()
        saveCurrentProgress()
    }
    fun togglePlayPause() {
        if (rsvpState.value.isPlaying) pause() else engine.play()
    }
    fun seekTo(index: Int) = engine.seekTo(index)
    fun skipForward() = engine.skipForward()
    fun skipBackward() = engine.skipBackward()

    // Settings updates
    fun setWpm(wpm: Int) = viewModelScope.launch { settingsRepo.updateWpm(wpm) }
    fun setCommaPause(ms: Long) = viewModelScope.launch { settingsRepo.updateCommaPause(ms) }
    fun setPeriodPause(ms: Long) = viewModelScope.launch { settingsRepo.updatePeriodPause(ms) }
    fun setParagraphPause(ms: Long) = viewModelScope.launch { settingsRepo.updateParagraphPause(ms) }
    fun setContextWordCount(count: Int) = viewModelScope.launch { settingsRepo.updateContextWordCount(count) }
    fun setNextWordCount(count: Int) = viewModelScope.launch { settingsRepo.updateNextWordCount(count) }
    fun setShowContext(show: Boolean) = viewModelScope.launch { settingsRepo.updateShowContext(show) }
    fun setAdaptiveSpeed(enabled: Boolean) = viewModelScope.launch { settingsRepo.updateAdaptiveSpeed(enabled) }
    fun setLengthThreshold(chars: Int) = viewModelScope.launch { settingsRepo.updateLengthThreshold(chars) }
    fun setMsPerExtraChar(ms: Long) = viewModelScope.launch { settingsRepo.updateMsPerExtraChar(ms) }
    fun setSpecialCharPenalty(ms: Long) = viewModelScope.launch { settingsRepo.updateSpecialCharPenalty(ms) }
    fun setFontSize(size: Int) = viewModelScope.launch { settingsRepo.updateFontSize(size) }
    fun setRampUpEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepo.updateRampUpEnabled(enabled) }
    fun setRampUpDuration(words: Int) = viewModelScope.launch { settingsRepo.updateRampUpDuration(words) }
    fun setTheme(theme: String) = viewModelScope.launch { settingsRepo.updateTheme(theme) }
    fun setFontFamily(family: String) = viewModelScope.launch { settingsRepo.updateFontFamily(family) }
    fun setLetterSpacing(sp: Float) = viewModelScope.launch { settingsRepo.updateLetterSpacing(sp) }
    fun setContextLineSpacing(sp: Float) = viewModelScope.launch { settingsRepo.updateContextLineSpacing(sp) }
    fun setContextMargin(dp: Int) = viewModelScope.launch { settingsRepo.updateContextMargin(dp) }

    fun addBookmark(label: String) {
        val uri = currentUri ?: return
        val state = rsvpState.value
        library.addBookmark(uri, state.wordIndex, label, state.currentChapterTitle)
    }

    fun removeBookmark(wordIndex: Int) {
        val uri = currentUri ?: return
        library.removeBookmark(uri, wordIndex)
    }

    override fun onCleared() {
        super.onCleared()
        saveCurrentProgress()
        engine.destroy()
    }
}

sealed class LoadingState {
    data object Idle : LoadingState()
    data object Loading : LoadingState()
    data object Success : LoadingState()
    data class Error(val message: String) : LoadingState()
}
