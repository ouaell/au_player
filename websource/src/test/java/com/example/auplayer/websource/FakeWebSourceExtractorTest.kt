package com.example.auplayer.websource

import org.junit.Assert.assertTrue
import org.junit.Test

class FakeWebSourceExtractorTest {
    @Test
    fun `fake extracts nothing`() {
        val fake = FakeWebSourceExtractor()
        assertTrue(fake.extract("<html></html>", "https://example.com").isEmpty())
    }
}
