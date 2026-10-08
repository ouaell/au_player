package com.example.auplayer.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryTest {
    @Test
    fun `budget is a quarter of memory class`() {
        assertEquals(64L * 1024L * 1024L, MemoryBudget.maxBytes(256))
    }

    @Test
    fun `sample size halves to cover`() {
        assertEquals(1, MemoryBudget.sampleSize(800, 600, 800, 600))
        assertEquals(2, MemoryBudget.sampleSize(1600, 1200, 800, 600))
        assertEquals(4, MemoryBudget.sampleSize(3200, 2400, 800, 600))
        assertEquals(1, MemoryBudget.sampleSize(0, 0, 100, 100))
    }

    @Test
    fun `webtoon slices cap at 2048`() {
        assertEquals(1, MemoryBudget.webtoonSlices(1500))
        assertEquals(11, MemoryBudget.webtoonSlices(22000))
        assertEquals(0, MemoryBudget.webtoonSlices(0))
    }
}
