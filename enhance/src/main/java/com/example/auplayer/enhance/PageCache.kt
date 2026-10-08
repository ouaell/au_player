package com.example.auplayer.enhance

import java.io.File

/**
 * Enhanced-page disk cache (MS-048): LRU-ish eviction by oldest-first when
 * over cap (default 500 MB), keyed files, explicit clear.
 */
class PageCache(private val dir: File, private val maxBytes: Long = DEFAULT_CAP) {
    fun put(key: String, bytes: ByteArray): File {
        dir.mkdirs()
        val file = File(dir, fileName(key))
        file.writeBytes(bytes)
        evict()
        return file
    }

    fun get(key: String): File? {
        val file = File(dir, fileName(key))
        return if (file.isFile) file else null
    }

    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    fun sizeBytes(): Long {
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    private fun evict() {
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total <= maxBytes) break
            total -= file.length()
            file.delete()
        }
    }

    private fun fileName(key: String): String {
        val clean = key.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        return clean.takeLast(MAX_NAME) + CACHE_EXT
    }
}

private const val DEFAULT_CAP = 500L * 1024L * 1024L
private const val MAX_NAME = 64
private const val CACHE_EXT = ".webp"
