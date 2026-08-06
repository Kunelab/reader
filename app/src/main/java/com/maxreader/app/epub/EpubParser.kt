package com.maxreader.app.epub

import androidx.annotation.VisibleForTesting
import com.maxreader.app.model.BookChapter
import com.maxreader.app.model.BookData
import com.maxreader.app.model.RsvpWord
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Turns an EPUB archive into a flat stream of [RsvpWord]s.
 *
 * Plain Kotlin on purpose — no Context, no resources — so the tokenizing rules below can
 * be unit tested on the JVM. Titles that the file does not supply come back blank; the
 * caller substitutes whatever wording it wants to show.
 */
object EpubParser {

    private val PUNCTUATION_PAUSE = setOf(',', ';', ':')
    private val SENTENCE_END = setOf('.', '!', '?')

    // Known abbreviations that end with '.' but are NOT sentence endings
    // Stored lowercase for case-insensitive matching
    private val ABBREVIATIONS = setOf(
        // Titles (EN + FR)
        "mr.", "mrs.", "ms.", "dr.", "prof.", "rev.", "gen.", "gov.",
        "sgt.", "cpl.", "pvt.", "lt.", "col.", "capt.", "cmdr.", "adm.",
        "jr.", "sr.", "st.", "hon.",
        "m.", "mme.", "mlle.", "mgr.",
        // Latin / academic
        "etc.", "vs.", "e.g.", "i.e.", "al.", "approx.", "dept.", "est.",
        "fig.", "vol.", "no.", "p.", "pp.", "ch.", "ed.", "assn.",
        // Time / measurement
        "a.m.", "p.m.", "jan.", "feb.", "mar.", "apr.", "jun.",
        "jul.", "aug.", "sep.", "sept.", "oct.", "nov.", "dec.",
        "ave.", "blvd.", "rd.", "ft.", "mt.", "inc.", "ltd.", "corp.",
        // French common
        "av.", "bd.", "cf.", "env.", "hab.", "min.", "sec.",
    )

    /**
     * Returns true if the word looks like an abbreviation rather than a sentence end.
     * Checks: known abbreviation list, single-letter initial (A. B. M.),
     * multiple internal dots (U.S.A.), and next-word-starts-lowercase heuristic.
     */
    @VisibleForTesting
    internal fun isAbbreviation(word: String, nextWord: String?): Boolean {
        val stripped = word.trimEnd('"', '\'', '\u201D', '\u2019', ')', ']')
        val lower = stripped.lowercase()

        // Known abbreviation
        if (lower in ABBREVIATIONS) return true

        // Single letter + dot: likely an initial (M. J. etc.)
        if (stripped.length == 2 && stripped[0].isLetter() && stripped[1] == '.') return true

        // Multiple dots inside the word: U.S.A., a.m., etc.
        if (stripped.count { it == '.' } >= 2) return true

        // Next word starts with lowercase → probably not a sentence boundary
        if (nextWord != null) {
            val firstLetter = nextWord.firstOrNull { it.isLetter() }
            if (firstLetter != null && firstLetter.isLowerCase()) return true
        }

        return false
    }

    // Short units that naturally pair with a preceding number
    private val SHORT_UNITS = setOf(
        // Metric
        "km", "m", "cm", "mm", "kg", "g", "mg", "l", "ml", "dl",
        "km/h", "m/s",
        // Time
        "h", "min", "s", "ms",
        // Other
        "%", "€", "$", "£", "¥",
        // Imperial
        "ft", "in", "lb", "oz", "mi", "yd",
    )

    // Lone currency/number symbols that should attach to the next token
    private val CURRENCY_SYMBOLS = setOf("$", "€", "£", "¥", "n°", "N°", "#")

    /**
     * Decides whether two adjacent tokens should be merged into one for RSVP display.
     */
    @VisibleForTesting
    internal fun shouldMerge(current: String, next: String): Boolean {
        // 1. Abbreviation + following word: "M." + "Sarkozy" → "M. Sarkozy"
        if (current.lastOrNull() == '.' && isAbbreviation(current, next)) return true

        // 2. Single-character words merge with next (too fast to read alone)
        //    "a" + "book", "I" + "am", "à" + "Paris", "y" + "compris", "ô" + "rage"
        if (current.length == 1 && current[0].isLetter()) return true

        // 3. Number + short unit: "10" + "km", "5" + "kg", "100" + "%"
        val isNumber = current.all { it.isDigit() || it == ',' || it == '.' || it == '\u00A0' }
        if (isNumber && current.any { it.isDigit() } && next.lowercase() in SHORT_UNITS) return true

        // 4. Currency/symbol prefix + number: "$" + "10", "n°" + "5", "#" + "42"
        if (current in CURRENCY_SYMBOLS && next.firstOrNull()?.isDigit() == true) return true

        return false
    }

    fun parse(input: InputStream): BookData {
        val entries = readZipEntries(input)
        return parseEpub(entries)
    }

