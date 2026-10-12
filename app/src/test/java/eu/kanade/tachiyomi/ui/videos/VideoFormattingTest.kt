package eu.kanade.tachiyomi.ui.videos

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFormattingTest {

    @Test
    fun `duration below an hour has no hour field`() {
        assertEquals("0:00", formatDuration(0L))
        assertEquals("0:07", formatDuration(7_000L))
        assertEquals("9:05", formatDuration(545_000L))
        assertEquals("59:59", formatDuration(3_599_000L))
    }

    @Test
    fun `duration past an hour keeps a zero padded minute and second`() {
        assertEquals("1:00:00", formatDuration(3_600_000L))
        assertEquals("1:02:03", formatDuration(3_723_000L))
        assertEquals("12:34:56", formatDuration(45_296_000L))
    }

    @Test
    fun `negative duration clamps to zero instead of rendering junk`() {
        assertEquals("0:00", formatDuration(-5_000L))
    }

    @Test
    fun `sub second input truncates rather than rounding up`() {
        assertEquals("0:00", formatDuration(999L))
        assertEquals("0:01", formatDuration(1_000L))
    }

    @Test
    fun `size switches unit at each boundary`() {
        assertEquals("512 B", formatSize(512L))
        assertEquals("1 KB", formatSize(1024L))
        assertEquals("1 MB", formatSize(1024L * 1024))
        assertEquals("1.0 GB", formatSize(1024L * 1024 * 1024))
    }

    @Test
    fun `size keeps one decimal only in gigabytes`() {
        assertEquals("1.5 GB", formatSize((1.5 * 1024 * 1024 * 1024).toLong()))
        assertEquals("900 MB", formatSize(900L * 1024 * 1024))
    }

    @Test
    fun `extension is stripped but hidden files keep their name`() {
        assertEquals("Episode 01", displayName("Episode 01.mkv"))
        assertEquals("Episode 01", displayName("Episode 01"))
        assertEquals("archive.tar", displayName("archive.tar.gz"))
        assertEquals(".nomedia", displayName(".nomedia"))
    }

    @Test
    fun `a name that is only an extension falls back to the original`() {
        assertEquals("mkv", displayName("mkv"))
    }
}