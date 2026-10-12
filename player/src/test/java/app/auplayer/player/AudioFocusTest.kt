package app.auplayer.player

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioFocusTest {
    @Test
    fun `loss pauses and gain resumes`() {
        val handler = AudioFocusHandler()
        assertEquals(FocusAction.Pause, handler.onChange(AudioFocusChange.LOSS, playing = true))
        assertEquals(FocusAction.Resume, handler.onChange(AudioFocusChange.GAIN, playing = false))
    }

    @Test
    fun `loss while paused does not resume later`() {
        val handler = AudioFocusHandler()
        assertEquals(FocusAction.Nothing, handler.onChange(AudioFocusChange.LOSS, playing = false))
        assertEquals(FocusAction.Nothing, handler.onChange(AudioFocusChange.GAIN, playing = false))
    }

    @Test
    fun `transient pauses and duck unducks`() {
        val handler = AudioFocusHandler()
        assertEquals(
            FocusAction.Pause,
            handler.onChange(AudioFocusChange.TRANSIENT_LOSS, playing = true),
        )
        val ducker = AudioFocusHandler()
        assertEquals(FocusAction.Duck, ducker.onChange(AudioFocusChange.DUCK, playing = true))
        assertEquals(FocusAction.Unduck, ducker.onChange(AudioFocusChange.GAIN, playing = true))
    }

    @Test
    fun `duck while paused is ignored`() {
        val handler = AudioFocusHandler()
        assertEquals(FocusAction.Nothing, handler.onChange(AudioFocusChange.DUCK, playing = false))
    }
}
