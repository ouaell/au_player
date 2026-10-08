package com.example.auplayer.routing

import org.junit.Assert.assertEquals
import org.junit.Test

class IntentRouterTest {
    @Test
    fun `view video goes to player`() {
        assertEquals(
            Route.Player("file:///s/v.mp4"),
            IntentRouter.route(Incoming(IncomingAction.VIEW, "video/mp4", listOf("file:///s/v.mp4")))
        )
    }

    @Test
    fun `view hls by extension goes to player`() {
        assertEquals(
            Route.Player("https://x/y.m3u8"),
            IntentRouter.route(Incoming(IncomingAction.VIEW, null, listOf("https://x/y.m3u8")))
        )
    }

    @Test
    fun `send images go to reader`() {
        assertEquals(
            Route.Reader(listOf("content://a/1", "content://a/2"), ReaderKind.IMAGES),
            IntentRouter.route(
                Incoming(IncomingAction.SEND_MULTIPLE, "image/*", listOf("content://a/1", "content://a/2"))
            )
        )
    }

    @Test
    fun `cbz goes to archive reader`() {
        assertEquals(
            Route.Reader(listOf("file:///s/c.cbz"), ReaderKind.ARCHIVE),
            IntentRouter.route(Incoming(IncomingAction.VIEW, "application/x-cbz", listOf("file:///s/c.cbz")))
        )
    }

    @Test
    fun `pdf goes to pdf reader`() {
        assertEquals(
            Route.Reader(listOf("file:///s/d.pdf"), ReaderKind.PDF),
            IntentRouter.route(Incoming(IncomingAction.VIEW, "application/pdf", listOf("file:///s/d.pdf")))
        )
    }

    @Test
    fun `text with urls goes to url reader`() {
        assertEquals(
            Route.Reader(listOf("https://x/1.jpg"), ReaderKind.URLS),
            IntentRouter.route(
                Incoming(IncomingAction.SEND, "text/plain", listOf("look https://x/1.jpg ok"))
            )
        )
    }

    @Test
    fun `empty and unknown are unsupported`() {
        assertEquals(Route.Unsupported, IntentRouter.route(Incoming(IncomingAction.VIEW, "video/mp4", emptyList())))
        assertEquals(
            Route.Unsupported,
            IntentRouter.route(Incoming(IncomingAction.SEND, "text/plain", listOf("no links")))
        )
        assertEquals(
            Route.Unsupported,
            IntentRouter.route(Incoming(IncomingAction.OTHER, "video/mp4", listOf("file:///s/v.mp4")))
        )
    }
}