    private fun readZipEntries(stream: InputStream): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        val zis = ZipInputStream(stream)
        var entry: ZipEntry? = zis.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                entries[entry.name] = zis.readBytes()
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
        zis.close()
        return entries
    }

    private fun parseEpub(entries: Map<String, ByteArray>): BookData {
        val containerXml = entries["META-INF/container.xml"]
            ?: throw EpubException.NotAnEpub("no META-INF/container.xml")

        val opfPath = parseContainerXml(String(containerXml, Charsets.UTF_8))
        val opfDir = opfPath.substringBeforeLast('/', "")

        val opfContent = entries[opfPath]
            ?: throw EpubException.MissingContent(opfPath)

        val opfData = parseOpf(String(opfContent, Charsets.UTF_8))

        var globalIdx = 0
        val chapters = opfData.spineItemPaths.mapIndexedNotNull { chapterIdx, relativePath ->
            val fullPath = if (opfDir.isNotEmpty()) "$opfDir/$relativePath" else relativePath
            val htmlBytes = entries[fullPath] ?: return@mapIndexedNotNull null
            val html = String(htmlBytes, Charsets.UTF_8)

            parseChapter(html, chapterIdx, globalIdx).also { chapter ->
                globalIdx += chapter.words.size
            }
        }

        return BookData(
            title = opfData.title,
            author = opfData.author,
            chapters = chapters
        )
    }

    private fun parseContainerXml(xml: String): String {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                val path = parser.getAttributeValue(null, "full-path")
                if (path != null) return path
            }
            eventType = parser.next()
        }
        throw EpubException.NotAnEpub("no rootfile in container.xml")
    }

    private fun parseOpf(xml: String): OpfData {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())

        // Left blank when the file does not say; the caller supplies wording.
        var title = ""
        var author = ""
        val manifest = mutableMapOf<String, String>()
        val spineIds = mutableListOf<String>()

        var eventType = parser.eventType
        var currentTag = ""
        var inMetadata = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name ?: ""
                    when (currentTag) {
                        "metadata" -> inMetadata = true
                        "item" -> {
                            val id = parser.getAttributeValue(null, "id") ?: ""
                            val href = parser.getAttributeValue(null, "href") ?: ""
                            val mediaType = parser.getAttributeValue(null, "media-type") ?: ""
                            if (mediaType.contains("html") || mediaType.contains("xml")) {
                                manifest[id] = href
                            }
                        }
                        "itemref" -> {
                            val idref = parser.getAttributeValue(null, "idref") ?: ""
                            spineIds.add(idref)
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inMetadata) {
                        val text = parser.text?.trim() ?: ""
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "title" -> title = text
                                "creator" -> author = text
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "metadata") inMetadata = false
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        val spineItemPaths = spineIds.mapNotNull { manifest[it] }
        return OpfData(title, author, spineItemPaths)
    }

    // Patterns that indicate non-content chapters
    private val SKIP_TITLE_PATTERNS = listOf(
        "cover", "table of contents", "toc", "copyright",
        "title page", "dedication", "about the author",
        "also by", "acknowledgment", "colophon"
    )

    private fun isContentChapter(title: String, wordCount: Int): Boolean {
        if (wordCount < 10) return false
        val lower = title.lowercase()
        return SKIP_TITLE_PATTERNS.none { lower.contains(it) }
    }

    private fun parseChapter(
        html: String,
        chapterIdx: Int,
        globalIdxStart: Int
    ): BookChapter {
        val doc = Jsoup.parse(html)

        val paragraphs = mutableListOf<String>()
        val blockTags = doc.select("p, h1, h2, h3, h4, h5, h6, div, li, blockquote")
        for (element in blockTags) {
            val text = element.text().trim()
            if (text.isNotEmpty()) {
                paragraphs.add(text)
            }
        }

        if (paragraphs.isEmpty()) {
            val bodyText = doc.body()?.text()?.trim() ?: ""
            if (bodyText.isNotEmpty()) {
                paragraphs.add(bodyText)
            }
        }

        // Blank when the chapter carries no heading; the caller names it.
        val chapterTitle = doc.select("h1, h2, h3").firstOrNull()?.text().orEmpty()

        var globalIdx = globalIdxStart
        val words = mutableListOf<RsvpWord>()
        for (paragraph in paragraphs) {
            val rawWords = paragraph.split("\\s+".toRegex()).filter { it.isNotBlank() }

            // Pre-pass: merge tokens that make more sense together
            val merged = mutableListOf<String>()
            var i = 0
            while (i < rawWords.size) {
                val raw = rawWords[i]
                val next = rawWords.getOrNull(i + 1)

                if (next != null && shouldMerge(raw, next)) {
                    merged.add("$raw $next")
                    i += 2
                } else {
                    merged.add(raw)
                    i++
                }
            }

            for ((wIdx, raw) in merged.withIndex()) {
                val lastChar = raw.lastOrNull()
                val endsWithPunctuation = lastChar != null &&
                        (lastChar in PUNCTUATION_PAUSE || lastChar in SENTENCE_END)

                val nextWord = merged.getOrNull(wIdx + 1)
                val isEndOfSentence = lastChar != null && lastChar in SENTENCE_END
                        && !isAbbreviation(raw, nextWord)

                val isLastWordInParagraph = wIdx == merged.size - 1

                words.add(
                    RsvpWord(
                        text = raw,
                        orpIndex = calculateOrp(raw),
                        endsWithPunctuation = endsWithPunctuation,
                        isEndOfSentence = isEndOfSentence,
                        isEndOfParagraph = isLastWordInParagraph,
                        globalIndex = globalIdx,
                        chapterIndex = chapterIdx
                    )
                )
                globalIdx++
            }
        }

        return BookChapter(
            title = chapterTitle,
            paragraphs = paragraphs,
            words = words,
            isContentChapter = isContentChapter(chapterTitle, words.size)
        )
    }

    fun calculateOrp(word: String): Int {
        val len = word.length
        return when {
            len <= 1 -> 0
            len <= 3 -> 0
            len <= 5 -> 1
            len <= 7 -> 2
            len <= 9 -> 3
            len <= 13 -> 4
            len <= 17 -> 5
            else -> (len * 0.3).toInt().coerceAtMost(7)
        }
    }

    private data class OpfData(
        val title: String,
        val author: String,
        val spineItemPaths: List<String>
    )
}
