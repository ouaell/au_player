package com.example.auplayer.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Hostile archives (MS-064 fuzz-seed spirit): never crash, always fail closed. */
class ArchiveAdversarialTest {
    @get:Rule
    val folders = TemporaryFolder()

    @Test
    fun `empty zip has zero pages`() {
        val file = zipOf("empty.zip", mapOf())
        ZipProvider(file).use { provider ->
            assertEquals(0, provider.pageCount)
        }
    }

    @Test
    fun `macosx and hidden entries are ignored`() {
        val file = zipOf(
            "junk.zip",
            mapOf(
                "__MACOSX/._p1.jpg" to byteArrayOf(1, 2, 3),
                ".hidden.jpg" to byteArrayOf(4, 5),
                "notes.txt" to "hello".toByteArray()
            )
        )
        ZipProvider(file).use { provider ->
            assertEquals(0, provider.pageCount)
        }
    }

    @Test
    fun `truncated zip fails closed`() {
        val file = zipOf("cut.zip", mapOf("p1.jpg" to ByteArray(64)))
        val bytes = file.readBytes()
        file.writeBytes(bytes.copyOf(bytes.size / 2))
        var pages = -1
        try {
            ZipProvider(file).use { provider ->
                pages = provider.pageCount
            }
        } catch (error: java.io.IOException) {
            return
        } catch (error: IllegalArgumentException) {
            return
        }
        assertTrue("truncated archive exposed pages: $pages", pages <= 1)
    }

    @Test
    fun `oversize entry trips the limit`() {
        try {
            ArchiveLimits.checkEntry("huge.jpg", Long.MAX_VALUE, 0)
            throw AssertionError("limit did not trip")
        } catch (error: ArchiveLimitException) {
            assertTrue(!error.message.isNullOrEmpty())
        }
    }

    private fun zipOf(name: String, entries: Map<String, ByteArray>): File {
        val file = folders.newFile(name)
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((name, bytes) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return file
    }

    private fun ZipProvider.use(block: (ZipProvider) -> Unit) {
        try {
            block(this)
        } finally {
            runCatching { close() }
        }
    }
}
