package com.example.auplayer.websource

import com.example.auplayer.reader.FetchHeaders
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PageDownloaderTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val bytes = ByteArray(128) { it.toByte() }

    private fun image(): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "image/png")
        .setBody(Buffer().write(bytes))

    @Test
    fun `download lands bytes in cache with headers`() = runBlocking {
        MockWebServer().use { web ->
            web.enqueue(image())
            val dir = tmp.newFolder("cache")
            val downloader = PageDownloader(
                OkHttpClient(),
                FetchHeaders(referer = "https://p.example/", cookies = "k=v"),
                dir
            )
            val result = downloader.download(web.url("/p.png").toString())
            assertTrue(result is PageResult.Downloaded)
            assertArrayEquals(bytes, (result as PageResult.Downloaded).file.readBytes())
            val request = web.takeRequest()
            assertEquals("https://p.example/", request.getHeader("Referer"))
            assertEquals("k=v", request.getHeader("Cookie"))
        }
    }

    @Test
    fun `errors become failed placeholders`() = runBlocking {
        MockWebServer().use { web ->
            repeat(3) { web.enqueue(MockResponse().setResponseCode(403)) }
            val downloader = PageDownloader(OkHttpClient(), FetchHeaders(), tmp.newFolder("c2"))
            val result = downloader.download(web.url("/p.png").toString())
            assertTrue(result is PageResult.Failed)
        }
    }
}
