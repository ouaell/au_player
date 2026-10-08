package com.example.auplayer.reader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * CBZ/ZIP provider (MS-034). Streams entries (never extracts to paths, so
 * zip-slip is impossible by construction). One archive handle per provider;
 * entries are bounded by [ArchiveLimits].
 */
class ZipProvider(private val file: File) : PageProvider {
    override val title: String = file.nameWithoutExtension

    private val archive = ZipFile(file)
    private val names: List<String> = archive.entries().toList()
        .map { it.name }
        .filter { isImageName(it) }
        .sortedWith(NaturalOrder)
        .also { names ->
            names.forEachIndexed { index, name ->
                val entry = archive.getEntry(name)
                ArchiveLimits.checkEntry(name, entry.size, index)
            }
        }

    override val pageCount: Int get() = names.size

    override suspend fun open(index: Int): InputStream = withContext(Dispatchers.IO) {
        ref(index)
        val entry = archive.getEntry(names[index])
        val bytes = archive.getInputStream(entry).use { it.readBytes() }
        ByteArrayInputStream(bytes)
    }

    override suspend fun size(index: Int): Pair<Int, Int>? {
        ref(index)
        return null
    }

    override fun close() {
        archive.close()
    }
}

/** 7z provider via commons-compress SeekableByteChannel-free path (MS-034). */
class SevenZProvider(private val file: File) : PageProvider {
    override val title: String = file.nameWithoutExtension

    private val archive = SevenZFile.builder().setFile(file).get()
    private val names: List<String> = archive.entries
        .map { it.name }
        .filter { isImageName(it) }
        .sortedWith(NaturalOrder)
        .also { names ->
            names.forEachIndexed { index, name ->
                val entry = archive.entries.first { it.name == name }
                ArchiveLimits.checkEntry(name, entry.size, index)
            }
        }

    override val pageCount: Int get() = names.size

    override suspend fun open(index: Int): InputStream = withContext(Dispatchers.IO) {
        ref(index)
        val entry = archive.entries.first { it.name == names[index] }
        val bytes = archive.getInputStream(entry).use { it.readBytes() }
        ByteArrayInputStream(bytes)
    }

    override suspend fun size(index: Int): Pair<Int, Int>? {
        ref(index)
        return null
    }

    override fun close() {
        archive.close()
    }
}

/**
 * Local folder provider (MS-034). SAF `DocumentFile` variant lands with
 * device/SAF testing; sorting, filtering and caps are shared and tested here.
 */
class FolderProvider(private val dir: File) : PageProvider {
    override val title: String = dir.name

    private val files: List<File> = (dir.listFiles() ?: emptyArray())
        .filter { it.isFile && isImageName(it.name) }
        .sortedWith { left, right -> NaturalOrder.compare(left.name, right.name) }

    override val pageCount: Int get() = files.size

    override suspend fun open(index: Int): InputStream = withContext(Dispatchers.IO) {
        ref(index)
        files[index].inputStream()
    }

    override suspend fun size(index: Int): Pair<Int, Int>? {
        ref(index)
        return null
    }

    override fun close() = Unit
}
