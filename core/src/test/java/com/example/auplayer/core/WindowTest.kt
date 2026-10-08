package com.example.auplayer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowTest {
    @Test
    fun `width maps to classes`() {
        assertEquals(WindowClass.COMPACT, windowClass(599))
        assertEquals(WindowClass.MEDIUM, windowClass(600))
        assertEquals(WindowClass.MEDIUM, windowClass(839))
        assertEquals(WindowClass.EXPANDED, windowClass(840))
    }

    @Test
    fun `screen stays on only for foreground playback`() {
        assertTrue(shouldKeepScreenOn(playing = true, inPip = false, inBackground = false))
        assertFalse(shouldKeepScreenOn(playing = false, inPip = false, inBackground = false))
        assertFalse(shouldKeepScreenOn(playing = true, inPip = true, inBackground = false))
        assertFalse(shouldKeepScreenOn(playing = true, inPip = false, inBackground = true))
    }
}
