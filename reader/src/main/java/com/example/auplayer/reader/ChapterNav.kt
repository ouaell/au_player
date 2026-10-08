package com.example.auplayer.reader

/**
 * Chapter navigation for local archives/folders (MS-072): siblings in
 * natural order plus the "continue to next" prompt at the last page.
 * Image format support: WebP/AVIF where the platform decodes, HEIC static,
 * GIF/APNG first frame, JPEG XL → unsupported message (device-verified).
 */
object ChapterNav {
    fun siblings(files: List<String>, current: String): Pair<String?, String?> {
        val ordered = files.sortedWith(NaturalOrder)
        val index = ordered.indexOf(current)
        if (index < 0) return Pair(null, null)
        val prev = if (index > 0) ordered[index - 1] else null
        val next = ordered.getOrNull(index + 1)
        return Pair(prev, next)
    }

    fun atEndPrompt(pageIndex: Int, pageCount: Int): Boolean {
        return pageCount > 0 && pageIndex >= pageCount - 1
    }
}
