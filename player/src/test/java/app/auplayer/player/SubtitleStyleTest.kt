package app.auplayer.player

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleStyleTest {
    @Test
    fun `style maps to mpv props`() {
        val props = subtitleProps(SubtitleStyle())
        assertEquals("16", props["sub-font-size"])
        assertEquals("#FFFFFFFF", props["sub-color"])
        assertEquals("100.0", props["sub-pos"])
    }

    @Test
    fun `font is only set when chosen`() {
        assertEquals(null, subtitleProps(SubtitleStyle())["sub-font"])
        assertEquals(
            "Noto",
            subtitleProps(SubtitleStyle(font = "Noto"))["sub-font"]
        )
    }

    @Test
    fun `argb converts to aarrggbb`() {
        assertEquals("#FFFF0000", mpvColor(0xFFFF0000L))
        assertEquals("#80000000", mpvColor(0x80000000L))
    }

    @Test
    fun `delays map to sub and audio props`() {
        assertEquals(
            mapOf("sub-delay" to "0.5", "audio-delay" to "-0.2"),
            delayProps(0.5, -0.2)
        )
    }
}
