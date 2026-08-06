package com.maxreader.app.rsvp

import com.maxreader.app.model.BookData
import com.maxreader.app.model.RsvpWord
import com.maxreader.app.settings.RsvpSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RsvpState(
    val currentWord: RsvpWord? = null,
    val lastWord: RsvpWord? = null,
    val contextWords: List<RsvpWord> = emptyList(),
    val nextWords: List<RsvpWord> = emptyList(),
    val isPlaying: Boolean = false,
    val wordIndex: Int = 0,
    val totalWords: Int = 0,
    val progressPercent: Float = 0f,
    val currentChapterTitle: String = ""
)

class RsvpEngine {

    companion object {
        // Characters inside words that make them harder to read in RSVP
        val SPECIAL_CHARS = setOf(
            '-',        // hyphen (light-greenish, well-known)
            '\u2014',   // em dash —
            '\u2013',   // en dash –
            '\'',       // apostrophe (don't, it's)
            '\u2019',   // right single quote / curly apostrophe '
            '\u2018',   // left single quote '
            '/',        // slash (and/or)
            '.',        // mid-word dot (U.S.A.)
        )
    }

    private val _state = MutableStateFlow(RsvpState())
    val state: StateFlow<RsvpState> = _state.asStateFlow()

    private var book: BookData? = null
    private var allWords: List<RsvpWord> = emptyList()
    private var settings: RsvpSettings = RsvpSettings()

    private var playJob: Job? = null
    private var rampUpStartIndex: Int = 0 // tracks where play() was started for ramp-up
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun loadBook(bookData: BookData, startIndex: Int = 0) {
        book = bookData
        allWords = bookData.allWords
        seekTo(startIndex)
    }

    fun updateSettings(newSettings: RsvpSettings) {
        settings = newSettings
    }

    fun play() {
        if (allWords.isEmpty()) return
        if (_state.value.isPlaying) return

        _state.value = _state.value.copy(isPlaying = true)
        rampUpStartIndex = _state.value.wordIndex
        playJob = scope.launch {
            var idx = _state.value.wordIndex
            while (isActive && idx < allWords.size) {
                val word = allWords[idx]
                val lastWord = if (idx > 0) allWords[idx - 1] else null

                // Build context: last N words up to and including current
                val contextStart = (idx - settings.contextWordCount + 1).coerceAtLeast(0)
                val contextWords = allWords.subList(contextStart, idx + 1)

                val nextEnd = (idx + 1 + settings.nextWordCount).coerceAtMost(allWords.size)
                val nextWords = if (idx + 1 < allWords.size) allWords.subList(idx + 1, nextEnd) else emptyList()

                val chapterTitle = book?.chapters?.getOrNull(word.chapterIndex)?.title ?: ""

                _state.value = RsvpState(
                    currentWord = word,
                    lastWord = lastWord,
                    contextWords = contextWords,
                    nextWords = nextWords,
                    isPlaying = true,
                    wordIndex = idx,
                    totalWords = allWords.size,
                    progressPercent = if (allWords.isNotEmpty()) idx.toFloat() / allWords.size else 0f,
                    currentChapterTitle = chapterTitle
                )

                // Calculate delay
                val targetDelay = 60_000L / settings.wpm

                // Ramp-up: start at 50% speed and linearly reach full speed
                val baseDelay = if (settings.rampUpEnabled) {
                    val wordsSinceStart = idx - rampUpStartIndex
                    if (wordsSinceStart < settings.rampUpDurationWords) {
                        val progress = wordsSinceStart.toFloat() / settings.rampUpDurationWords
                        // Lerp from 2x delay (50% speed) to 1x delay (full speed)
                        (targetDelay * (2f - progress)).toLong()
                    } else targetDelay
                } else targetDelay

                // Adaptive speed: extra time for long/complex words
                val adaptiveDelay = if (settings.adaptiveSpeed) {
                    val len = word.text.length
                    val extraChars = (len - settings.lengthThreshold).coerceAtLeast(0)
                    val lengthPenalty = extraChars * settings.msPerExtraChar

                    // Count special characters: - ' \u2019 \u2018 \u2014 \u2013 / and mid-word dots
                    val specialCount = word.text.count { it in SPECIAL_CHARS }
                    val specialPenalty = specialCount * settings.specialCharPenaltyMs

                    lengthPenalty + specialPenalty
                } else 0L

                val punctuationDelay = when {
                    word.isEndOfSentence -> settings.periodPauseMs
                    word.isEndOfParagraph -> settings.paragraphPauseMs
                    word.endsWithPunctuation -> settings.commaPauseMs
                    else -> 0L
                }

                delay(baseDelay + punctuationDelay + adaptiveDelay)
                idx++
            }

            // Finished
            _state.value = _state.value.copy(isPlaying = false)
        }
    }

    fun pause() {
        playJob?.cancel()
        playJob = null
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) pause() else play()
    }

    fun seekTo(index: Int) {
        val wasPlaying = _state.value.isPlaying
        pause()

        val idx = index.coerceIn(0, (allWords.size - 1).coerceAtLeast(0))
        if (allWords.isEmpty()) return

        val word = allWords[idx]
        val lastWord = if (idx > 0) allWords[idx - 1] else null
        val contextStart = (idx - settings.contextWordCount + 1).coerceAtLeast(0)
        val contextWords = allWords.subList(contextStart, idx + 1)

        val nextEnd = (idx + 1 + settings.nextWordCount).coerceAtMost(allWords.size)
        val nextWords = if (idx + 1 < allWords.size) allWords.subList(idx + 1, nextEnd) else emptyList()

        val chapterTitle = book?.chapters?.getOrNull(word.chapterIndex)?.title ?: ""

        _state.value = RsvpState(
            currentWord = word,
            lastWord = lastWord,
            contextWords = contextWords,
            nextWords = nextWords,
            isPlaying = false,
            wordIndex = idx,
            totalWords = allWords.size,
            progressPercent = idx.toFloat() / allWords.size,
            currentChapterTitle = chapterTitle
        )

        if (wasPlaying) play()
    }

    fun skipForward(words: Int = 10) {
        seekTo(_state.value.wordIndex + words)
    }

    fun skipBackward(words: Int = 10) {
        seekTo(_state.value.wordIndex - words)
    }

    fun destroy() {
        playJob?.cancel()
        scope.cancel()
    }
}
