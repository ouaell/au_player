package com.example.auplayer.reader

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.TimeUnit

class UrlListTest {
    private val bytes = ByteArray(256) { it.toByte() }

    private fun server(vararg responses: MockResponse): MockWebServer {
        val server = MockWebServer()
        responses.forEach { server.enqueue(it) }
        server.start()
        return server
    }

    private fun image(): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "image/jpeg")
        .setBody(Buffer().write(bytes))

    private fun provider(server: MockWebServer, path: String = "/a.jpg"): UrlListProvider {
        return UrlListProvider(
            urls = listOf(server.url(path).toString()),
            client = OkHttpClient(),
            headers = FetchHeaders(
                referer = "https://ref.example/",
                userAgent = "AuTest/1",
                cookies = "k=v"
            ),
            retryDelayMs = 0
        )
    }

    @Test
    fun `image downloads with headers`() = runBlocking {
        server(image()).use { web ->
            val data = provider(web).open(0).readBytes()
            assertArrayEquals(bytes, data)
            val request = web.takeRequest()
            assertEquals("https://ref.example/", request.getHeader("Referer"))
            assertEquals("AuTest/1", request.getHeader("User-Agent"))
            assertEquals("k=v", request.getHeader("Cookie"))
        }
    }

    @Test
    fun `redirect is followed`() = runBlocking {
        server(
            MockResponse().setResponseCode(302).addHeader("Location", "/a.jpg"),
            image("/a.jpg")
        ).use { web ->
            val data = provider(web, "/start").open(0).readBytes()
            assertArrayEquals(bytes, data)
        }
    }

    @Test
    fun `http error surfaces the code`() {
        server(
            MockResponse().setResponseCode(403),
            MockResponse().setResponseCode(403),
            MockResponse().setResponseCode(403)
        ).use { web ->
            val error = assertThrows(UrlFetchException::class.java) {
                runBlocking { provider(web).open(0) }
            }
            assertEquals(403, error.code)
        }
    }

    @Test
    fun `wrong content type is rejected without retry`() {
        server(MockResponse().setResponseCode(200).setBody("nope")).use { web ->
            assertThrows(ContentTypeException::class.java) {
                runBlocking { provider(web).open(0) }
            }
            assertEquals(0, web.requestCount - 1)
        }
    }

    @Test
    fun `oversize image is rejected`() {
        server(image()).use { web ->
            val small = UrlListProvider(
                urls = listOf(web.url("/a.jpg").toString()),
                client = OkHttpClient(),
                maxBytes = 10,
                retryDelayMs = 0
            )
            assertThrows(SizeCapException::class.java) {
                runBlocking { small.open(0) }
            }
        }
    }

    @Test
    fun `timeout fails the fetch`() {
        server(image().setBodyDelay(2, TimeUnit.SECONDS)).use { web ->
            val quick = UrlListProvider(
                urls = listOf(web.url("/a.jpg").toString()),
                client = OkHttpClient.Builder().callTimeout(300, TimeUnit.MILLISECONDS).build(),
                retryDelayMs = 0
            )
            assertThrows(Exception::class.java) {
                runBlocking { quick.open(0) }
            }
        }
    }

    @Test
    fun `empty list rejects page zero`() {
        assertEquals(0, UrlListProvider(emptyList()).pageCount)
        assertThrows(IndexOutOfBoundsException::class.java) {
            runBlocking { UrlListProvider(emptyList()).open(0) }
        }
    }
}
