package app.auplayer.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistTest {
    private fun list() = Playlist().apply {
        setItems(listOf(QueueItem("a", "A"), QueueItem("b", "B")))
    }

    @Test
    fun `starts at first item`() {
        assertEquals(QueueItem("a", "A"), list().current())
    }

    @Test
    fun `move validates bounds`() {
        val playlist = list()
        assertTrue(playlist.moveTo(1))
        assertEquals(QueueItem("b", "B"), playlist.current())
        assertFalse(playlist.moveTo(9))
        assertFalse(playlist.moveTo(-1))
    }

    @Test
    fun `auto-next off stops at end`() {
        val playlist = list()
        playlist.moveTo(1)
        assertNull(playlist.moveOnEof(LoopMode.OFF, autoNext = false))
    }

    @Test
    fun `all loops the queue`() {
        val playlist = list()
        playlist.moveTo(1)
        assertEquals(QueueItem("a", "A"), playlist.moveOnEof(LoopMode.ALL, autoNext = true))
    }

    @Test
    fun `one replays current`() {
        val playlist = list()
        assertEquals(QueueItem("a", "A"), playlist.moveOnEof(LoopMode.ONE, autoNext = true))
    }

    @Test
    fun `empty playlist stops`() {
        assertNull(Playlist().moveOnEof(LoopMode.ALL, autoNext = true))
    }
}
