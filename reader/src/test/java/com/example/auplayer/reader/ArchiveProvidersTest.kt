package com.example.auplayer.reader

import kotlinx.coroutines.runBlocking
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveProvidersTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun payload(name: String) = "bytes-of-$name".toByteArray()

    private fun makeZip(names: List<String>): File {
        val file = File(tmp.root, "test.cbz")
        ZipOutputStream(file.outputStream()).use { zip ->
            names.forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(payload(name))
                zip.closeEntry()
            }
        }
        return file
    }

    private fun make7z(names: List<String>): File {
        val file = File(tmp.root, "test.7z")
        SevenZOutputFile(file).use { out ->
            names.forEach { name ->
                val entry = SevenZArchiveEntry()
                entry.name = name
                out.putArchiveEntry(entry)
                out.write(payload(name))
                out.closeArchiveEntry()
            }
        }
        return file
    }

    @Test
    fun `natural order sorts numerically`() {
        val sorted = listOf("10.jpg", "2.jpg", "1.jpg", "ch01.jpg", "ch2.jpg").sortedWith(NaturalOrder)
        assertEquals(listOf("1.jpg", "2.jpg", "10.jpg", "ch01.jpg", "ch2.jpg"), sorted)
    }

    @Test
    fun `zip filters sorts and streams`() = runBlocking {
        val provider = ZipProvider(
            makeZip(listOf("__MACOSX/._x.jpg", ".hidden.jpg", "10.jpg", "2.jpg", "1.jpg", "note.txt"))
        )
        assertEquals(3, provider.pageCount)
        assertArrayEquals(payload("1.jpg"), provider.open(0).readBytes())
        assertArrayEquals(payload("2.jpg"), provider.open(1).readBytes())
        assertArrayEquals(payload("10.jpg"), provider.open(2).readBytes())
        provider.close()
    }

    @Test
    fun `sevenz round trips in natural order`() = runBlocking {
        val provider = SevenZProvider(make7z(listOf("10.jpg", "1.jpg", "2.jpg")))
        assertEquals(3, provider.pageCount)
        assertArrayEquals(payload("1.jpg"), provider.open(0).readBytes())
        provider.close()
    }

    @Test
    fun `limits reject bombs`() {
        assertThrows(ArchiveLimitException::class.java) {
            ArchiveLimits.checkEntry("huge.jpg", Long.MAX_VALUE, 0)
        }
        assertThrows(ArchiveLimitException::class.java) {
            ArchiveLimits.checkEntry("ok.jpg", 10, 10_000)
        }
        ArchiveLimits.checkEntry("ok.jpg", 10, 0)
    }

    @Test
    fun `image names filter macosx hidden and non-images`() {
        assertTrue(isImageName("ch/01.JPG"))
        assertTrue(isImageName("a.webp"))
        assertFalse(isImageName("__MACOSX/._01.jpg"))
        assertFalse(isImageName(".hidden.png"))
        assertFalse(isImageName("note.txt"))
        assertFalse(isImageName("noext"))
    }

    @Test
    fun `folder provider lists images naturally`() = runBlocking {
        val dir = tmp.newFolder("series")
        listOf("10.jpg", "1.jpg", "2.jpg", "cover.txt").forEach { name ->
            File(dir, name).writeBytes(payload(name))
        }
        val provider = FolderProvider(dir)
        assertEquals(3, provider.pageCount)
        assertArrayEquals(payload("1.jpg"), provider.open(0).readBytes())
        provider.close()
    }
}
