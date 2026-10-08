package com.example.auplayer.player

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TracksTest {
    private val list = TrackList(
        listOf(
            Track(1, TrackKind.AUDIO, lang = "en", codec = "aac"),
            Track(2, TrackKind.AUDIO, lang = "ar", codec = "ac3"),
            Track(0, TrackKind.VIDEO, codec = "h264"),
            Track(3, TrackKind.SUBTITLE, lang = "en"),
            Track(4, TrackKind.SUBTITLE, lang = "ar")
        )
    )

    @Test
    fun `audio and subtitle lists filter by kind`() {
        assertEquals(listOf(1, 2), list.audio().map { it.id })
        assertEquals(listOf(3, 4), list.subtitles().map { it.id })
    }

    @Test
    fun `audio command sets aid`() {
        assertEquals(listOf("set", "aid", "2"), list.audioCommand(2))
    }

    @Test
    fun `subtitle command sets sid or disables`() {
        assertEquals(listOf("set", "sid", "4"), list.subtitleCommand(4))
        assertEquals(listOf("set", "sid", "no"), list.subtitleCommand(null))
    }

    @Test
    fun `external subs use sub-add`() {
        assertEquals(listOf("sub-add", "/s/a.ass"), list.externalSubCommand("/s/a.ass"))
    }

    @Test
    fun `supported extensions gate sidecars`() {
        assertTrue(list.isSupportedSubtitle("Ep 12.srt"))
        assertTrue(list.isSupportedSubtitle("/s/a.ASS"))
        assertTrue(list.isSupportedSubtitle("sub.vtt"))
        assertFalse(list.isSupportedSubtitle("sub.ssa"))
        assertFalse(list.isSupportedSubtitle("video.mkv"))
    }

    @Test
    fun `engine applies track commands`() {
        val bridge = FakeMpvBridge()
        val engine = PlayerEngine(bridge, Dispatchers.Unconfined)
        engine.setAudioTrack(2)
        engine.setSubtitleTrack(null)
        engine.addSubtitleFile("/s/a.srt")
        assertEquals(
            listOf(
                listOf("set", "aid", "2"),
                listOf("set", "sid", "no"),
                listOf("sub-add", "/s/a.srt")
            ),
            bridge.commands
        )
    }
}
