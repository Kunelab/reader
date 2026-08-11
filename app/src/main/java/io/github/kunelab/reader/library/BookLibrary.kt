package io.github.kunelab.reader.library

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest

private const val TAG = "BookLibrary"

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

/**
 * On-disk store for the book list and per-book bookmarks.
 *
 * Every disk access happens on [Dispatchers.IO] and writes are serialised through
 * [writeMutex], so concurrent saves cannot interleave and produce a half-written file.
 */
class BookLibrary(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private val file: File get() = File(context.filesDir, "library.json")
    private val bookmarksDir: File get() = File(context.filesDir, "bookmarks")

    private val writeMutex = Mutex()

    private val _books = MutableStateFlow<List<LibraryBook>>(emptyList())
    val books: Flow<List<LibraryBook>> = _books.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: Flow<List<Bookmark>> = _bookmarks.asStateFlow()

    /** Reads the persisted library into memory. Must be called once before the list is used. */
    suspend fun load() {
        _books.value = withContext(Dispatchers.IO) { loadFromDisk() }
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
        persistBooks(list)
    }

    fun getBookProgress(uri: Uri): LibraryBook? {
        return _books.value.find { it.uri == uri.toString() }
    }

    fun removeBook(uri: String) {
        val list = _books.value.toMutableList()
        list.removeAll { it.uri == uri }
        _books.value = list
        persistBooks(list)
        scope.launch(Dispatchers.IO) {
            writeMutex.withLock {
                runCatching { bookmarkFile(uri).delete() }
                    .onFailure { Log.w(TAG, "Could not delete bookmarks for removed book", it) }
            }
        }
    }

    // --- Bookmarks ---

    suspend fun loadBookmarks(uri: Uri) {
        _bookmarks.value = withContext(Dispatchers.IO) { loadBookmarksFromDisk(uri.toString()) }
    }

    fun addBookmark(uri: Uri, wordIndex: Int, label: String, chapterTitle: String) {
        val list = _bookmarks.value.toMutableList()
        list.add(Bookmark(wordIndex = wordIndex, label = label, chapterTitle = chapterTitle))
        _bookmarks.value = list
        persistBookmarks(uri.toString(), list)
    }

    fun removeBookmark(uri: Uri, wordIndex: Int) {
        val list = _bookmarks.value.toMutableList()
        list.removeAll { it.wordIndex == wordIndex }
        _bookmarks.value = list
        persistBookmarks(uri.toString(), list)
    }

    // --- Persistence ---

    private fun persistBooks(books: List<LibraryBook>) {
        scope.launch(Dispatchers.IO) {
            writeMutex.withLock {
                runCatching { writeAtomically(file, booksToJson(books)) }
                    .onFailure { Log.e(TAG, "Failed to save library", it) }
            }
        }
    }

    private fun persistBookmarks(uriKey: String, bookmarks: List<Bookmark>) {
        scope.launch(Dispatchers.IO) {
            writeMutex.withLock {
                runCatching {
                    bookmarksDir.mkdirs()
                    writeAtomically(bookmarkFile(uriKey), bookmarksToJson(bookmarks))
                }.onFailure { Log.e(TAG, "Failed to save bookmarks", it) }
            }
        }
    }

    /**
     * Writes via a temporary file and renames it into place, so an interrupted write
     * leaves the previous contents intact instead of truncating them.
     */
    private fun writeAtomically(target: File, contents: String) {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.writeText(contents)
        if (!tmp.renameTo(target)) {
            // renameTo does not replace on every filesystem; fall back to delete-then-rename.
            target.delete()
            if (!tmp.renameTo(target)) {
                tmp.delete()
                throw IOException("Could not replace ${target.name}")
            }
        }
    }

    /**
     * Bookmarks are stored one file per book, named after a hash of the book's URI.
     * SHA-256 rather than String.hashCode(), whose 32-bit range collides easily enough
     * that two books could overwrite each other's bookmarks.
     */
    private fun bookmarkFile(uriKey: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(uriKey.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(bookmarksDir, "bm_$digest.json")
    }

    private fun booksToJson(books: List<LibraryBook>): String {
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
        return arr.toString()
    }

    private fun bookmarksToJson(bookmarks: List<Bookmark>): String {
        val arr = JSONArray()
        for (bm in bookmarks) {
            arr.put(JSONObject().apply {
                put("wordIndex", bm.wordIndex)
                put("label", bm.label)
                put("chapterTitle", bm.chapterTitle)
                put("timestamp", bm.timestamp)
            })
        }
        return arr.toString()
    }

    private fun loadFromDisk(): List<LibraryBook> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                LibraryBook(
                    uri = obj.getString("uri"),
                    title = obj.optString("title", ""),
                    author = obj.optString("author", ""),
                    lastWordIndex = obj.optInt("lastWordIndex", 0),
                    totalWords = obj.optInt("totalWords", 0),
                    lastChapterTitle = obj.optString("lastChapterTitle", ""),
                    lastOpenedTimestamp = obj.optLong("lastOpenedTimestamp", 0)
                )
            }
        } catch (e: JSONException) {
            Log.e(TAG, "library.json is corrupt, starting with an empty library", e)
            emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "Could not read library.json", e)
            emptyList()
        }
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
        } catch (e: JSONException) {
            Log.e(TAG, "Bookmark file is corrupt", e)
            emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "Could not read bookmark file", e)
            emptyList()
        }
    }
}
