package com.example.auplayer.websource

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectorParseTest {
    @Test
    fun `parses flat collector output`() {
        val raw = """[{"u":"https://a/x.jpg","w":800,"h":600,"ad":false}," +
            """{"u":"https://a/y.jpg","w":0,"h":0,"ad":true}]"""
        val out = CollectorParse.parse(raw)
        assertEquals(2, out.size)
        assertEquals("https://a/x.jpg", out[0].url)
        assertEquals(800, out[0].width)
        assertEquals(600, out[0].height)
        assertEquals(false, out[0].fromAd)
        assertEquals(0, out[0].order)
        assertEquals(true, out[1].fromAd)
        assertEquals(1, out[1].order)
    }

    @Test
    fun `unwraps evaluateJavascript string encoding`() {
        val raw = "\"[{\\\"u\\\":\\\"https://a/x.jpg\\\",\\\"w\\\":10,\\\"h\\\":10,\\\"ad\\\":false}]\""
        val out = CollectorParse.parse(raw)
        assertEquals(1, out.size)
        assertEquals("https://a/x.jpg", out[0].url)
    }

    @Test
    fun `blank null and garbage yield empty`() {
        assertTrue(CollectorParse.parse(null).isEmpty())
        assertTrue(CollectorParse.parse("").isEmpty())
        assertTrue(CollectorParse.parse("null").isEmpty())
        assertTrue(CollectorParse.parse("not json").isEmpty())
        assertTrue(CollectorParse.parse("[oops").isEmpty())
    }

    @Test
    fun `entry without url is skipped`() {
        val raw = """[{"w":10,"h":10,"ad":false}]"""
        assertTrue(CollectorParse.parse(raw).isEmpty())
    }
}
