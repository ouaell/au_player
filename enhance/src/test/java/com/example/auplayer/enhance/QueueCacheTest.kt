package com.example.auplayer.enhance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class QueueCacheTest {
    @Test
    fun `prefetch order is current plus three`() {
        assertEquals(listOf(5, 6, 7, 8), PrefetchOrder.batch(5, 100))
        assertEquals(listOf(98, 99), PrefetchOrder.batch(98, 100))
        assertEquals(emptyList<Int>(), PrefetchOrder.batch(0, 0))
    }

    @Test
    fun `stale pages drop outside the window`() {
        assertEquals(listOf(7, 8), PrefetchOrder.dropStale(listOf(3, 7, 8, 50), 7))
    }

    @Test
    fun `cache keys pin content page model scale`() {
        assertEquals("abc/4/realesr/x2", cacheKey("abc", 4, "realesr", 2))
    }

    @Test
    fun `cache stores reads and clears`() {
        val cache = PageCache(newFolder(), 10_000)
        assertNull(cache.get("k"))
        cache.put("k", byteArrayOf(1, 2, 3))
        assertEquals(true, cache.get("k")?.isFile)
        assertEquals(3L, cache.sizeBytes())
        cache.clear()
        assertEquals(0L, cache.sizeBytes())
    }

    @Test
    fun `cache evicts oldest first`() {
        val cache = PageCache(newFolder(), 5)
        cache.put("old", byteArrayOf(1, 2, 3))
        Thread.sleep(5)
        cache.put("new", byteArrayOf(4, 5, 6))
        assertNull(cache.get("old"))
        assertEquals(true, cache.get("new")?.isFile)
    }

    @get:Rule
    val tmp = TemporaryFolder()

    private fun newFolder(): java.io.File = tmp.newFolder("pagecache")
}
