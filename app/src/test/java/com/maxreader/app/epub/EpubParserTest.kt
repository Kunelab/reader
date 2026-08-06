package com.maxreader.app.epub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Exercises the whole parse path against EPUBs assembled in memory.
 *
 * Runs under Robolectric because the parser uses XmlPullParser, which is a stub in the
 * plain JVM android.jar.
 */
@RunWith(RobolectricTestRunner::class)
class EpubParserTest {

    private fun epub(
        opf: String = defaultOpf,
        container: String = defaultContainer,
        chapters: Map<String, String> = mapOf("OEBPS/chapter1.xhtml" to defaultChapter),
        omitOpf: Boolean = false,
        omitContainer: Boolean = false
    ): ByteArrayInputStream {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            fun put(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put("mimetype", "application/epub+zip")
            if (!omitContainer) put("META-INF/container.xml", container)
            if (!omitOpf) put("OEBPS/content.opf", opf)
            chapters.forEach { (name, content) -> put(name, content) }
        }
        return ByteArrayInputStream(bytes.toByteArray())
    }

    private val defaultContainer = """
        <?xml version="1.0"?>
        <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
          <rootfiles>
            <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
          </rootfiles>
        </container>
    """.trimIndent()

    private val defaultOpf = """
        <?xml version="1.0"?>
        <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
          <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
            <dc:title>The Test Book</dc:title>
            <dc:creator>A. Writer</dc:creator>
          </metadata>
          <manifest>
            <item id="c1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
          </manifest>
          <spine>
            <itemref idref="c1"/>
          </spine>
        </package>
    """.trimIndent()

    private val defaultChapter = """
        <html><body>
          <h1>The First Chapter</h1>
          <p>The quick brown fox jumps over the lazy dog and then runs away into woods.</p>
        </body></html>
    """.trimIndent()

    @Test
    fun `reads title and author from the opf metadata`() {
        val book = EpubParser.parse(epub())
        assertEquals("The Test Book", book.title)
        assertEquals("A. Writer", book.author)
    }

    @Test
    fun `takes the chapter title from its first heading`() {
        val book = EpubParser.parse(epub())
        assertEquals("The First Chapter", book.chapters.single().title)
    }

    @Test
    fun `leaves metadata blank rather than inventing wording`() {
        // The caller localises these; the parser must not bake in English.
        val opfWithoutMetadata = defaultOpf
            .replace("<dc:title>The Test Book</dc:title>", "")
            .replace("<dc:creator>A. Writer</dc:creator>", "")

        val book = EpubParser.parse(epub(opf = opfWithoutMetadata))
        assertEquals("", book.title)
        assertEquals("", book.author)
    }

    @Test
    fun `chapter title is blank when the chapter has no heading`() {
        val book = EpubParser.parse(
            epub(chapters = mapOf("OEBPS/chapter1.xhtml" to "<html><body><p>Just prose here.</p></body></html>"))
        )
        assertEquals("", book.chapters.single().title)
    }

    @Test
    fun `word indices run continuously across chapters`() {
        val twoChapterOpf = defaultOpf
            .replace(
                """<item id="c1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>""",
                """<item id="c1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
                   <item id="c2" href="chapter2.xhtml" media-type="application/xhtml+xml"/>"""
            )
            .replace("""<itemref idref="c1"/>""", """<itemref idref="c1"/><itemref idref="c2"/>""")

        val book = EpubParser.parse(
            epub(
                opf = twoChapterOpf,
                chapters = mapOf(
                    "OEBPS/chapter1.xhtml" to defaultChapter,
                    "OEBPS/chapter2.xhtml" to "<html><body><h1>Second</h1><p>More words follow here now.</p></body></html>"
                )
            )
        )

        assertEquals(2, book.chapters.size)
        val all = book.allWords
        assertEquals(all.indices.toList(), all.map { it.globalIndex })
        assertEquals(0, book.chapters[0].words.first().chapterIndex)
        assertEquals(1, book.chapters[1].words.first().chapterIndex)
    }

    @Test
    fun `the last word of a paragraph is marked as a paragraph end`() {
        val book = EpubParser.parse(epub())
        val words = book.chapters.single().words
        assertTrue(words.last().isEndOfParagraph)
    }

    @Test
    fun `an abbreviation mid-sentence does not end the sentence`() {
        val book = EpubParser.parse(
            epub(
                chapters = mapOf(
                    "OEBPS/chapter1.xhtml" to
                        "<html><body><p>We met Mr. Smith at noon and walked back home again.</p></body></html>"
                )
            )
        )
        val words = book.chapters.single().words
        // "Mr." merges with "Smith", and neither is treated as a sentence boundary.
        assertTrue(words.any { it.text == "Mr. Smith" })
        assertEquals(1, words.count { it.isEndOfSentence })
        assertEquals("again.", words.first { it.isEndOfSentence }.text)
    }

    @Test
    fun `front matter is flagged as non-content`() {
        val book = EpubParser.parse(
            epub(
                chapters = mapOf(
                    "OEBPS/chapter1.xhtml" to
                        "<html><body><h1>Copyright</h1><p>${"word ".repeat(20)}</p></body></html>"
                )
            )
        )
        assertEquals(false, book.chapters.single().isContentChapter)
    }

    @Test
    fun `a missing container is reported as not an epub`() {
        assertThrows(EpubException.NotAnEpub::class.java) {
            EpubParser.parse(epub(omitContainer = true))
        }
    }

    @Test
    fun `an opf the container points at but does not exist is reported as missing content`() {
        assertThrows(EpubException.MissingContent::class.java) {
            EpubParser.parse(epub(omitOpf = true))
        }
    }

    @Test
    fun `a container with no rootfile is reported as not an epub`() {
        val emptyContainer = """
            <?xml version="1.0"?>
            <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
              <rootfiles></rootfiles>
            </container>
        """.trimIndent()

        assertThrows(EpubException.NotAnEpub::class.java) {
            EpubParser.parse(epub(container = emptyContainer))
        }
    }

    @Test
    fun `a spine entry with no matching file is skipped rather than failing`() {
        val book = EpubParser.parse(epub(chapters = emptyMap()))
        assertTrue(book.chapters.isEmpty())
    }

    @Test
    fun `images and fonts are not inflated`() {
        // A 30 MB image would trip the per-entry ceiling if it were read. Parsing
        // succeeding proves the asset was skipped rather than loaded.
        val book = EpubParser.parse(
            epub(
                chapters = mapOf(
                    "OEBPS/chapter1.xhtml" to defaultChapter,
                    "OEBPS/images/cover.jpg" to "x".repeat(30 * 1_048_576),
                    "OEBPS/fonts/body.ttf" to "y".repeat(1_048_576)
                )
            )
        )
        assertEquals("The First Chapter", book.chapters.single().title)
    }

    @Test
    fun `an oversized markup entry is rejected instead of exhausting memory`() {
        val huge = "<html><body><p>" + "word ".repeat(5_000_000) + "</p></body></html>"
        assertThrows(EpubException.NotAnEpub::class.java) {
            EpubParser.parse(
                epub(chapters = mapOf("OEBPS/chapter1.xhtml" to huge))
            )
        }
    }

    @Test
    fun `a spine file with an unusual extension is still read`() {
        // The skip rule is a denylist precisely so this keeps working.
        val opf = defaultOpf.replace("chapter1.xhtml", "chapter1.txt")
        val book = EpubParser.parse(
            epub(opf = opf, chapters = mapOf("OEBPS/chapter1.txt" to defaultChapter))
        )
        assertEquals("The First Chapter", book.chapters.single().title)
    }
}
