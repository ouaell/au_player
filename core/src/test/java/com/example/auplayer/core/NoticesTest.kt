package com.example.auplayer.core

import org.junit.Assert.assertTrue
import org.junit.Test

class NoticesTest {
    @Test
    fun `every entry is complete`() {
        assertTrue(THIRD_PARTY_NOTICES.isNotEmpty())
        THIRD_PARTY_NOTICES.forEach { entry ->
            assertTrue(entry.name.isNotBlank())
            assertTrue(entry.license.isNotBlank())
            assertTrue(entry.url.startsWith("https://"))
        }
    }
}
