package com.example.auplayer.websource

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebPolicyTest {
    @Test
    fun `https passes, http and schemes block`() {
        assertFalse(WebPolicy.shouldBlockUrl("https://x/y"))
        assertTrue(WebPolicy.shouldBlockUrl("http://x/y"))
        assertTrue(WebPolicy.shouldBlockUrl("intent://x"))
        assertTrue(WebPolicy.shouldBlockUrl("file:///x"))
        assertTrue(WebPolicy.shouldBlockUrl("content://x"))
    }

    @Test
    fun `external schemes ask before opening`() {
        assertTrue(WebPolicy.externalAppScheme("intent://x"))
        assertTrue(WebPolicy.externalAppScheme("market://x"))
        assertFalse(WebPolicy.externalAppScheme("https://x"))
    }

    @Test
    fun `hardening defaults are locked`() {
        assertEquals(
            WebSettings(
                javaScriptEnabled = true,
                allowFileAccess = false,
                allowContentAccess = false,
                mixedContentBlocked = true,
                safeBrowsingEnabled = true,
                popupBlocked = true,
                domStorageEnabled = true
            ),
            defaultWebSettings()
        )
    }
}
