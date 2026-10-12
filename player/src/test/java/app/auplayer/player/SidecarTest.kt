package app.auplayer.player

import org.junit.Assert.assertEquals
import org.junit.Test

class SidecarTest {
    private val files = listOf(
        "movie.eng.srt",
        "movie.ara.ass",
        "movie.srt",
        "movie.forced.eng.srt",
        "other.eng.srt",
        "movie.nfo",
    )

    @Test
    fun `matches stems and parses langs`() {
        assertEquals(
            listOf(
                Sidecar("movie.eng.srt", "eng"),
                Sidecar("movie.ara.ass", "ara"),
                Sidecar("movie.srt", null),
                Sidecar("movie.forced.eng.srt", "eng"),
            ),
            matchSidecars("movie.mkv", files),
        )
    }

    @Test
    fun `no match without shared stem`() {
        assertEquals(emptyList<Sidecar>(), matchSidecars("other.mkv", listOf("movie.eng.srt")))
    }
}
