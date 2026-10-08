package com.example.auplayer.core

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {
    @Test
    fun `defaults match the plan`() = runBlocking {
        val settings = InMemorySettingsStore().settings.first()
        assertEquals(EnhanceMode.OFF, settings.enhanceMode)
        assertEquals(DecoderPref.HARDWARE, settings.decoder)
        assertEquals(true, settings.resume)
        assertEquals(true, settings.autoNext)
        assertEquals(10L, settings.doubleTapSeconds)
        assertEquals(ReaderFilter.NONE, settings.readerFilter)
    }

    @Test
    fun `updates persist`() = runBlocking {
        val store = InMemorySettingsStore()
        store.update { it.copy(enhanceMode = EnhanceMode.ANIME, decoder = DecoderPref.SOFTWARE) }
        val settings = store.settings.first()
        assertEquals(EnhanceMode.ANIME, settings.enhanceMode)
        assertEquals(DecoderPref.SOFTWARE, settings.decoder)
    }
}
