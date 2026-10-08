package com.example.auplayer.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterNavTest {
    private val files = listOf("ch10.cbz", "ch2.cbz", "ch1.cbz")

    @Test
    fun `siblings follow natural order`() {
        assertEquals(Pair("ch1.cbz", "ch10.cbz"), ChapterNav.siblings(files, "ch2.cbz"))
        assertEquals(Pair(null, "ch2.cbz"), ChapterNav.siblings(files, "ch1.cbz"))
        assertEquals(Pair("ch2.cbz", null), ChapterNav.siblings(files, "ch10.cbz"))
    }

    @Test
    fun `unknown file has no siblings`() {
        assertEquals(Pair(null, null), ChapterNav.siblings(files, "x.cbz"))
    }

    @Test
    fun `last page prompts continue`() {
        assertTrue(ChapterNav.atEndPrompt(9, 10))
        assertFalse(ChapterNav.atEndPrompt(8, 10))
        assertFalse(ChapterNav.atEndPrompt(0, 0))
    }
}
