package com.example.auplayer.library

import org.junit.Assert.assertTrue
import org.junit.Test

class FakeLibrarySourceTest {
    @Test
    fun `fake has no folders`() {
        assertTrue(FakeLibrarySource().folders().isEmpty())
    }
}
