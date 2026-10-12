package app.auplayer.enhance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderEnhanceTest {
    @Test
    fun `toggle and progress track pages`() {
        var state = ReaderEnhanceState()
        assertFalse(state.enabled)
        state = state.copy(enabled = true, enhancingPages = setOf(3))
        assertTrue(state.enabled)
        assertEquals(setOf(3), state.enhancingPages)
        state = state.copy(enhancingPages = emptySet())
        assertTrue(state.enhancingPages.isEmpty())
    }

    @Test
    fun `thermal and battery pause new work`() {
        assertTrue(shouldPauseEnhance(thermal = 2, batterySaver = false))
        assertTrue(shouldPauseEnhance(thermal = 0, batterySaver = true))
        assertFalse(shouldPauseEnhance(thermal = 0, batterySaver = false))
    }
}
