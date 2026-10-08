package com.example.auplayer.routing

/** Android intent action, normalized (MS-040). */
enum class IncomingAction {
    VIEW,
    SEND,
    SEND_MULTIPLE,
    OTHER
}

data class Incoming(val action: IncomingAction, val mime: String?, val uris: List<String>)

enum class ReaderKind {
    IMAGES,
    ARCHIVE,
    PDF,
    URLS
}

/** Routing verdict (§7.2 + §17.7): video/audio → player, the rest → reader. */
sealed interface Route {
    data class Player(val uri: String) : Route
    data class Reader(val uris: List<String>, val kind: ReaderKind) : Route
    data object Unsupported : Route
}

object IntentRouter {
    fun route(incoming: Incoming): Route {
        if (incoming.uris.isEmpty()) return Route.Unsupported
        val mime = incoming.mime?.lowercase().orEmpty()
        return when {
            incoming.action == IncomingAction.VIEW && isPlayableStream(mime, incoming.uris) ->
                Route.Player(incoming.uris.first())
            isImage(mime) -> Route.Reader(incoming.uris, ReaderKind.IMAGES)
            isArchive(mime) -> Route.Reader(incoming.uris, ReaderKind.ARCHIVE)
            mime == PDF_MIME -> Route.Reader(incoming.uris, ReaderKind.PDF)
            else -> textRoute(incoming) ?: Route.Unsupported
        }
    }

    private fun textRoute(incoming: Incoming): Route? {
        val mime = incoming.mime?.lowercase().orEmpty()
        if (!mime.startsWith(TEXT_PREFIX)) return null
        val urls = incoming.uris.flatMap { extractUrls(it) }
        return if (urls.isNotEmpty()) Route.Reader(urls, ReaderKind.URLS) else null
    }

    private fun isPlayableStream(mime: String, uris: List<String>): Boolean {
        if (mime.startsWith(VIDEO_PREFIX) || mime.startsWith(AUDIO_PREFIX)) return true
        if (mime == HLS_MIME || mime == HLS_MIME_ALT || mime == DASH_MIME) return true
        val first = uris.first().lowercase()
        return first.contains(M3U8_EXT) || first.contains(MPD_EXT)
    }

    private fun isImage(mime: String): Boolean = mime.startsWith(IMAGE_PREFIX)

    private fun isArchive(mime: String): Boolean = mime in archiveMimes

    private fun extractUrls(text: String): List<String> {
        return text.split(regexWhitespace).filter { it.startsWith(HTTP) || it.startsWith(HTTPS) }
    }
}

private const val VIDEO_PREFIX = "video/"
private const val AUDIO_PREFIX = "audio/"
private const val IMAGE_PREFIX = "image/"
private const val TEXT_PREFIX = "text/"
private const val HLS_MIME = "application/x-mpegurl"
private const val HLS_MIME_ALT = "application/vnd.apple.mpegurl"
private const val DASH_MIME = "application/dash+xml"
private const val PDF_MIME = "application/pdf"
private const val M3U8_EXT = ".m3u8"
private const val MPD_EXT = ".mpd"
private const val HTTP = "http://"
private const val HTTPS = "https://"
private val archiveMimes = setOf(
    "application/zip",
    "application/x-cbz",
    "application/vnd.comicbook+zip"
)
private val regexWhitespace = Regex("\\s+")
