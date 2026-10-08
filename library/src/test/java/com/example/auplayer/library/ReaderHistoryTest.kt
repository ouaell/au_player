package com.example.auplayer.library

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderHistoryTest {
    @Test
    fun `saves and resumes pages`() = runBlocking {
        val history = ReaderHistory(InMemoryResumeStore())
        assertNull(history.lastPage("b1"))
        history.savePage("b1", "Book", 41, 200, 7)
        assertEquals(41, history.lastPage("b1"))
        history.savePage("b1", "Book", 42, 200, 8)
        assertEquals(42, history.lastPage("b1"))
    }

    @Test
    fun `recent lists unfinished books`() = runBlocking {
        val history = ReaderHistory(InMemoryResumeStore())
        history.savePage("done", "D", 200, 200, 9)
        history.savePage("open", "O", 3, 200, 10)
        assertEquals(listOf("open"), history.recent(5).map { it.uri })
    }
}
