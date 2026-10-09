package com.example.auplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ResumeEntry
import java.io.File

/**
 * Home quick actions (§10): Open file (SAF → inbox copy → VIEW), Open URL
 * (typed URL → VIEW), Web page (in-app browser). content:// URIs are copied
 * into the app cache first: mpv opens plain paths, never content URIs (§5.2).
 */
@Composable
fun HomeScreen(
    webEnabled: Boolean,
    onWeb: () -> Unit,
    history: List<ResumeEntry> = emptyList(),
    onResume: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var urlDialog by remember { mutableStateOf(false) }
    var urlText by remember { mutableStateOf("https://") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val path = copyToInbox(context, uri)
            val mime = context.contentResolver.getType(uri) ?: guessMime(path)
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.fromFile(File(path)), mime)
                    .setClassName(context, MainActivity::class.java.name)
            )
        } catch (error: java.io.IOException) {
            Log.w("HomeScreen", "open file failed", error)
        } catch (error: android.content.ActivityNotFoundException) {
            Log.w("HomeScreen", "no viewer for file", error)
        }
    }
    if (urlDialog) {
        UrlDialog(
            initial = urlText,
            onDismiss = { urlDialog = false },
            onOpen = { target ->
                urlText = target
                urlDialog = false
                if (target.trim().isNotEmpty()) {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(target.trim()))
                            .setClassName(context, MainActivity::class.java.name)
                    )
                }
            }
        )
    }
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "AU Player")
        ContinueSection(history = history, onResume = onResume)
        Button(
            onClick = { picker.launch("*/*") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Open file") }
        Button(
            onClick = { urlDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Open URL") }
        if (webEnabled) {
            Button(
                onClick = onWeb,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Web page") }
        }
    }
}

@Composable
private fun ContinueSection(history: List<ResumeEntry>, onResume: (String) -> Unit) {
    if (history.isEmpty()) return
    Text(text = "Continue watching")
    history.forEach { entry ->
        ContinueRow(entry = entry, onResume = onResume)
    }
}

@Composable
private fun ContinueRow(
    entry: ResumeEntry,
    onResume: (String) -> Unit
) {
    val progress = if (entry.durationMs > 0) {
        (entry.positionMs.toFloat() / entry.durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    TextButton(onClick = { onResume(entry.uri) }) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = entry.title, maxLines = 1)
            LinearProgressIndicator(progress = { progress })
        }
    }
}

@Composable
private fun UrlDialog(initial: String, onDismiss: () -> Unit, onOpen: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onOpen(text) }) { Text("Open") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Open URL") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true
            )
        }
    )
}
private fun copyToInbox(context: Context, uri: Uri): String {
    val name = queryName(context, uri) ?: "shared-${System.currentTimeMillis()}"
    val dir = File(context.cacheDir, "inbox")
    dir.mkdirs()
    val out = File(dir, name)
    context.contentResolver.openInputStream(uri)?.use { input ->
        out.outputStream().use { output -> input.copyTo(output) }
    } ?: error("cannot read $uri")
    return out.absolutePath
}

private fun queryName(context: Context, uri: Uri): String? {
    if (uri.scheme != "content") {
        return uri.lastPathSegment?.substringAfterLast('/')
    }
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            name = cursor.getString(index)
        }
    }
    return name
}

private fun guessMime(path: String): String {
    return when (path.substringAfterLast('.', "").lowercase()) {
        "mp4", "mkv", "webm", "avi", "mov" -> "video/*"
        "mp3", "m4a", "ogg", "opus", "flac" -> "audio/*"
        "pdf" -> "application/pdf"
        "zip", "cbz" -> "application/vnd.comicbook+zip"
        "jpg", "jpeg", "png", "webp", "gif" -> "image/*"
        else -> "*/*"
    }
}
