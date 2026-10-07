package com.example.auplayer.enhance

import org.junit.Assert.assertEquals
import org.junit.Test

class FakeEnhanceEngineTest {
    @Test
    fun `apply records modes and clear counts`() {
        val fake = FakeEnhanceEngine()
        fake.apply("Anime")
        fake.apply("Standard")
        fake.clear()
        assertEquals(listOf("Anime", "Standard"), fake.applied)
        assertEquals(1, fake.clears)
    }
}
