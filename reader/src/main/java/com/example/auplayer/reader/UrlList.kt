package com.example.auplayer.reader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Fetch errors (MS-036 done-when: a test per failure kind). */
class UrlFetchException(message: String, val code: Int = NO_CODE) : IOException(message)

class ContentTypeException(contentType: String?) : IOException("not an image: $contentType")

class SizeCapException(sizeBytes: Long) : IOException("image too large: $sizeBytes")

data class FetchHeaders(
    val referer: String? = null,
    val userAgent: String? = null,
    val cookies: String? = null
)

/**
 * Direct image URL list (MS-036): per-request Referer/UA, timeouts from the
 * client, 3 attempts with backoff, `image/*` gate, per-image size cap.
 * `WebSourceProvider` (MS-044) wraps this with WebView-captured headers.
 */
class UrlListProvider(
    private val urls: List<String>,
    private val client: OkHttpClient = OkHttpClient(),
    private val headers: FetchHeaders = FetchHeaders(),
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
    private val retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS
) : PageProvider {
    override val title: String = "web"
    override val pageCount: Int get() = urls.size

    override suspend fun open(index: Int): InputStream = withContext(Dispatchers.IO) {
        ref(index)
        fetch(urls[index], MAX_RETRIES)
    }

    override suspend fun size(index: Int): Pair<Int, Int>? {
        ref(index)
        return null
    }

    override fun close() = Unit

    private suspend fun fetch(url: String, attemptsLeft: Int): InputStream {
        val request = Request.Builder().url(url).apply {
            headers.referer?.let { header("Referer", it) }
            headers.userAgent?.let { header("User-Agent", it) }
            headers.cookies?.let { header("Cookie", it) }
        }.build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw UrlFetchException("fetch failed ${response.code}", response.code)
                }
                val body = response.body ?: throw ContentTypeException(null)
                val contentType = body.contentType()?.toString()
                if (contentType == null || !contentType.startsWith(IMAGE_PREFIX)) {
                    throw ContentTypeException(contentType)
                }
                val bytes = body.byteStream().readCapped(maxBytes)
                return ByteArrayInputStream(bytes)
            }
        } catch (error: IOException) {
            if (error is ContentTypeException || error is SizeCapException || attemptsLeft <= 1) {
                throw error
            }
            delay(retryDelayMs)
            return fetch(url, attemptsLeft - 1)
        }
    }

    private fun InputStream.readCapped(cap: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(READ_BUFFER)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > cap) throw SizeCapException(total)
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }
}

private const val MAX_RETRIES = 3
private const val DEFAULT_MAX_BYTES = 20_000_000L
private const val DEFAULT_RETRY_DELAY_MS = 1_000L
private const val IMAGE_PREFIX = "image/"
private const val READ_BUFFER = 8192
