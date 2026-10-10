package com.example.auplayer.player

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceResolverTest {
    @Test
    fun `http file resolves to Http`() {
        assertEquals(
            ResolveOutcome.Ok(MediaSource.Http("https://x/y.mp4")),
            SourceResolver.resolve("https://x/y.mp4", "video/mp4")
        )
    }

    @Test
    fun `m3u8 resolves to Hls by mime and by extension`() {
        assertEquals(
            ResolveOutcome.Ok(MediaSource.Hls("https://x/y.m3u8")),
            SourceResolver.resolve("https://x/y.m3u8", null)
        )
        assertEquals(
            ResolveOutcome.Ok(MediaSource.Hls("https://x/y")),
            SourceResolver.resolve("https://x/y", "application/x-mpegurl")
        )
    }

    @Test
    fun `mpd resolves to Dash`() {
        assertEquals(
            ResolveOutcome.Ok(MediaSource.Dash("https://x/y.mpd")),
            SourceResolver.resolve("https://x/y.mpd", null)
        )
    }

    @Test
    fun `file scheme resolves to File`() {
        assertEquals(
            ResolveOutcome.Ok(MediaSource.File("/s/v.mp4")),
            SourceResolver.resolve("file:///s/v.mp4", null)
        )
    }

    @Test
    fun `content video resolves to Content`() {
        val uri = "content://media/video/1"
        assertEquals(
            ResolveOutcome.Ok(MediaSource.Content(uri)),
            SourceResolver.resolve(uri, "video/mp4")
        )
    }

    @Test
    fun `content image is rejected`() {
        assertEquals(
            ResolveOutcome.Err(PlayerError.UnsupportedMime("image/jpeg")),
            SourceResolver.resolve("content://media/img/1", "image/jpeg")
        )
    }

    @Test
    fun `blank uri is EmptyUri`() {
        assertEquals(
            ResolveOutcome.Err(PlayerError.EmptyUri),
            SourceResolver.resolve("  ", null)
        )
    }

    @Test
    fun `unknown scheme is rejected`() {
        val outcome = SourceResolver.resolve("ftp://x/y.mp4", null)
        assertTrue(outcome is ResolveOutcome.Err)
        assertEquals(
            PlayerError.UnsupportedScheme("ftp"),
            (outcome as ResolveOutcome.Err).error
        )
    }

    @Test
    fun `opener plays resolved source on the engine`() {
        val bridge = FakeMpvBridge()
        val engine = PlayerEngine(bridge, Dispatchers.Unconfined)
        val opener = PlayerOpener(engine)
        // loadfile is gated on an armed surface (S1-001): arm first.
        engine.attach(Any())
        val outcome = opener.open("https://x/y.mp4", "video/mp4")
        assertEquals(ResolveOutcome.Ok(MediaSource.Http("https://x/y.mp4")), outcome)
        assertEquals(listOf(listOf("loadfile", "https://x/y.mp4")), bridge.commands)
    }

    @Test
    fun `opener records routing errors without touching the engine`() {
        val bridge = FakeMpvBridge()
        val opener = PlayerOpener(PlayerEngine(bridge, Dispatchers.Unconfined))
        val outcome = opener.open("ftp://x/y.mp4", null)
        assertTrue(outcome is ResolveOutcome.Err)
        assertTrue(bridge.commands.isEmpty())
    }
}
