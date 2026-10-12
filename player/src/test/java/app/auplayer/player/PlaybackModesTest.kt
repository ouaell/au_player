package app.auplayer.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackModesTest {
    @Test
    fun `speed clamps to quarter to four`() {
        assertEquals(0.25f, clampSpeed(0.1f))
        assertEquals(4f, clampSpeed(9f))
        assertEquals(1.5f, clampSpeed(1.5f))
    }

    @Test
    fun `speed cycles presets and wraps`() {
        assertEquals(1.25f, cycleSpeed(1f))
        assertEquals(0.25f, cycleSpeed(4f))
    }

    @Test
    fun `ab repeat loops inside the segment`() {
        val ab = ABRepeat()
        ab.setA(10_000)
        assertTrue(ab.setB(20_000))
        assertNull(ab.loopTarget(15_000))
        assertEquals(10_000L, ab.loopTarget(20_000))
        assertEquals(10_000L, ab.loopTarget(25_000))
    }

    @Test
    fun `ab rejects b before a and resets on a move`() {
        val ab = ABRepeat()
        assertFalse(ab.setB(5_000))
        ab.setA(10_000)
        assertFalse(ab.setB(10_000))
        assertTrue(ab.setB(20_000))
        ab.setA(25_000)
        assertNull(ab.loopTarget(30_000))
    }

    @Test
    fun `loop mode cycles and eof acts`() {
        assertEquals(LoopMode.ONE, cycleLoop(LoopMode.OFF))
        assertEquals(LoopMode.ALL, cycleLoop(LoopMode.ONE))
        assertEquals(LoopMode.OFF, cycleLoop(LoopMode.ALL))
        assertEquals(EofAction.REPLAY, afterEof(LoopMode.ONE, hasNextInPlaylist = true))
        assertEquals(EofAction.NEXT, afterEof(LoopMode.ALL, hasNextInPlaylist = true))
        assertEquals(EofAction.REPLAY, afterEof(LoopMode.ALL, hasNextInPlaylist = false))
        assertEquals(EofAction.STOP, afterEof(LoopMode.OFF, hasNextInPlaylist = false))
    }

    @Test
    fun `sleep timer counts down and expires`() {
        val timer = sleepTimer(10, 1_000)
        assertEquals(600_000L, timer.remainingMs(1_000))
        assertFalse(timer.expired(1_000))
        assertTrue(timer.expired(601_000))
        assertEquals(0L, timer.remainingMs(999_999))
    }
}
