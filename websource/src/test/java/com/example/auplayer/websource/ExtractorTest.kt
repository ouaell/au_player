package com.example.auplayer.websource

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractorTest {
    private val page = "https://site.example/manga/ch-1/"

    @Test
    fun `picks ordered unique large images`() {
        val picked = ImagePicker.pick(
            listOf(
                ImageCandidate("https://cdn.example/b.jpg", 800, 1200, fromAd = false, order = 1),
                ImageCandidate("/a.jpg", 800, 1200, fromAd = false, order = 0),
                ImageCandidate("https://cdn.example/b.jpg", 800, 1200, fromAd = false, order = 2)
            ),
            page
        )
        assertEquals(listOf("https://site.example/a.jpg", "https://cdn.example/b.jpg"), picked)
    }

    @Test
    fun `rejects icons ads and data uris`() {
        val picked = ImagePicker.pick(
            listOf(
                ImageCandidate("data:image/gif;base64,x", 100, 100, fromAd = false, order = 0),
                ImageCandidate("https://x/t.png", 10, 10, fromAd = false, order = 1),
                ImageCandidate("https://x/ad.jpg", 900, 900, fromAd = true, order = 2),
                ImageCandidate(null, 900, 900, fromAd = false, order = 3)
            ),
            page
        )
        assertTrue(picked.isEmpty())
    }

    @Test
    fun `resolves relative protocol and root urls`() {
        assertEquals("https://x/y.jpg", ImagePicker.resolveUrl(page, "//x/y.jpg"))
        assertEquals("https://site.example/r.jpg", ImagePicker.resolveUrl(page, "/r.jpg"))
        assertEquals(
            "https://site.example/manga/ch-1/i.jpg",
            ImagePicker.resolveUrl(page, "i.jpg")
        )
        assertNull(ImagePicker.resolveUrl(page, "data:image/png;base64,x"))
        assertNull(ImagePicker.resolveUrl("notaurl", "/r.jpg"))
    }

    @Test
    fun `collector covers required sources`() {
        assertTrue(JsCollector.SCRIPT.contains("data-src"))
        assertTrue(JsCollector.SCRIPT.contains("srcset"))
        assertTrue(JsCollector.SCRIPT.contains("backgroundImage"))
        assertTrue(JsCollector.SCRIPT.contains("currentSrc"))
        assertTrue(JsCollector.SCROLL_STEP.contains("scrollBy"))
    }
}
