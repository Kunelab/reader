package io.github.kunelab.reader.epub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the rules that decide where a sentence really ends and which tokens belong
 * together. These drive the pause timing during playback, so a regression here is felt
 * as the reader stuttering mid-sentence rather than as an obvious failure.
 */
class TokenizerTest {

    // --- isAbbreviation ---

    @Test
    fun `known abbreviations are not sentence ends`() {
        assertTrue(EpubParser.isAbbreviation("Mr.", "Smith"))
        assertTrue(EpubParser.isAbbreviation("etc.", "And"))
        assertTrue(EpubParser.isAbbreviation("Dr.", "Who"))
        assertTrue(EpubParser.isAbbreviation("Mme.", "Bovary"))
    }

    @Test
    fun `abbreviation matching ignores case`() {
        assertTrue(EpubParser.isAbbreviation("MR.", "Smith"))
        assertTrue(EpubParser.isAbbreviation("Etc.", "Next"))
    }

    @Test
    fun `single letter initials are abbreviations`() {
        assertTrue(EpubParser.isAbbreviation("M.", "Sarkozy"))
        assertTrue(EpubParser.isAbbreviation("J.", "Smith"))
    }

    @Test
    fun `words with several internal dots are abbreviations`() {
        assertTrue(EpubParser.isAbbreviation("U.S.A.", "Today"))
        assertTrue(EpubParser.isAbbreviation("a.m.", "Tomorrow"))
    }

    @Test
    fun `a following lowercase word means the sentence continues`() {
        assertTrue(EpubParser.isAbbreviation("Something.", "and"))
    }

    @Test
    fun `an ordinary word before a capital is a real sentence end`() {
        assertFalse(EpubParser.isAbbreviation("end.", "The"))
        assertFalse(EpubParser.isAbbreviation("home.", "She"))
    }

    @Test
    fun `trailing quotes and brackets are stripped before matching`() {
        assertTrue(EpubParser.isAbbreviation("Mr.\"", "Smith"))
        assertTrue(EpubParser.isAbbreviation("etc.)", "Next"))
        assertTrue(EpubParser.isAbbreviation("Dr.”", "Who"))
    }

    @Test
    fun `a null next word does not crash and is treated as a boundary`() {
        assertFalse(EpubParser.isAbbreviation("end.", null))
        assertTrue(EpubParser.isAbbreviation("Mr.", null))
    }

    // --- shouldMerge ---

    @Test
    fun `an abbreviation is merged with the word it qualifies`() {
        assertTrue(EpubParser.shouldMerge("M.", "Sarkozy"))
        assertTrue(EpubParser.shouldMerge("Dr.", "Who"))
    }

    @Test
    fun `single letter words are merged forward`() {
        assertTrue(EpubParser.shouldMerge("a", "book"))
        assertTrue(EpubParser.shouldMerge("I", "am"))
        assertTrue(EpubParser.shouldMerge("à", "Paris"))
    }

    @Test
    fun `a number is merged with a short unit`() {
        assertTrue(EpubParser.shouldMerge("10", "km"))
        assertTrue(EpubParser.shouldMerge("5", "kg"))
        assertTrue(EpubParser.shouldMerge("3,5", "cm"))
    }

    @Test
    fun `a number is not merged with an ordinary word`() {
        assertFalse(EpubParser.shouldMerge("10", "books"))
        assertFalse(EpubParser.shouldMerge("2024", "was"))
    }

    @Test
    fun `a currency symbol is merged with the amount that follows`() {
        assertTrue(EpubParser.shouldMerge("$", "10"))
        assertTrue(EpubParser.shouldMerge("€", "25"))
        assertTrue(EpubParser.shouldMerge("#", "42"))
    }

    @Test
    fun `a currency symbol is not merged with a non-number`() {
        assertFalse(EpubParser.shouldMerge("$", "dollars"))
    }

    @Test
    fun `ordinary adjacent words are left alone`() {
        assertFalse(EpubParser.shouldMerge("hello", "world"))
        assertFalse(EpubParser.shouldMerge("the", "book"))
    }

    @Test
    fun `single character punctuation is not merged as if it were a word`() {
        // Only letters qualify for the single-character rule.
        assertFalse(EpubParser.shouldMerge("-", "word"))
    }

    // --- calculateOrp ---

    @Test
    fun `orp stays inside the word for every length`() {
        // A word is rendered by slicing at the ORP, so an index at or past the end
        // would throw. Guard the whole range rather than a few samples.
        for (len in 1..60) {
            val word = "a".repeat(len)
            val orp = EpubParser.calculateOrp(word)
            assertTrue("ORP $orp out of range for length $len", orp in 0 until len)
        }
    }

    @Test
    fun `orp sits near the start for short words and drifts right as they grow`() {
        assertEquals(0, EpubParser.calculateOrp("a"))
        assertEquals(0, EpubParser.calculateOrp("cat"))
        assertEquals(1, EpubParser.calculateOrp("house"))     // 5 chars
        assertEquals(2, EpubParser.calculateOrp("running"))   // 7 chars
        assertEquals(3, EpubParser.calculateOrp("standard"))  // 8 chars
        assertEquals(4, EpubParser.calculateOrp("extraordinary")) // 13 chars
    }

    @Test
    fun `orp never runs past the cap for very long words`() {
        assertTrue(EpubParser.calculateOrp("a".repeat(40)) <= 7)
    }
}
