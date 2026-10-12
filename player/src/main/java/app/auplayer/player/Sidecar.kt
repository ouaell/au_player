package app.auplayer.player

/** Sidecar subtitle match (MS-066): same stem, optional language tag. */
data class Sidecar(val file: String, val lang: String?)

private val knownLangs = setOf(
    "en", "eng", "ar", "ara", "fr", "fra", "de", "deu", "es", "spa", "und"
)

/**
 * Matches `movie.eng.srt`-style sidecars to `movie.mkv` (§17.7-2). Encoding
 * sniffing + `sub-codepage` wiring land with device subtitle tests (MS-059).
 */
fun matchSidecars(
    videoName: String,
    candidates: List<String>,
    subExts: Set<String> = setOf("srt", "ass", "vtt")
): List<Sidecar> {
    val stem = videoName.substringBeforeLast('.', videoName)
    return candidates.mapNotNull { file ->
        if (!file.startsWith(stem + ".")) return@mapNotNull null
        if (file.substringAfterLast('.', "").lowercase() !in subExts) return@mapNotNull null
        val middle = file.removePrefix(stem + ".").split('.').dropLast(1)
        val lang = middle.firstOrNull { it.lowercase() in knownLangs }?.lowercase()
        Sidecar(file, lang)
    }
}
