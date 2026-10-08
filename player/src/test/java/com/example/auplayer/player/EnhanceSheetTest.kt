package com.example.auplayer.player

import com.example.auplayer.core.EnhanceMode
import org.junit.Assert.assertEquals
import org.junit.Test

class EnhanceSheetTest {
    @Test
    fun `labels carry lowered state`() {
        assertEquals("Off", enhanceLabel(EnhanceMode.OFF, lowered = false))
        assertEquals("Anime", enhanceLabel(EnhanceMode.ANIME, lowered = false))
        assertEquals("Anime · lowered", enhanceLabel(EnhanceMode.ANIME, lowered = true))
        assertEquals("Standard", enhanceLabel(EnhanceMode.STANDARD, lowered = false))
        assertEquals("Standard · lowered", enhanceLabel(EnhanceMode.STANDARD, lowered = true))
    }
}
