package com.example.auplayer.library

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryTest {
    private fun entry(uri: String, pos: Long, dur: Long, at: Long) =
        ResumeEntry(uri, uri, pos, dur, at)

    @Test
    fun `save and load round trip`() = runBlocking {
        val store = InMemoryResumeStore()
        store.save(entry("a", 10, 100, 3))
        assertEquals(entry("a", 10, 100, 3), store.load("a"))
        assertNull(store.load("missing"))
    }

    @Test
    fun `continue watching orders by recency and drops finished`() = runBlocking {
        val store = InMemoryResumeStore()
        store.save(entry("old", 10, 100, 1))
        store.save(entry("new", 20, 100, 2))
        store.save(entry("done", 100, 100, 3))
        store.save(entry("zero", 0, 100, 4))
        assertEquals(listOf("new", "old"), store.continueWatching(10).map { it.uri })
        assertEquals(listOf("new"), store.continueWatching(1).map { it.uri })
    }

    @Test
    fun `remove and clear work`() = runBlocking {
        val store = InMemoryResumeStore()
        store.save(entry("a", 10, 100, 1))
        store.remove("a")
        assertNull(store.load("a"))
        store.save(entry("b", 10, 100, 1))
        store.clear()
        assertEquals(emptyList<ResumeEntry>(), store.continueWatching(10))
    }
}
