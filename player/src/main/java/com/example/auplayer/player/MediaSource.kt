package com.example.auplayer.player

import com.example.auplayer.core.Diag

/** Playable origins (MS-010). `content://` fd resolution needs ContentResolver
 *  and lands with device testing; the scheme routing here is fully specified. */
sealed interface MediaSource {
    data class File(val path: String) : MediaSource
    data class Content(val uri: String) : MediaSource
    data class Http(val url: String) : MediaSource
    data class Hls(val url: String) : MediaSource
    data class Dash(val url: String) : MediaSource
}

/** Error taxonomy (MS-010 done-when: a test per error type). */
sealed interface PlayerError {
    data class UnsupportedScheme(val scheme: String?) : PlayerError
    data class UnsupportedMime(val mime: String) : PlayerError
    data object EmptyUri : PlayerError
}

sealed interface ResolveOutcome {
    data class Ok(val source: MediaSource) : ResolveOutcome
    data class Err(val error: PlayerError) : ResolveOutcome
}

private const val SCHEME_SEP = "://"
private const val FILE_PREFIX = "file://"
private const val M3U8_MIME = "application/x-mpegurl"
private const val M3U8_MIME_ALT = "application/vnd.apple.mpegurl"
private const val M3U8_EXT = ".m3u8"
private const val DASH_MIME = "application/dash+xml"
private const val MPD_EXT = ".mpd"
private const val VIDEO_PREFIX = "video/"
private const val AUDIO_PREFIX = "audio/"

/** Pure intent/URI → source routing. */
object SourceResolver {
    fun resolve(uriString: String, mimeType: String?): ResolveOutcome {
        if (uriString.isBlank()) return ResolveOutcome.Err(PlayerError.EmptyUri)
        val scheme = uriString.substringBefore(SCHEME_SEP, missingDelimiterValue = "").lowercase()
        return when (scheme) {
            "http", "https" -> resolveHttp(uriString, mimeType)
            "file" -> ResolveOutcome.Ok(MediaSource.File(uriString.lowercase().removePrefix(FILE_PREFIX)))
            "content" -> resolveContent(uriString, mimeType)
            else -> ResolveOutcome.Err(PlayerError.UnsupportedScheme(scheme.ifEmpty { null }))
        }
    }

    private fun resolveHttp(uriString: String, mimeType: String?): ResolveOutcome {
        val lower = uriString.lowercase()
        return if (mimeType == M3U8_MIME || mimeType == M3U8_MIME_ALT || lower.contains(M3U8_EXT)) {
            ResolveOutcome.Ok(MediaSource.Hls(uriString))
        } else if (mimeType == DASH_MIME || lower.contains(MPD_EXT)) {
            ResolveOutcome.Ok(MediaSource.Dash(uriString))
        } else {
            ResolveOutcome.Ok(MediaSource.Http(uriString))
        }
    }

    private fun resolveContent(uriString: String, mimeType: String?): ResolveOutcome {
        val playable = mimeType == null ||
            mimeType.startsWith(VIDEO_PREFIX) ||
            mimeType.startsWith(AUDIO_PREFIX)
        return if (playable) {
            ResolveOutcome.Ok(MediaSource.Content(uriString))
        } else if (mimeType != null) {
            ResolveOutcome.Err(PlayerError.UnsupportedMime(mimeType))
        } else {
            ResolveOutcome.Err(PlayerError.UnsupportedMime(""))
        }
    }

    /** mpv `loadfile` argument for a resolved source. */
    fun playable(source: MediaSource): String = when (source) {
        is MediaSource.File -> source.path
        is MediaSource.Content -> source.uri
        is MediaSource.Http -> source.url
        is MediaSource.Hls -> source.url
        is MediaSource.Dash -> source.url
    }
}

/** Opens a resolved source on the engine, or records the routing error. */
class PlayerOpener(private val engine: PlayerEngine) {
    fun open(uriString: String, mimeType: String?): ResolveOutcome {
        return when (val outcome = SourceResolver.resolve(uriString, mimeType)) {
            is ResolveOutcome.Ok -> {
                engine.open(SourceResolver.playable(outcome.source))
                Diag.record("player", "open ${outcome.source}")
                outcome
            }
            is ResolveOutcome.Err -> {
                Diag.record("player", "open failed ${outcome.error}")
                outcome
            }
        }
    }
}
