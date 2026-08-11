package io.github.kunelab.reader.model

/**
 * A single word to be displayed in RSVP mode.
 */
data class RsvpWord(
    val text: String,
    val orpIndex: Int,       // Optimal Recognition Point index (the bold char)
    val endsWithPunctuation: Boolean,
    val isEndOfSentence: Boolean,
    val isEndOfParagraph: Boolean,
    val globalIndex: Int,    // index in the full word list
    val chapterIndex: Int
)

data class BookChapter(
    val title: String,
    val words: List<RsvpWord>,
    val isContentChapter: Boolean = true // false for cover, TOC, copyright, etc.
)

data class BookData(
    val title: String,
    val author: String,
    val chapters: List<BookChapter>
) {
    val allWords: List<RsvpWord> by lazy {
        chapters.flatMap { it.words }
    }
}
