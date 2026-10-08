package com.example.auplayer.websource

import com.example.auplayer.reader.FetchHeaders
import com.example.auplayer.reader.UrlListProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import kotlin.math.absoluteValue

/** Per-page download outcome (MS-044): placeholders render on [Failed]. */
sealed interface PageResult {
    data class Downloaded(val file: File) : PageResult
    data class Failed(val error: String) : PageResult
}

/**
 * Page downloader (MS-044): reuses [UrlListProvider] (cookies/Referer/UA,
 * retries, type + size gates) and lands bytes in the app cache. One file per
 * URL, resumable by name.
 */
class PageDownloader(
    private val client: OkHttpClient = OkHttpClient(),
    private val headers: FetchHeaders = FetchHeaders(),
    private val cacheDir: File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES
) {
    suspend fun download(url: String): PageResult = withContext(Dispatchers.IO) {
        try {
            val provider = UrlListProvider(listOf(url), client, headers, maxBytes, 0)
            provider.open(0).use { stream ->
                val file = File(cacheDir, fileNameFor(url))
                file.outputStream().use { out -> stream.copyTo(out) }
                PageResult.Downloaded(file)
            }
        } catch (error: IOException) {
            PageResult.Failed(error.message ?: "download failed")
        }
    }

    private fun fileNameFor(url: String): String {
        val last = url.substringAfterLast('/').takeLast(MAX_NAME)
        val clean = last.filter { it.isLetterOrDigit() || it == '.' || it == '-' || it == '_' }
        if (clean.contains('.')) return clean
        return "page-" + absoluteValue(url.hashCode()).toString() + ".img"
    }
}

private const val DEFAULT_MAX_BYTES = 20_000_000L
private const val MAX_NAME = 64
