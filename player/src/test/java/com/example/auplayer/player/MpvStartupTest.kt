package com.example.auplayer.player

import com.example.auplayer.core.Diag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MpvStartupTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun paths() = MpvPaths(
        configDir = tmp.newFolder("config"),
        cacheDir = tmp.newFolder("cache"),
        filesDir = tmp.newFolder("files")
    )

    @Test
    fun `startup options match section 3-4`() {
        val options = startupOptions(paths()).toMap()
        assertEquals("gpu", options["vo"])
        assertEquals("android", options["gpu-context"])
        assertEquals("yes", options["opengl-es"])
        assertEquals("mediacodec-copy", options["hwdec"])
        assertEquals("audiotrack,opensles", options["ao"])
        assertEquals("audio", options["video-sync"])
        assertEquals("no", options["save-position-on-quit"])
        assertEquals("bilinear", options["scale"])
        assertEquals("bilinear", options["dscale"])
        assertEquals("yes", options["cache"])
        assertEquals("32MiB", options["demuxer-max-bytes"])
        assertEquals("16MiB", options["demuxer-max-back-bytes"])
        assertTrue(options.getValue("tls-ca-file").endsWith("cacert.pem"))
        assertTrue(options.getValue("sub-fonts-dir").endsWith("fonts"))
        assertTrue(options.getValue("gpu-shader-cache-dir").endsWith("mpv-shaders"))
    }

    @Test
    fun `dirs are created`() {
        val created = ensureMpvDirs(paths())
        assertTrue(created.all { it.isDirectory })
    }

    @Test
    fun `apply sets options in order and logs once`() {
        Diag.clear()
        val bridge = FakeMpvBridge()
        val paths = paths()
        applyStartup(bridge, paths)
        assertEquals(startupOptions(paths).size, bridge.options.size)
        assertEquals("gpu", bridge.options["vo"])
        assertEquals(listOf("startup options applied"), Diag.entries().map { it.message })
    }

    @Test
    fun `diag store is bounded`() {
        Diag.clear()
        repeat(250) { Diag.record("t", "m$it") }
        val entries = Diag.entries()
        assertEquals(200, entries.size)
        assertEquals("m50", entries.first().message)
        Diag.clear()
    }
}
