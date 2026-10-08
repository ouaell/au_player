package com.example.auplayer.player

enum class TrackKind {
    AUDIO,
    SUBTITLE,
    VIDEO
}

data class Track(
    val id: Int,
    val kind: TrackKind,
    val lang: String? = null,
    val title: String? = null,
    val codec: String? = null
)

/** Track extensions the player can load as sidecars (MS-017). */
val supportedSubExtensions: Set<String> = setOf("srt", "ass", "vtt")

/** Pure track-list model; selection maps to mpv `aid`/`sid` commands. */
class TrackList(tracks: List<Track>) {
    private val all = tracks.toList()

    fun audio(): List<Track> = all.filter { it.kind == TrackKind.AUDIO }

    fun subtitles(): List<Track> = all.filter { it.kind == TrackKind.SUBTITLE }

    fun audioCommand(id: Int): List<String> = listOf("set", "aid", id.toString())

    fun subtitleCommand(id: Int?): List<String> {
        return if (id == null) {
            listOf("set", "sid", "no")
        } else {
            listOf("set", "sid", id.toString())
        }
    }

    fun externalSubCommand(path: String): List<String> = listOf("sub-add", path)

    fun isSupportedSubtitle(path: String): Boolean {
        return path.substringAfterLast(EXT_SEP, "").lowercase() in supportedSubExtensions
    }
}

private const val EXT_SEP = "."
