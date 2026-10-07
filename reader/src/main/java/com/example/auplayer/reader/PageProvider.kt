package com.example.auplayer.reader

import java.io.ByteArrayInputStream
import java.io.InputStream

/** PageProvider contract §7.2. Providers land in MS-034/MS-035/MS-036. */
interface PageProvider {
    val title: String
    val pageCount: Int
    suspend fun open(index: Int): InputStream
    suspend fun size(index: Int): Pair<Int, Int>?
    fun close()
}

/** Fake for JVM/Compose tests. */
class FakePageProvider(
    override val title: String = "demo",
    override val pageCount: Int = 3,
) : PageProvider {
    override suspend fun open(index: Int): InputStream =
        ByteArrayInputStream(ByteArray(0))
    override suspend fun size(index: Int): Pair<Int, Int>? = FAKE_WIDTH to FAKE_HEIGHT
    override fun close() = Unit
}

private const val FAKE_WIDTH = 100
private const val FAKE_HEIGHT = 100
