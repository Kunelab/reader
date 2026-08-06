package com.maxreader.app.library

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class LibraryBook(
    val uri: String,
    val title: String,
    val author: String,
    val lastWordIndex: Int = 0,
    val totalWords: Int = 0,
    val lastChapterTitle: String = "",
    val lastOpenedTimestamp: Long = System.currentTimeMillis()
) {
    val progressPercent: Float
        get() = if (totalWords > 0) lastWordIndex.toFloat() / totalWords else 0f
}

data class Bookmark(
    val wordIndex: Int,
    val label: String,
    val chapterTitle: String,
    val timestamp: Long = System.currentTimeMillis()
)

class BookLibrary(private val context: Context) {

    private val file: File get() = File(context.filesDir, "library.json")
    private val bookmarksDir: File get() = File(context.filesDir, "bookmarks").also { it.mkdirs() }

    private val _books = MutableStateFlow<List<LibraryBook>>(emptyList())
    val books: Flow<List<LibraryBook>> = _books.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: Flow<List<Bookmark>> = _bookmarks.asStateFlow()

    init {
        _books.value = loadFromDisk()
    }

    fun getBooks(): List<LibraryBook> = _books.value

    fun saveBookProgress(
        uri: Uri,
        title: String,
        author: String,
        wordIndex: Int,
        totalWords: Int,
        chapterTitle: String
    ) {
        val list = _books.value.toMutableList()
        val existing = list.indexOfFirst { it.uri == uri.toString() }
        val book = LibraryBook(
            uri = uri.toString(),
            title = title,
            author = author,
            lastWordIndex = wordIndex,
            totalWords = totalWords,
            lastChapterTitle = chapterTitle,
            lastOpenedTimestamp = System.currentTimeMillis()
        )
        if (existing >= 0) {
            list[existing] = book
        } else {
            list.add(0, book)
        }
        _books.value = list
        saveToDisk(list)
    }

    fun getBookProgress(uri: Uri): LibraryBook? {
        return _books.value.find { it.uri == uri.toString() }
    }

    fun removeBook(uri: String) {
        val list = _books.value.toMutableList()
        list.removeAll { it.uri == uri }
        _books.value = list
        saveToDisk(list)
    }

    // --- Bookmarks ---

    fun loadBookmarks(uri: Uri) {
        _bookmarks.value = loadBookmarksFromDisk(uri.toString())
    }

    fun addBookmark(uri: Uri, wordIndex: Int, label: String, chapterTitle: String) {
        val key = uri.toString()
        val list = _bookmarks.value.toMutableList()
        list.add(Bookmark(wordIndex = wordIndex, label = label, chapterTitle = chapterTitle))
        _bookmarks.value = list
        saveBookmarksToDisk(key, list)
    }

    fun removeBookmark(uri: Uri, wordIndex: Int) {
        val key = uri.toString()
        val list = _bookmarks.value.toMutableList()
        list.removeAll { it.wordIndex == wordIndex }
        _bookmarks.value = list
        saveBookmarksToDisk(key, list)
    }

    private fun bookmarkFile(uriKey: String): File {
        val safeKey = uriKey.hashCode().toString()
        return File(bookmarksDir, "bm_$safeKey.json")
    }

    private fun saveBookmarksToDisk(uriKey: String, bookmarks: List<Bookmark>) {
        val arr = JSONArray()
        for (bm in bookmarks) {
            arr.put(JSONObject().apply {
                put("wordIndex", bm.wordIndex)
                put("label", bm.label)
                put("chapterTitle", bm.chapterTitle)
                put("timestamp", bm.timestamp)
            })
        }
        bookmarkFile(uriKey).writeText(arr.toString())
    }

    private fun loadBookmarksFromDisk(uriKey: String): List<Bookmark> {
        val f = bookmarkFile(uriKey)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Bookmark(
                    wordIndex = obj.getInt("wordIndex"),
                    label = obj.optString("label", ""),
                    chapterTitle = obj.optString("chapterTitle", ""),
                    timestamp = obj.optLong("timestamp", 0)
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun saveToDisk(books: List<LibraryBook>) {
        val arr = JSONArray()
        for (book in books) {
            arr.put(JSONObject().apply {
                put("uri", book.uri)
                put("title", book.title)
                put("author", book.author)
                put("lastWordIndex", book.lastWordIndex)
                put("totalWords", book.totalWords)
                put("lastChapterTitle", book.lastChapterTitle)
                put("lastOpenedTimestamp", book.lastOpenedTimestamp)
            })
        }
        file.writeText(arr.toString())
    }

    private fun loadFromDisk(): List<LibraryBook> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                LibraryBook(
                    uri = obj.getString("uri"),
                    title = obj.optString("title", "Unknown"),
                    author = obj.optString("author", "Unknown"),
                    lastWordIndex = obj.optInt("lastWordIndex", 0),
                    totalWords = obj.optInt("totalWords", 0),
                    lastChapterTitle = obj.optString("lastChapterTitle", ""),
                    lastOpenedTimestamp = obj.optLong("lastOpenedTimestamp", 0)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
