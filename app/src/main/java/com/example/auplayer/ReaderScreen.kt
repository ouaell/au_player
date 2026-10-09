package com.example.auplayer

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.example.auplayer.reader.PagedReader
import com.example.auplayer.reader.PageProvider
import com.example.auplayer.reader.PdfPageProvider
import com.example.auplayer.reader.FolderProvider
import com.example.auplayer.reader.ZipProvider
import com.example.auplayer.routing.ReaderKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reader entry for VIEW intents (MS-040 routing made visible): archive
 * (.zip/.cbz via [ZipProvider]) and PDF (via [PdfPageProvider]) render as
 * bitmap pages in [PagedReader] with a title + page counter (progress memory
 * and modes are MS-038/MS-041 scope). Failures show text, never crash; page
 * counts go to logcat under E4GATE for the emulator gate.
 */
@Composable
fun ReaderScreen(uri: String, kind: ReaderKind, modifier: Modifier = Modifier) {
    val file = remember(uri) { File(android.net.Uri.parse(uri).path ?: uri) }
    val provider = remember(file, kind) { openReaderProvider(file, kind) }
    DisposableEffect(provider) {
        onDispose { try { provider?.close() } catch (_: Exception) { } }
    }
    val count = provider?.pageCount ?: 0
    val error = if (provider == null) "cannot open ${file.name}" else null
    if (count > 0) {
        Log.i("E4GATE", "reader pages=$count title=${provider?.title}")
    }
    var current by remember { mutableStateOf(0) }
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "${provider?.title ?: file.name} — ${current + 1} / $count",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        )
        ReaderBody(provider = provider, count = count, current = current, error = error) {
            current = it
        }
    }
}

@Composable
private fun ReaderBody(
    provider: PageProvider?,
    count: Int,
    current: Int,
    error: String?,
    onPageChange: (Int) -> Unit
) {
    if (error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = error)
        }
    } else if (provider != null && count > 0) {
        PagedReader(
            pageCount = count,
            current = current,
            onPageChange = onPageChange
        ) { index ->
            PageImage(provider = provider, index = index)
        }
    }
}

/** Opens the provider for a VIEW route; specific catches keep failures visible. */
internal fun openReaderProvider(file: File, kind: ReaderKind): PageProvider? {
    return try {
        if (file.isDirectory) {
            FolderProvider(file)
        } else if (kind == ReaderKind.PDF || file.extension.lowercase() == PDF_EXT) {
            PdfPageProvider(file)
        } else {
            ZipProvider(file)
        }
    } catch (error: java.io.IOException) {
        Log.w("ReaderScreen", "cannot open ${file.name}", error)
        null
    } catch (error: SecurityException) {
        Log.w("ReaderScreen", "locked file ${file.name}", error)
        null
    } catch (error: IllegalArgumentException) {
        Log.w("ReaderScreen", "bad archive ${file.name}", error)
        null
    }
}

private const val PDF_EXT = "pdf"

@Composable
private fun PageImage(provider: PageProvider, index: Int, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, provider, index) {
        value = withContext(Dispatchers.IO) {
            try {
                provider.open(index).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (error: java.io.IOException) {
                Log.w("ReaderScreen", "page $index unreadable", error)
                null
            } catch (error: SecurityException) {
                Log.w("ReaderScreen", "page $index locked", error)
                null
            }
        }
    }
    val image = bitmap
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(bitmap = image, contentDescription = "page ${index + 1}")
        } else {
            Text(text = "loading page ${index + 1}…")
        }
    }
}
