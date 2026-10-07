package com.example.auplayer.reader

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FakePageProviderTest {
    @Test
    fun `fake serves defaults`() = runBlocking {
        val fake = FakePageProvider()
        assertEquals("demo", fake.title)
        assertEquals(3, fake.pageCount)
        assertEquals(100 to 100, fake.size(0))
        fake.open(0).close()
        fake.close()
    }
}
