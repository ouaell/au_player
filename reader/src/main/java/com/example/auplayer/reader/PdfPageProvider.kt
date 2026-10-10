package com.example.auplayer.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

/**
 * PDF provider (MS-035, device-bound: PdfRenderer has no JVM equivalent).
 * Single-thread discipline via [Mutex] (one page open at a time, §7.2);
 * pages render at [targetWidthPx] and stream back as PNG (fits [PageProvider]).
 * Encrypted files throw [SecurityException] to the caller (password UI is a
 * follow-up; unprotected files are the 1.0 path).
 */
class PdfPageProvider(
    file: File,
    private val targetWidthPx: Int = 1080
) : PageProvider {
    override val title: String = file.nameWithoutExtension

    private val descriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(descriptor)
    private val mutex = Mutex()

    override val pageCount: Int get() = renderer.pageCount

    override suspend fun open(index: Int): InputStream = withContext(Dispatchers.IO) {
        require(index in 0 until pageCount) { "page $index of $pageCount" }
        mutex.withLock {
            renderer.openPage(index).use { page ->
                val scale = targetWidthPx.toFloat() / page.width.coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(
                    (page.width * scale).toInt().coerceAtLeast(1),
                    (page.height * scale).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                // Pages without a background paint stay transparent: paper white.
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
                bitmap.recycle()
                ByteArrayInputStream(out.toByteArray())
            }
        }
    }

    override suspend fun size(index: Int): Pair<Int, Int>? {
        require(index in 0 until pageCount) { "page $index of $pageCount" }
        return null
    }

    override fun close() {
        renderer.close()
        descriptor.close()
    }
}

private const val PNG_QUALITY = 100
