package app.auplayer.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlsLogicTest {
    @Test
    fun `visible right after interaction`() {
        val hide = AutoHide()
        hide.interact(1_000)
        assertTrue(hide.visible(1_500))
    }

    @Test
    fun `hidden after timeout`() {
        val hide = AutoHide()
        hide.interact(1_000)
        // Derived from the constant, not a literal: a hardcoded window silently
        // rots the moment the timeout is retuned.
        assertFalse(hide.visible(1_000 + AUTO_HIDE_TIMEOUT + 1))
    }

    @Test
    fun `still visible just before the timeout`() {
        val hide = AutoHide()
        hide.interact(1_000)
        assertTrue(hide.visible(1_000 + AUTO_HIDE_TIMEOUT - 1))
    }

    @Test
    fun `scrubbing pins controls visible`() {
        val hide = AutoHide()
        hide.interact(1_000)
        hide.setScrubbing(true)
        assertTrue(hide.visible(999_999))
        hide.setScrubbing(false)
        assertFalse(hide.visible(999_999))
    }

    @Test
    fun `format time covers zero minutes hours negative`() {
        assertEquals("00:00", formatTime(0))
        assertEquals("01:01", formatTime(61_000))
        assertEquals("1:00:00", formatTime(3_600_000))
        assertEquals("00:00", formatTime(-500))
    }

    @Test
    fun `controls auto hide only while playing`() {
        assertFalse(controlsVisible(autoHideVisible = false, isPlaying = true))
        assertTrue(controlsVisible(autoHideVisible = false, isPlaying = false))
    }

    @Test
    fun `controls never hide while a sheet or tap keeps them alive`() {
        assertTrue(controlsVisible(autoHideVisible = true, isPlaying = true))
        assertTrue(controlsVisible(autoHideVisible = true, isPlaying = false))
    }
}
