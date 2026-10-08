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

/** Pure intent/URI → source routing. */
object SourceResolver {
    fun resolve(uriString: String, mimeType: String?): ResolveOutcome {
        if (uriString.isBlank()) return ResolveOutcome.Err(PlayerError.EmptyUri)
        val scheme = uriString.substringBefore("://", missingDelimiterValue = "")
        if (scheme.isEmpty()) return ResolveOutcome.Err(PlayerError.UnsupportedScheme(null))
        val lower = uriString.lowercase()
        return when (scheme.lowercase()) {
            "http", "https" -> {
                if (mimeType == "application/x-mpegurl" || mimeType == "application/vnd.apple.mpegurl" ||
                    lower.contains(".m3u8")
                ) {
                    ResolveOutcome.Ok(MediaSource.Hls(uriString))
                } else if (mimeType == "application/dash+xml" || lower.contains(".mpd")) {
                    ResolveOutcome.Ok(MediaSource.Dash(uriString))
                } else {
                    ResolveOutcome.Ok(MediaSource.Http(uriString))
                }
            }
            "file" -> ResolveOutcome.Ok(MediaSource.File(lower.removePrefix("file://")))
            "content" -> {
                val playable = mimeType == null ||
                    mimeType.startsWith("video/") ||
                    mimeType.startsWith("audio/")
                if (playable) {
                    ResolveOutcome.Ok(MediaSource.Content(uriString))
                } else {
                    ResolveOutcome.Err(PlayerError.UnsupportedMime(mimeType ?: ""))
                }
            }
            else -> ResolveOutcome.Err(PlayerError.UnsupportedScheme(scheme))
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
