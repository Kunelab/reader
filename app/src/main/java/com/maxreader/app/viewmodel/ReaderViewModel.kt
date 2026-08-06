package com.maxreader.app.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maxreader.app.R
import com.maxreader.app.epub.EpubException
import com.maxreader.app.epub.EpubParser
import com.maxreader.app.library.Bookmark
import com.maxreader.app.library.BookLibrary
import com.maxreader.app.library.LibraryBook
import com.maxreader.app.model.BookData
import com.maxreader.app.rsvp.RsvpEngine
import com.maxreader.app.rsvp.RsvpState
import com.maxreader.app.settings.RsvpSettings
import com.maxreader.app.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

private const val TAG = "ReaderViewModel"

/**
 * Supplies wording for anything the EPUB itself did not name. The parser leaves these
 * blank so that it can stay free of Android resources.
 */
private fun BookData.withDisplayNames(app: Application): BookData = copy(
    title = title.ifBlank { app.getString(R.string.unknown_title) },
    author = author.ifBlank { app.getString(R.string.unknown_author) },
    chapters = chapters.mapIndexed { index, chapter ->
        if (chapter.title.isNotBlank()) chapter
        else chapter.copy(title = app.getString(R.string.chapter_default, index + 1))
    }
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val engine = RsvpEngine()
    private val library = BookLibrary(application, viewModelScope)

    private val _bookData = MutableStateFlow<BookData?>(null)
    val bookData: StateFlow<BookData?> = _bookData.asStateFlow()

    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    private var currentUri: Uri? = null
    private var loadJob: Job? = null

    val rsvpState: StateFlow<RsvpState> = engine.state

    // The full state changes on every word. These slices change far less often, so the
    // chrome that reads them is not dragged into 25 recompositions a second at high WPM.
    val currentChapterTitle: StateFlow<String> = engine.state
        .map { it.currentChapterTitle }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val isPlaying: StateFlow<Boolean> = engine.state
        .map { it.isPlaying }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val settings: StateFlow<RsvpSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, RsvpSettings())

    val libraryBooks: StateFlow<List<LibraryBook>> = library.books
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val bookmarks: StateFlow<List<Bookmark>> = library.bookmarks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { library.load() }
        viewModelScope.launch {
            settings.collect { newSettings ->
                engine.updateSettings(newSettings)
            }
        }
    }

    /**
     * Opens a book the user picked, or one handed to us by another app. Takes a lasting
     * hold on the read permission so the book can still be reopened from the library
     * after the process is restarted.
     */
    fun openBook(uri: Uri) {
        try {
            getApplication<Application>().contentResolver
                .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            // Some providers do not offer a persistable grant. The book still opens now;
            // it just may not be reopenable from the library later.
            Log.w(TAG, "Could not persist read access to $uri", e)
        }
        loadBook(uri)
    }

    fun loadBook(uri: Uri) {
        // A second tap while a book is still parsing would race the first one.
        if (loadJob?.isActive == true) return

        loadJob = viewModelScope.launch(Dispatchers.IO) {
            _loadingState.value = LoadingState.Loading
            val app = getApplication<Application>()
            try {
                val parsed = app.contentResolver.openInputStream(uri)?.use { EpubParser.parse(it) }
                    ?: throw IOException("Provider returned no stream for $uri")
                val book = parsed.withDisplayNames(app)

                _bookData.value = book
                currentUri = uri

                // Check if we have a saved position for this book
                val savedProgress = library.getBookProgress(uri)
                val startIndex = savedProgress?.lastWordIndex ?: 0

                engine.loadBook(book, startIndex)
                library.loadBookmarks(uri)
                _loadingState.value = LoadingState.Success
            } catch (e: CancellationException) {
                throw e // never report cancellation as a failure
            } catch (e: SecurityException) {
                Log.w(TAG, "Lost access to $uri", e)
                _loadingState.value = LoadingState.Error(app.getString(R.string.error_access_lost))
            } catch (e: EpubException.MissingContent) {
                Log.w(TAG, "Damaged EPUB at $uri", e)
                _loadingState.value =
                    LoadingState.Error(app.getString(R.string.error_epub_missing_content))
            } catch (e: EpubException) {
                Log.w(TAG, "Not a usable EPUB at $uri", e)
                _loadingState.value =
                    LoadingState.Error(app.getString(R.string.error_not_valid_epub))
            } catch (e: IOException) {
                Log.w(TAG, "Could not read $uri", e)
                _loadingState.value =
                    LoadingState.Error(app.getString(R.string.error_cannot_open_file))
            } catch (e: Exception) {
                // Details go to logcat rather than the screen: exception text can carry
                // full content:// paths, and it is not phrased for a reader anyway.
                Log.e(TAG, "Failed to load $uri", e)
                _loadingState.value = LoadingState.Error(app.getString(R.string.error_load_failed))
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
        // Hand the grant back; the system caps how many an app may hold.
        try {
            getApplication<Application>().contentResolver.releasePersistableUriPermission(
                Uri.parse(uriString),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "No persisted permission to release for $uriString", e)
        }
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
