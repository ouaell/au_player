package com.example.auplayer.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PageRefTest {
    private val provider = FakePageProvider(title = "ch1", pageCount = 5)

    @Test
    fun `ref carries index and stable id`() {
        assertEquals(PageRef(2, "ch1#2"), provider.ref(2))
    }

    @Test
    fun `out of range throws`() {
        assertThrows(IndexOutOfBoundsException::class.java) { provider.ref(-1) }
        assertThrows(IndexOutOfBoundsException::class.java) { provider.ref(5) }
    }
}
